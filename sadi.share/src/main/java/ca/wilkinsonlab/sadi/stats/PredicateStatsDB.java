package ca.wilkinsonlab.sadi.stats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.math.stat.regression.SimpleRegression;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.virtual.sparql.VirtuosoSPARQLEndpoint;
import ca.wilkinsonlab.sadi.share.Config;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.vocab.PredicateStats;

public class PredicateStatsDB 
{
	protected final static Logger log = Logger.getLogger(PredicateStatsDB.class);

	public final static int NO_STATS_AVAILABLE = -1;

	protected static PredicateStatsDB theInstance = null;
	protected Cache statsCache = null;

	public final static String ROOT_CONFIG_KEY = "sadi.statsdb";
	protected final static String ENDPOINT_URL_CONFIG_KEY = "endpoint";
	protected final static String USERNAME_CONFIG_KEY = "username";
	protected final static String PASSWORD_CONFIG_KEY = "password";
	protected final static String SAMPLES_GRAPH_CONFIG_KEY = "samplesGraph";
	protected final static String STATS_GRAPH_CONFIG_KEY = "statsGraph";
	protected final static String SAMPLE_CACHE_SIZE_CONFIG_KEY = "sampleCacheSize";
	protected final static String NUM_SAMPLES_TO_PURGE_ON_CACHE_FULL_CONFIG_KEY = "numSamplesToPurgeOnCacheFull";
	
	protected final static String DEFAULT_SAMPLES_GRAPH = "http://sadiframework.org/predicateStats/samples";
	protected final static String DEFAULT_STATS_GRAPH = "http://sadiframework.org/predicateStats/summaryStats";
	protected final static int DEFAULT_SAMPLE_CACHE_SIZE = 10000;
	protected final static int DEFAULT_NUM_SAMPLES_TO_PURGE_ON_CACHE_FULL = 200; 

	protected final static int INTERNAL_STATS_CACHE_SIZE = 5000;
	protected final static int UNINITIALIZED = -1; 
	
	protected SPARQLEndpoint endpoint;
	protected String samplesGraph;
	protected String statsGraph;
	protected int sampleCacheSize;
	protected int numSamplesToPurgeOnCacheFull;
	
	/** an internal counter that tracks the current number of samples in the DB */
	protected int numSamples = UNINITIALIZED;
	
	public PredicateStatsDB(Configuration config) throws IOException
	{
		this(config.getString(ENDPOINT_URL_CONFIG_KEY), 
			config.getString(USERNAME_CONFIG_KEY),
			config.getString(PASSWORD_CONFIG_KEY),
			config.getString(SAMPLES_GRAPH_CONFIG_KEY, DEFAULT_SAMPLES_GRAPH),
			config.getString(STATS_GRAPH_CONFIG_KEY, DEFAULT_STATS_GRAPH),
			config.getInt(SAMPLE_CACHE_SIZE_CONFIG_KEY, DEFAULT_SAMPLE_CACHE_SIZE),
			config.getInt(NUM_SAMPLES_TO_PURGE_ON_CACHE_FULL_CONFIG_KEY, DEFAULT_NUM_SAMPLES_TO_PURGE_ON_CACHE_FULL));
	}
	
	public PredicateStatsDB(String endpointURL, String username, String password) throws IOException 
	{
		this(endpointURL,
			username,
			password,
			DEFAULT_SAMPLES_GRAPH,
			DEFAULT_STATS_GRAPH,
			DEFAULT_SAMPLE_CACHE_SIZE,
			DEFAULT_NUM_SAMPLES_TO_PURGE_ON_CACHE_FULL);
	}
	
	public PredicateStatsDB(
			String endpointURL,
			String username,
			String password,
			String samplesGraph,
			String statsGraph,
			int sampleCacheSize,
			int numSamplesToPurgeOnCacheFull) 
	
	throws IOException
	{
		endpoint = new VirtuosoSPARQLEndpoint(endpointURL, username, password);
		
		initCache();
		
		this.samplesGraph = samplesGraph;
		this.statsGraph = statsGraph;
		this.sampleCacheSize = sampleCacheSize;
		this.numSamplesToPurgeOnCacheFull = numSamplesToPurgeOnCacheFull; 

		if(this.numSamplesToPurgeOnCacheFull > this.sampleCacheSize) {
			
			log.warn("numSamplesToPurgeOnCacheFull should never be greater than the sample cache size");
			this.numSamplesToPurgeOnCacheFull = Math.max(this.sampleCacheSize / 10, 1);
			log.warn(String.format("set numSamplesToPurgeOnCacheFull to %d (10% of sample cache size)", this.numSamplesToPurgeOnCacheFull));
		}

		initNumSamples();
	}
	
	protected void initCache() 
	{
		String statsCacheName = String.valueOf(System.currentTimeMillis()); 
		
		statsCache = new Cache(new CacheConfiguration(statsCacheName, INTERNAL_STATS_CACHE_SIZE)
				       .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
				       .overflowToDisk(false)
				       .eternal(false)
				       .timeToLiveSeconds(600)
				       .timeToIdleSeconds(300));

		Config.getCacheManager().addCache(statsCache);
	}
	
	protected void initNumSamples() throws IOException 
	{
		String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("count.samples.sparql.template"));
		String countQuery = SPARQLStringUtils.strFromTemplate(queryTemplate, this.samplesGraph);
		List<Map<String,String>> results = endpoint.selectQuery(countQuery);

		// this should never happen, even if the samples graph is empty

		if(results.size() == 0) {
			throw new RuntimeException("zero results from COUNT query");
		}

		// this output behaviour is probably Virtuoso-dependent

		Map<String, String> firstRow = results.iterator().next();
		String firstColumn = firstRow.keySet().iterator().next();

		this.numSamples = Integer.valueOf(firstRow.get(firstColumn));	

		log.info(String.format("predicate stats db currently has %d samples", this.numSamples));
	}
	
	public void recordSample(Property predicate, boolean directionIsForward, int numInputs, int responseTime)
	{
		if(this.numSamples >= this.sampleCacheSize) {
			log.info(String.format("samples db has reached maximum size of %d samples, purging %d oldest samples", this.sampleCacheSize, this.numSamplesToPurgeOnCacheFull));
			purgeSamples();
		}	
		
		try {

			log.info(String.format("recording sample for predicate stats (predicate = %s, direction = %s, numInputs = %d, responseTime = %d ms)", 
					predicate.getURI(),	directionIsForward ? "forward" : "reverse",	numInputs, responseTime));
			
			String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("record.sample.sparql.template"));
			
			String query = SPARQLStringUtils.strFromTemplate(
					queryTemplate, 
					this.samplesGraph, 
					PredicateStats.PREDICATE, 
					predicate.getURI(),
					PredicateStats.DIRECTION_IS_FORWARD,
					Boolean.toString(directionIsForward),
					PredicateStats.NUM_INPUTS,
					String.valueOf(numInputs),
					PredicateStats.RESPONSE_TIME,
					String.valueOf(responseTime),
					PredicateStats.TIMESTAMP,
					String.valueOf(System.currentTimeMillis()));
			
			endpoint.updateQuery(query);

			this.numSamples++;
		
		} catch(IOException e) {
			
			log.error("error updating predicate stats db: ", e);
		}
	}
	
	public void recomputeStats()
	{
		log.info("recomputing summary stats");
		for(Property p : getAllPredicatesWithSamples()) {
			recomputeStats(p);
		}
	}
	
	public void recomputeStats(Property p) 
	{
		recomputeStats(p, true);
		recomputeStats(p, false);
	}
	
	public void recomputeStats(Property p, boolean directionIsForward)
	{
		try {

			String direction = directionIsForward ? "forward" : "reverse";

			String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.samples.by.predicate.sparql.template"));
			String query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.samplesGraph, p.getURI(), String.valueOf(directionIsForward));
			List<Map<String,String>> results = endpoint.selectQuery(query);
			
			if(results.size() == 0) {
				log.info(String.format("no samples for %s in %s direction", p.getURI(), direction));
				return;
			}
			
			log.info(String.format("computing summary statistics for %s in %s direction", p.getURI(), direction));
			
			SimpleRegression regressionModel = new SimpleRegression();

			long responseTimeSum = 0;
			
			for(Map<String,String> binding : results) {
				int numInputs = Integer.valueOf(binding.get("numInputs"));
				int responseTime = Integer.valueOf(binding.get("responseTime"));
				responseTimeSum += responseTime;
				regressionModel.addData(numInputs, responseTime);
			}

			int responseTimeAverage = (int)Math.round( ((double)responseTimeSum) / results.size() ); 
			
			/* 
			 * NaN indicates that the regression line could not be computed.
			 * This happens if there aren't at least two data points with
			 * distinct x values.
			 */

			int estimatedBaseTime;
			int estimatedTimePerInput;
			
			if(Double.isNaN(regressionModel.getIntercept())) {

				estimatedBaseTime = responseTimeAverage;
				estimatedTimePerInput = 0;

			} else {
			
				estimatedBaseTime = (int)Math.round(Math.max(0, regressionModel.getIntercept()));
				estimatedTimePerInput = (int)Math.round(Math.max(0, regressionModel.getSlope()));

			}
			
			recordSummaryStats(p, directionIsForward, estimatedBaseTime, estimatedTimePerInput, results.size());
			
		} catch(IOException e) {
			
			log.error("error querying predicate stats db: ", e);
		}
	}
	
	protected void recordSummaryStats(Property p, boolean directionIsForward, int estimatedBaseTime, int estimatedTimePerInput, int numSamples)
	{
		try {
			
			String baseTimePredicate;
			String timePerInputPredicate;
			String numSamplesPredicate;

			if(directionIsForward) {
				
				baseTimePredicate = PredicateStats.ESTIMATED_BASE_TIME_FORWARD;
				timePerInputPredicate = PredicateStats.ESTIMATED_TIME_PER_INPUT_FORWARD;
				numSamplesPredicate = PredicateStats.NUM_SAMPLES_FORWARD;
				
			} else {
			
				baseTimePredicate = PredicateStats.ESTIMATED_BASE_TIME_REVERSE;
				timePerInputPredicate = PredicateStats.ESTIMATED_TIME_PER_INPUT_REVERSE;
				numSamplesPredicate = PredicateStats.NUM_SAMPLES_REVERSE;

			}

			log.info(String.format("computed %s stats for %s: baseTime = %d, timePerInput = %d, numSamples = %d", 
					directionIsForward ? "forward" : "reverse",
					p.getURI(),
					estimatedBaseTime, 
					estimatedTimePerInput, 
					numSamples));

			/* delete any existing stats, before inserting the new values */
			
			String deleteTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("delete.summary.stats.sparql.template"));
			
			String deleteQuery = SPARQLStringUtils.strFromTemplate(
					deleteTemplate, 
					this.statsGraph,
					p.getURI(),
					baseTimePredicate,
					p.getURI(),
					timePerInputPredicate,
					p.getURI(),
					numSamplesPredicate,
					this.statsGraph,
					p.getURI(),
					baseTimePredicate,
					p.getURI(),
					timePerInputPredicate,
					p.getURI(),
					numSamplesPredicate);
			
			endpoint.updateQuery(deleteQuery);
			
			/* insert new stats */
			
			String insertTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("record.summary.stats.sparql.template"));
			
			String insertQuery = SPARQLStringUtils.strFromTemplate(
					insertTemplate, 
					this.statsGraph,
					p.getURI(),
					baseTimePredicate,
					String.valueOf(estimatedBaseTime),
					p.getURI(),
					timePerInputPredicate,
					String.valueOf(estimatedTimePerInput),
					p.getURI(),
					numSamplesPredicate,
					String.valueOf(numSamples));
			
			endpoint.updateQuery(insertQuery);
			
		} catch(IOException e) {
			
			log.error("error updating predicate stats db: ", e);
		}
		
	}
	
	protected Collection<Property> getAllPredicatesWithSamples() 
	{
		Collection<Property> predicates = new ArrayList<Property>();
		
		try {

			String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.predicates.with.samples.sparql"));
			String query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.samplesGraph);
			List<Map<String,String>> results = endpoint.selectQuery(query);
			
			for(Map<String,String> binding : results) {
				predicates.add(ResourceFactory.createProperty(binding.get("predicate")));
			}
			
		} catch(IOException e) {
			
			log.error("error querying predicate stats db: ", e);
		}
		
		return predicates;
	}
	
	public void clear()
	{
		log.info("clearing predicate stats db");
		
		clearSamplesGraph();
		clearStatsGraph();
	}
	
	public void clearStatsGraph()
	{
		try {
			endpoint.updateQuery(SPARQLStringUtils.strFromTemplate("CLEAR GRAPH %u%", this.statsGraph));
		} catch (IOException e) {
			log.error("error updating predicate stats db: ", e);
		}
	}
	
	public void clearSamplesGraph()
	{
		try {
		
			endpoint.updateQuery(SPARQLStringUtils.strFromTemplate("CLEAR GRAPH %u%", this.samplesGraph));
			this.numSamples = 0;
		
		} catch (IOException e) {
			log.error("error updating predicate stats db: ", e);
		}
	}
	
	public int getEstimatedTime(Property predicate, boolean directionIsForward, int numInputs)
	{
		StatsEntry statsEntry = getStatsEntry(predicate, directionIsForward);
			
		/* there are no stats for the given predicate/direction in the DB */
		if(statsEntry == null) {
			return NO_STATS_AVAILABLE;
		}
		
		return (statsEntry.getBaseTime() + (numInputs * statsEntry.getTimePerInput()));
	}
	
	public void purgeSamples()
	{
		try {
		
			purgeSamplesByTimestamp(getTimestampCutoffForPurge());
			this.numSamples -= numSamplesToPurgeOnCacheFull;
			
			/* sanity check */
			if(this.numSamples < 0) {
				throw new RuntimeException("numSamples should never be less than zero!");
			}
			
		} catch(IOException e) {
			log.error("error updating predicate stats db: ", e);
		}
	}
	
	protected void purgeSamplesByTimestamp(long cutoffTimestamp) throws IOException
	{
		String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("purge.samples.by.timestamp.sparql.template"));
		String query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.samplesGraph, this.samplesGraph, String.valueOf(cutoffTimestamp));
		endpoint.updateQuery(query);
	}
	
	protected long getTimestampCutoffForPurge() throws IOException
	{
		String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.timestamp.cutoff.sparql.template"));
		String query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.samplesGraph, String.valueOf(this.numSamplesToPurgeOnCacheFull - 1));
		List<Map<String,String>> results = endpoint.selectQuery(query);

		// this will only happen when the graph is empty, and we should 
		// never be calling this method if that is the case.

		if(results.size() == 0) {
			log.error("query to determine timestamp cutoff failed (no results)");
			return 0;
		}

		Map<String, String> firstRow = results.iterator().next();
		return Long.valueOf(firstRow.get("timestamp"));			
	}
	
	protected String getCacheKey(Property p, boolean directionIsForward) 
	{
		StringBuilder builder = new StringBuilder();
		builder.append(p.getURI());
		builder.append(":");
		builder.append(String.valueOf(directionIsForward));

		return builder.toString();
	}
	
	protected StatsEntry getStatsEntry(Property p, boolean directionIsForward)
	{
		Element cacheEntry = statsCache.get(getCacheKey(p, directionIsForward));

		if(cacheEntry != null) {
			StatsEntry statsEntry = (StatsEntry)(cacheEntry.getObjectValue());
			return (StatsEntry)(statsEntry.clone());
		}
		
		try {
			
			String direction = directionIsForward ? "forward" : "reverse";
			
			String baseTimePredicate;
			String timePerInputPredicate;
			String numSamplesPredicate;

			if(directionIsForward) {
				
				baseTimePredicate = PredicateStats.ESTIMATED_BASE_TIME_FORWARD;
				timePerInputPredicate = PredicateStats.ESTIMATED_TIME_PER_INPUT_FORWARD;
				numSamplesPredicate = PredicateStats.NUM_SAMPLES_FORWARD;
				
			} else {
			
				baseTimePredicate = PredicateStats.ESTIMATED_BASE_TIME_REVERSE;
				timePerInputPredicate = PredicateStats.ESTIMATED_TIME_PER_INPUT_REVERSE;
				numSamplesPredicate = PredicateStats.NUM_SAMPLES_REVERSE;

			}

			String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.summary.stats.sparql.template"));
			
			String query = SPARQLStringUtils.strFromTemplate(
					queryTemplate, 
					this.statsGraph,
					p.getURI(),
					baseTimePredicate,
					p.getURI(),
					timePerInputPredicate,
					p.getURI(),
					numSamplesPredicate);
					
			List<Map<String,String>> results = endpoint.selectQuery(query);
			
			if(results.size() == 0) {
				log.info(String.format("no stats available for %s in the %s direction", p.getURI(), direction));
				return null;
			}
			
			if(results.size() > 1) {
				log.error(String.format("stats db is corrupt! (more than one entry for %s in the %s direction)", p.getURI(), direction));
				return null;
			}
			
			Map<String,String> firstRow = results.iterator().next();
			
			int baseTime = Integer.valueOf(firstRow.get("baseTime"));
			int timePerInput = Integer.valueOf(firstRow.get("timePerInput"));
			int numSamples = Integer.valueOf(firstRow.get("numSamples"));
			
			StatsEntry statsEntry = new StatsEntry(p.getURI(), directionIsForward, baseTime, timePerInput, numSamples);
		
			Element newCacheEntry = new Element(getCacheKey(p, directionIsForward), statsEntry);  
			statsCache.put(newCacheEntry);
			
			return (StatsEntry)(statsEntry.clone());

		} catch(IOException e) {

			log.error("error querying predicate stats db: ", e);
		}
		
		return null;
	}
	
	protected static class StatsEntry
	{
		protected String predicate;
		protected boolean directionIsForward;
		protected int baseTime;
		protected int timePerInput;
		protected int numSamples;

		public StatsEntry(String predicate, boolean directionIsForward, int baseTime, int timePerInput, int numSamples)
		{
			this.predicate = predicate;
			this.directionIsForward = directionIsForward;
			this.baseTime = baseTime;
			this.timePerInput = timePerInput;
			this.numSamples = numSamples;
		}

		public String getPredicate() {
			return predicate;
		}

		public boolean directionIsForward() {
			return directionIsForward;
		}

		public int getBaseTime() {
			return baseTime;
		}

		public int getTimePerInput() {
			return timePerInput;
		}

		public int getNumSamples() {
			return numSamples;
		}
		
		@Override
		protected Object clone() 
		{
			return new StatsEntry(predicate, directionIsForward, baseTime, timePerInput, numSamples);
		}
		
	}
	
}
