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

import org.apache.commons.lang.time.StopWatch;
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
	
	public final static String DEFAULT_SAMPLES_GRAPH = "http://sadiframework.org/predicateStats/samples";
	public final static String DEFAULT_STATS_GRAPH = "http://sadiframework.org/predicateStats/summaryStats";

	protected final static int INTERNAL_STATS_CACHE_SIZE = 5000;
	protected final static int UNINITIALIZED = -1; 

	/*
	 * This value is placed in the statsCache to indicate that we have already queried the
	 * stats endpoint for predicate X in direction Y, and there are no stats available.
	 */
	protected static final SummaryStatsEntry NULL_STATS_ENTRY = new SummaryStatsEntry(null, false, NO_STATS_AVAILABLE, NO_STATS_AVAILABLE, NO_STATS_AVAILABLE);

	protected SPARQLEndpoint endpoint;
	protected String samplesGraph;
	protected String statsGraph;
	
	public PredicateStatsDB(Configuration config) throws IOException
	{
		this(config.getString(ENDPOINT_URL_CONFIG_KEY), 
			config.getString(USERNAME_CONFIG_KEY),
			config.getString(PASSWORD_CONFIG_KEY),
			config.getString(SAMPLES_GRAPH_CONFIG_KEY, DEFAULT_SAMPLES_GRAPH),
			config.getString(STATS_GRAPH_CONFIG_KEY, DEFAULT_STATS_GRAPH));
	}
	
	public PredicateStatsDB(String endpointURL, String username, String password) throws IOException 
	{
		this(endpointURL,
			username,
			password,
			DEFAULT_SAMPLES_GRAPH,
			DEFAULT_STATS_GRAPH);
	}
	
	public PredicateStatsDB(
			String endpointURL,
			String username,
			String password,
			String samplesGraph,
			String statsGraph) 
	
	throws IOException
	{
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		
		endpoint = new VirtuosoSPARQLEndpoint(endpointURL, username, password);
		
		initCache();
		
		this.samplesGraph = samplesGraph;
		this.statsGraph = statsGraph;

		stopWatch.stop();
		log.info(String.format("initialized predicate stats db in %dms", stopWatch.getTime()));
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
	
	public int getNumSamples() throws IOException 
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

		return Integer.valueOf(firstRow.get(firstColumn));	
	}
	
	public synchronized void recordSample(Property predicate, boolean directionIsForward, int numInputs, int responseTime)
	{
		
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

		} catch(IOException e) {
			
			log.error("error updating predicate stats db: ", e);
		}
	}
	
	public synchronized void recomputeStats()
	{
		
		log.debug("recomputing summary stats");
		
		for(Property p : getAllPredicatesWithSamples()) {
			recomputeStats(p, true);
			recomputeStats(p, false);
		}
		
		recomputeAverageStats();
		
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

				/* 
				 * The number of inputs must be the same for all samples, 
				 * otherwise we would have been able to compute a
				 * regression line. 
				 */
				int numInputs = Integer.valueOf(results.iterator().next().get("numInputs"));
				long responseTimeSum = 0;

				log.debug(String.format("unable to compute regression line, all time samples for %s are for the same number of inputs (%d)", p.getURI(), numInputs));
				
				for(Map<String,String> binding : results) {
					responseTimeSum += Integer.valueOf(binding.get("responseTime"));
				}
				
				int averageResponseTime = (int)(responseTimeSum / results.size());
				recordAverageResponseTime(p, directionIsForward, numInputs, averageResponseTime);

			} else {
			
				int estimatedBaseTime = (int)Math.round(Math.max(0, regressionModel.getIntercept()));
				int estimatedTimePerInput = (int)Math.round(Math.max(0, regressionModel.getSlope()));
				recordSummaryStats(p, directionIsForward, estimatedBaseTime, estimatedTimePerInput, results.size());

			}
			
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

	protected void recordAverageResponseTime(Property p, boolean directionIsForward, int numInputs, int averageResponseTime)
	{
		try {
			
			String responseTimePredicate = directionIsForward ? PredicateStats.AVERAGE_RESPONSE_TIME_FORWARD : PredicateStats.AVERAGE_RESPONSE_TIME_REVERSE;

			log.debug(String.format("computed average response time of %d seconds for %d inputs to %s in the %s direction", 
					averageResponseTime,
					numInputs,
					p.getURI(),
					directionIsForward ? "forward" : "reverse"));

			String insertTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("record.average.response.time.sparql.template"));
			
			String insertQuery = SPARQLStringUtils.strFromTemplate(
					insertTemplate, 
					this.statsGraph,
					p.getURI(),
					String.valueOf(numInputs),
					responseTimePredicate,
					String.valueOf(averageResponseTime));
			
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
		} catch (IOException e) {
			log.error("error updating predicate stats db: ", e);
		}
	}
	
	public int getEstimatedTime(Property predicate, boolean directionIsForward, int numInputs)
	{
		/* 
		 * If a regression line has been computed for the given predicate and direction,
		 * we can predict the response time for any number of inputs. 
		 */
		
		SummaryStatsEntry statsEntry = getSummaryStats(predicate, directionIsForward);

		if(statsEntry != null) {
			return (statsEntry.getBaseTime() + (numInputs * statsEntry.getTimePerInput()));
		}

		/* 
		 * There were insufficient samples to compute a regression line for the given
		 * predicate and direction, the last time the stats were recomputed.
		 * 
		 * However, the stats DB may have samples recorded for exact number of inputs
		 * the caller is requesting an estimate for.  In this case, we can return the average 
		 * of those response time of the matching samples as a prediction. 
		 */
		
		if(getAverageResponseTime(predicate, directionIsForward, numInputs) != NO_STATS_AVAILABLE) {
			return getAverageResponseTime(predicate, directionIsForward, numInputs);
		}
		
		/*
		 * If there is no regression line for the given predicate and direction, and
		 * there is no average response time for numInputs specifically, then use a
		 * regression representing an "average" service (i.e. average base time and
		 * average time-per-input).
		 * 
		 * Average base time and time-per-input may not be available yet, if there
		 * aren't sufficient samples to compute a regression line for at least
		 * one predicate.  If this is the case, then return NO_STATS_AVAILABLE.
		 */
		
		try {

			if(getAverageBaseTime() == NO_STATS_AVAILABLE || getAverageTimePerInput() == NO_STATS_AVAILABLE) {
				return NO_STATS_AVAILABLE;
			} else {
				return (getAverageBaseTime() + (numInputs * getAverageTimePerInput()));
			}

		} catch(IOException e){

			log.error("error occurred querying stats db for average base time and average time-per-input, returning NO_STATS_AVAILABLE:", e);
			return NO_STATS_AVAILABLE;

		}
		
	}
	
	public synchronized void purgeSamples(int numSamplesToPurge)
	{
		try {
			purgeSamplesByTimestamp(getTimestampCutoffForPurge(numSamplesToPurge));
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
	
	protected long getTimestampCutoffForPurge(int numSamplesToPurge) throws IOException
	{
		String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.timestamp.cutoff.sparql.template"));
		String query = SPARQLStringUtils.strFromTemplate(queryTemplate, this.samplesGraph, String.valueOf(numSamplesToPurge - 1));
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
	
	protected String getCacheKeyForSummaryStats(Property p, boolean directionIsForward) 
	{
		StringBuilder builder = new StringBuilder();
		builder.append(p.getURI());
		builder.append(":");
		builder.append(String.valueOf(directionIsForward));

		return builder.toString();
	}
	
	protected String getCacheKeyForSamplesAverage(Property p, boolean directionIsForward, int numInputs)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(p.getURI());
		builder.append(":");
		builder.append(String.valueOf(directionIsForward));
		builder.append(":");
		builder.append(String.valueOf(numInputs));
		
		return builder.toString();
	}
	
	protected SummaryStatsEntry getSummaryStats(Property p, boolean directionIsForward)
	{
		Element cacheEntry = statsCache.get(getCacheKeyForSummaryStats(p, directionIsForward));

		if(cacheEntry != null) {
			
			SummaryStatsEntry statsEntry = (SummaryStatsEntry)(cacheEntry.getObjectValue());
			
			if(statsEntry.equals(NULL_STATS_ENTRY)) {
				return null;
			} else {
				return (SummaryStatsEntry)(statsEntry.clone());
			}
		
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
			
			SummaryStatsEntry statsEntry;
			
			if(results.size() == 0) {
			
				log.debug(String.format("no stats available for %s in the %s direction", p.getURI(), direction));
				statsEntry = NULL_STATS_ENTRY;
			
			} else if(results.size() > 1) {
			
				log.error(String.format("stats db is corrupt! (more than one entry for %s in the %s direction)", p.getURI(), direction));
				statsEntry = NULL_STATS_ENTRY;
			
			} else {

				Map<String,String> firstRow = results.iterator().next();
				int baseTime = Integer.valueOf(firstRow.get("baseTime"));
				int timePerInput = Integer.valueOf(firstRow.get("timePerInput"));
				int numSamples = Integer.valueOf(firstRow.get("numSamples"));
				
				statsEntry = new SummaryStatsEntry(p.getURI(), directionIsForward, baseTime, timePerInput, numSamples);

			}
			
			Element newCacheEntry = new Element(getCacheKeyForSummaryStats(p, directionIsForward), statsEntry);  
			statsCache.put(newCacheEntry);
			
			return statsEntry.equals(NULL_STATS_ENTRY) ? null : (SummaryStatsEntry)(statsEntry.clone()); 

		} catch(IOException e) {

			log.error("error querying predicate stats db: ", e);
		}
		
		return null;
	}

	protected int getAverageResponseTime(Property p, boolean directionIsForward, int numInputs)
	{
		Element cacheEntry = statsCache.get(getCacheKeyForSamplesAverage(p, directionIsForward, numInputs));

		if(cacheEntry != null) {
			return (Integer)(cacheEntry.getObjectValue());
		}
		
		try {
			
			String responseTimePredicate = directionIsForward ? PredicateStats.AVERAGE_RESPONSE_TIME_FORWARD : PredicateStats.AVERAGE_RESPONSE_TIME_REVERSE;
			String direction = directionIsForward ? "forward" : "reverse";
			
			String queryTemplate = SPARQLStringUtils.readFully(PredicateStatsDB.class.getResource("get.average.response.time.sparql.template"));
			
			String query = SPARQLStringUtils.strFromTemplate(
					queryTemplate, 
					this.statsGraph,
					p.getURI(),
					String.valueOf(numInputs),
					responseTimePredicate);
					
			List<Map<String,String>> results = endpoint.selectQuery(query);
			
			Integer averageResponseTime;
			
			if(results.size() == 0) {
			
				log.debug(String.format("no response time average available for %d inputs %s in the %s direction", numInputs, p.getURI(), direction));
				averageResponseTime = NO_STATS_AVAILABLE;
			
			} else { 
			
				if(results.size() > 1) {
					log.error(String.format("stats db is corrupt! (more than one response time average is recorded for %d inputs to %s in the %s direction)", numInputs, p.getURI(), direction));
				}

				Map<String,String> firstRow = results.iterator().next();
				averageResponseTime = Integer.valueOf(firstRow.get("averageResponseTime"));
				
			}
			
			Element newCacheEntry = new Element(getCacheKeyForSamplesAverage(p, directionIsForward, numInputs), averageResponseTime);  
			statsCache.put(newCacheEntry);
			
			return averageResponseTime;

		} catch(IOException e) {

			log.error("error querying predicate stats db: ", e);
		}
		
		return NO_STATS_AVAILABLE;
	}
	
	protected static class SummaryStatsEntry
	{
		protected String predicate;
		protected boolean directionIsForward;
		protected int baseTime;
		protected int timePerInput;
		protected int numSamples;

		public SummaryStatsEntry(String predicate, boolean directionIsForward, int baseTime, int timePerInput, int numSamples)
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
			return new SummaryStatsEntry(predicate, directionIsForward, baseTime, timePerInput, numSamples);
		}
		
	}
	
}
