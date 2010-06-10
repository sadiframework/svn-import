package ca.wilkinsonlab.sadi.stats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	public final static String ENDPOINT_URL_CONFIG_KEY = "endpoint";
	public final static String USERNAME_CONFIG_KEY = "username";
	public final static String PASSWORD_CONFIG_KEY = "password";
	public final static String SAMPLES_GRAPH_CONFIG_KEY = "samplesGraph";
	public final static String STATS_GRAPH_CONFIG_KEY = "statsGraph";
	public final static String SAMPLE_CACHE_SIZE_CONFIG_KEY = "sampleCacheSize";
	public final static String NUM_SAMPLES_TO_PURGE_ON_CACHE_FULL_CONFIG_KEY = "numSamplesToPurgeOnCacheFull";
	
	public final static String DEFAULT_SAMPLES_GRAPH = "http://sadiframework.org/predicateStats/samples";
	public final static String DEFAULT_STATS_GRAPH = "http://sadiframework.org/predicateStats/summaryStats";
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
	
	public synchronized static PredicateStatsDB theInstance() 
	{
		if(theInstance == null) {
			try {
				theInstance = new PredicateStatsDB(Config.getConfiguration().subset(PredicateStatsDB.ROOT_CONFIG_KEY));
			} catch(IOException e) {
				log.error("error creating stats db singleton: ", e);
			}
		}
		
		return theInstance;
	}
	
	protected PredicateStatsDB(Configuration config) throws IOException
	{
		this(config.getString(ENDPOINT_URL_CONFIG_KEY), 
			config.getString(USERNAME_CONFIG_KEY),
			config.getString(PASSWORD_CONFIG_KEY),
			config.getString(SAMPLES_GRAPH_CONFIG_KEY, DEFAULT_SAMPLES_GRAPH),
			config.getString(STATS_GRAPH_CONFIG_KEY, DEFAULT_STATS_GRAPH),
			config.getInt(SAMPLE_CACHE_SIZE_CONFIG_KEY, DEFAULT_SAMPLE_CACHE_SIZE),
			config.getInt(NUM_SAMPLES_TO_PURGE_ON_CACHE_FULL_CONFIG_KEY, DEFAULT_NUM_SAMPLES_TO_PURGE_ON_CACHE_FULL));
	}
	
	protected PredicateStatsDB(String endpointURL, String username, String password) throws IOException 
	{
		this(endpointURL,
			username,
			password,
			DEFAULT_SAMPLES_GRAPH,
			DEFAULT_STATS_GRAPH,
			DEFAULT_SAMPLE_CACHE_SIZE,
			DEFAULT_NUM_SAMPLES_TO_PURGE_ON_CACHE_FULL);
	}
	
	protected PredicateStatsDB(
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

		log.debug(String.format("predicate stats db currently has %d samples", this.numSamples));
	}
	
	public synchronized void recordSample(Property predicate, boolean directionIsForward, int numInputs, int responseTime)
	{
		if(this.numSamples >= this.sampleCacheSize) {
			log.debug(String.format("samples db has reached maximum size of %d samples, purging %d oldest samples", this.sampleCacheSize, this.numSamplesToPurgeOnCacheFull));
			purgeSamples();
		}	
		
		try {

			log.debug(String.format("recording sample for predicate stats (predicate = %s, direction = %s, numInputs = %d, responseTime = %d ms)", 
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
	
	public synchronized void recomputeStats()
	{
		
		log.debug("recomputing summary stats");
		
		Set<Property> propertiesToEstimateForward = new HashSet<Property>();
		Set<Property> propertiesToEstimateReverse = new HashSet<Property>();
		
		for(Property p : getAllPredicatesWithSamples()) {
			
			if(!recomputeStats(p, true)) {
				propertiesToEstimateForward.add(p);
			}
			if(!recomputeStats(p, false)) {
				propertiesToEstimateReverse.add(p);
			}
		
		}
		
		recomputeAverageStats();
		
		for(Property p : propertiesToEstimateForward) {
			recomputeEstimatedStats(p, true);
		}
		
		for(Property p : propertiesToEstimateReverse) {
			recomputeEstimatedStats(p, false);
		}
		
	}
	
	public synchronized void recomputeStats(Property p) 
	{
	}
	
	public synchronized boolean recomputeStats(Property p, boolean directionIsForward)
	{
		try {

			String direction = directionIsForward ? "forward" : "reverse";

			String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.samples.by.predicate.sparql.template"));
			String query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.samplesGraph, p.getURI(), String.valueOf(directionIsForward));
			List<Map<String,String>> results = endpoint.selectQuery(query);
			
			if(results.size() == 0) {
				log.debug(String.format("no samples for %s in %s direction, skipping computation of stats", p.getURI(), direction));
				return false;
			}
			
			log.debug(String.format("computing summary statistics for %s in %s direction", p.getURI(), direction));
			
			SimpleRegression regressionModel = new SimpleRegression();

			for(Map<String,String> binding : results) {
				int numInputs = Integer.valueOf(binding.get("numInputs"));
				int responseTime = Integer.valueOf(binding.get("responseTime"));
				regressionModel.addData(numInputs, responseTime);
			}

			/* 
			 * NaN indicates that the regression line could not be computed.
			 * This happens if there aren't at least two data points with
			 * distinct x values.
			 */

			if(Double.isNaN(regressionModel.getIntercept())) {
				log.debug(String.format("insufficient samples to calculate regression line for %s, stats will be estimated using available samples and average base time", p.getURI()));
				return false;
			}
			
			int estimatedBaseTime = (int)Math.round(Math.max(0, regressionModel.getIntercept()));
			int estimatedTimePerInput = (int)Math.round(Math.max(0, regressionModel.getSlope()));
			recordSummaryStats(p, directionIsForward, estimatedBaseTime, estimatedTimePerInput, results.size());
			
		} catch(IOException e) {
			
			log.error("error querying/updating predicate stats db: ", e);
			return false;
		
		}
		
		return true;
	}
	
	protected void recomputeAverageStats() 
	{
		
		try {
			
			log.debug("recomputing averages for base time and time-per-input");
			
			String queryTemplate;
			String query;
			List<Map<String,String>> results;
			
			/* update averageBaseTime */
			
			queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.all.base.times.sparql.template"));
			query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.statsGraph);
			results = endpoint.selectQuery(query);
			
			if(results.size() == 0) {

				log.debug("unable to compute average base time, no stats available");
			
			} else {

				long baseTimeSum = 0;
				for(Map<String, String> result : results) {
					baseTimeSum += Integer.valueOf(result.get("baseTime"));
				}

	            int averageBaseTime = (int)(baseTimeSum / results.size());

	            log.debug(String.format("recording average base time: %d", averageBaseTime));
	            
				queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("update.avg.base.time.sparql.template"));
				query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.statsGraph, this.statsGraph, String.valueOf(averageBaseTime), this.statsGraph, String.valueOf(averageBaseTime));

				endpoint.updateQuery(query);
				
			}

			/* update averageTimePerInput */
			
			queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.all.time.per.inputs.sparql.template"));
			query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.statsGraph);
			results = endpoint.selectQuery(query);
			
			if(results.size() == 0) {
				
				log.debug("unable to compute average time per input, no stats available");
				
			} else {

				long timePerInputSum = 0;
				for(Map<String, String> result : results) {
					timePerInputSum += Integer.valueOf(result.get("timePerInput"));
				}

	            int averageTimePerInput = (int)(timePerInputSum / results.size());
				
	            log.debug(String.format("recording average time-per-input: %d", averageTimePerInput));
	            
				queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("update.avg.time.per.input.sparql.template"));
				query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.statsGraph, this.statsGraph, String.valueOf(averageTimePerInput), this.statsGraph, String.valueOf(averageTimePerInput));
				
				endpoint.updateQuery(query);
				
			}
			
		} catch(IOException e) {

			log.error("error querying/updating predicate stats db: ", e);
		}
		
	}
	
	protected void recomputeEstimatedStats(Property p, boolean directionIsForward) 
	{
		try {

			String direction = directionIsForward ? "forward" : "reverse";

			String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.samples.by.predicate.sparql.template"));
			String query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.samplesGraph, p.getURI(), String.valueOf(directionIsForward));
			List<Map<String,String>> results = endpoint.selectQuery(query);
			
			if(results.size() == 0) {
				log.debug(String.format("no samples for %s in %s direction, stats db will use average base time and average time-per-input to estimate %s", p.getURI(), direction, p.getURI()));
				return;
			}
			
			log.debug(String.format("computing estimated statistics for %s in %s direction, based on existing samples and average base time", p.getURI(), direction));
			
			int numInputsSum = 0;
			int responseTimeSum = 0;
			
			for(Map<String,String> binding : results) {

				int numInputs = Integer.valueOf(binding.get("numInputs"));
				int responseTime = Integer.valueOf(binding.get("responseTime"));

				numInputsSum += numInputs;
				responseTimeSum += responseTime;
				
			}

			int numInputsAvg = numInputsSum / results.size();
			int responseTimeAvg = responseTimeSum / results.size();
			
			if(getAverageTimePerInput() != NO_STATS_AVAILABLE) {

				int estimatedTimePerInput = getAverageTimePerInput();
				int estimatedBaseTime = (responseTimeAvg - (estimatedTimePerInput * numInputsAvg)); 

				recordSummaryStats(p, directionIsForward, estimatedBaseTime, estimatedTimePerInput, results.size());

			}
			
		} catch(IOException e) {
			
			log.error("error querying/updating predicate stats db: ", e);
		}
		
	}
	
	protected int getAverageBaseTime() throws IOException
	{
		Integer averageBaseTime;
		
		Element cacheEntry = statsCache.get(PredicateStats.AVERAGE_BASE_TIME);

		if(cacheEntry != null) {
		
			averageBaseTime = (Integer)(cacheEntry.getObjectValue());

		} else {

			String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.avg.base.time.sparql.template"));
			String query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.statsGraph, this.statsGraph);
			List<Map<String,String>> results = endpoint.selectQuery(query);

			if(results.size() == 0) {

				averageBaseTime = NO_STATS_AVAILABLE;

			} else {

				Map<String, String> firstRow = results.iterator().next();
				averageBaseTime =  Integer.valueOf(firstRow.get("averageBaseTime"));

			}

			statsCache.put(new Element(PredicateStats.AVERAGE_BASE_TIME, averageBaseTime));
		
		}

		if(averageBaseTime == NO_STATS_AVAILABLE) {
			log.debug("no value available for average base time");
		}

		return averageBaseTime;
	}
	
	protected int getAverageTimePerInput() throws IOException
	{
		Integer averageTimePerInput;
		
		Element cacheEntry = statsCache.get(PredicateStats.AVERAGE_TIME_PER_INPUT);

		if(cacheEntry != null) {

			averageTimePerInput = (Integer)(cacheEntry.getObjectValue());
		
		} else {
		
			String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.avg.time.per.input.sparql.template"));
			String query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.statsGraph, this.statsGraph);
			List<Map<String,String>> results = endpoint.selectQuery(query);

			if(results.size() == 0) {

				averageTimePerInput = NO_STATS_AVAILABLE;

			} else {

				Map<String, String> firstRow = results.iterator().next();
				averageTimePerInput = Integer.valueOf(firstRow.get("averageTimePerInput"));

			}

			statsCache.put(new Element(PredicateStats.AVERAGE_TIME_PER_INPUT, averageTimePerInput));
		
		}
		
		if(averageTimePerInput == NO_STATS_AVAILABLE) {
			log.debug("no value available for average time-per-input");
		}
		
		return averageTimePerInput;
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

			log.debug(String.format("computed %s stats for %s: baseTime = %d, timePerInput = %d, numSamples = %d", 
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
	
	public synchronized void clear()
	{
		log.debug("clearing predicate stats db");
		
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
	
	public synchronized void clearSamplesGraph()
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
			
			try {
			
				if(getAverageBaseTime() == NO_STATS_AVAILABLE || getAverageTimePerInput() == NO_STATS_AVAILABLE) {
					return NO_STATS_AVAILABLE;
				} else {
					return (getAverageBaseTime() + (numInputs * getAverageTimePerInput()));
				}
			
			} catch(IOException e){
			
				log.error("error occurred querying stats db for average base time and average time-per-input, returning NO_STATS_AVAILABLE instead:", e);
				return NO_STATS_AVAILABLE;

			}
			
		}
		
		return (statsEntry.getBaseTime() + (numInputs * statsEntry.getTimePerInput()));
	}
	
	public synchronized void purgeSamples()
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
				log.debug(String.format("no stats available for %s in the %s direction", p.getURI(), direction));
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
