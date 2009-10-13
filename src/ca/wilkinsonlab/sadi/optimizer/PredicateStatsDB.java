package ca.wilkinsonlab.sadi.optimizer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.share.Config;
import ca.wilkinsonlab.sadi.sparql.VirtuosoSPARQLEndpoint;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.vocab.PredicateStats;
import ca.wilkinsonlab.sadi.vocab.W3C;

public class PredicateStatsDB extends VirtuosoSPARQLEndpoint 
{
	public final static Log LOGGER = LogFactory.getLog(PredicateStatsDB.class);
	
 	protected final static String CONFIG_ROOT = "share.statsdb";

 	protected final static String ENDPOINT_CONFIG_KEY = "endpoint";
	protected final static String GRAPH_CONFIG_KEY = "graph";
	protected final static String USERNAME_CONFIG_KEY = "username";
	protected final static String PASSWORD_CONFIG_KEY = "password";

	// remember, PredicateStats.INFINITY == -1
	public final static int NO_SAMPLES_AVAILABLE= -2;
	
	private int avgForwardSelectivity; 
	private int avgReverseSelectivity;
	private int avgForwardTime;
	private int avgReverseTime;

	private boolean avgStatsInitialized = false;

	private String graphName;
	
	public PredicateStatsDB() throws HttpException, IOException
	{
		this(Config.getConfiguration().subset(CONFIG_ROOT));
	}
	
	public PredicateStatsDB(Configuration config) throws IOException
	{
		super(config.getString(ENDPOINT_CONFIG_KEY), 
			config.getString(USERNAME_CONFIG_KEY),
			config.getString(PASSWORD_CONFIG_KEY));
		
		graphName = config.getString(GRAPH_CONFIG_KEY);
	}
	
	public String getGraphName()
	{
		return graphName;
	}
	
	/**
	 * Return true if the statistics database has been initialized with at least one sample
	 * of each type (forward selectivity, reverse selectivity, forward time, reverse time).
	 * 
	 * @return true if the statistics DB has at least one sample of each type, false otherwise.
	 */
	public boolean isPopulated() throws IOException 
	{
		return !(hasSample(false, false) &&
				hasSample(false, true) &&
				hasSample(true, false) &&
				hasSample(true, true));
	}
	
	private boolean hasSample(boolean statIsSelectivity, boolean directionIsForward) throws IOException
	{
		String samplePredicate = statIsSelectivity ? PredicateStats.PREDICATE_SELECTIVITYSAMPLE : PredicateStats.PREDICATE_TIMESAMPLE;		
		
		String query = 	
			"SELECT * FROM %u% WHERE {\n" +
			"    ?predicate %u% ?sample .\n" +
			"    ?sample %u% %v% .\n" +
			"    FILTER (?sample != %v%)" +
			"}\n" +
			"LIMIT 1";
		
		query = SPARQLStringUtils.strFromTemplate(query, 
				getGraphName(),
				samplePredicate,
				PredicateStats.PREDICATE_TIMESTAMP, String.valueOf(directionIsForward),
				String.valueOf(PredicateStats.INFINITY));
		
		return (selectQuery(query).size() == 1);
	}	
	
	public boolean statIsInfinity(String predicate, boolean statIsSelectivity, boolean directionIsForward) throws IOException
	{
		String query = 
			"SELECT ?infinity FROM %u%\n" +
			"WHERE {\n" +
			"   %u% %u% ?sample .\n" +
			"   ?sample %u% %s% . \n" +
			"   ?sample %u% ?infinity .\n" +
			"   FILTER (?infinity = %v%) \n" +
			"}";
		
		String samplePredicate = statIsSelectivity ? PredicateStats.PREDICATE_SELECTIVITYSAMPLE : PredicateStats.PREDICATE_TIMESAMPLE;
		String statPredicate = statIsSelectivity ? PredicateStats.PREDICATE_SELECTIVITY : PredicateStats.PREDICATE_TIME;

		query = SPARQLStringUtils.strFromTemplate(query, 
				getGraphName(),
				predicate,
				samplePredicate,
				PredicateStats.PREDICATE_DIRECTION_IS_FORWARD,
				String.valueOf(directionIsForward),
				statPredicate,
				String.valueOf(PredicateStats.INFINITY));

		List<Map<String,String>> results = selectQuery(query);
		
		return (results.size() > 0);
	}
	
	public int getAverageSampleValue(String predicate, boolean statIsSelectivity, boolean directionIsForward) throws IOException
	{
		if(statIsInfinity(predicate, statIsSelectivity, directionIsForward))
			return PredicateStats.INFINITY;
		
		String query = 
			"SELECT AVG(?stat) FROM %u%\n" +
			"WHERE {\n" +
			"   %u% %u% ?sample .\n" +
			"   ?sample %u% %s% .\n" +
			"   ?sample %u% ?stat .\n" +
			"}";

		String samplePredicate = statIsSelectivity ? PredicateStats.PREDICATE_SELECTIVITYSAMPLE : PredicateStats.PREDICATE_TIMESAMPLE;
		String statPredicate = statIsSelectivity ? PredicateStats.PREDICATE_SELECTIVITY : PredicateStats.PREDICATE_TIME;
		
		/*
		String samplePredicate;
		String statPredicate;
		if(statIsSelectivity) {
			samplePredicate = PredicateStats.PREDICATE_SELECTIVITYSAMPLE;
			statPredicate = PredicateStats.PREDICATE_SELECTIVITY;
		}
		else {
			samplePredicate = PredicateStats.PREDICATE_TIMESAMPLE;
			statPredicate = PredicateStats.PREDICATE_TIME;
		}
		*/

		query = SPARQLStringUtils.strFromTemplate(query, 
				getGraphName(),
				predicate,
				samplePredicate,
				PredicateStats.PREDICATE_DIRECTION_IS_FORWARD,
				String.valueOf(directionIsForward),
				statPredicate);
		
		List<Map<String,String>> results = selectQuery(query);
	
		if(results.size() == 0)
			throw new RuntimeException();
		
		// If the predicate has no stats in the DB
		if(!results.get(0).keySet().iterator().hasNext())
			return NO_SAMPLES_AVAILABLE;
		
		String columnHeader = results.get(0).keySet().iterator().next();
		return Integer.valueOf(results.get(0).get(columnHeader));
	}
	
	public int getPredicateStat(String predicate, boolean statIsSelectivity, boolean directionIsForward) throws IOException 
	{
		// Special case: We want to delay querying with an rdf:type for as long as possible
		if(predicate.equals(W3C.PREDICATE_RDF_TYPE) && !directionIsForward)
			return PredicateStats.INFINITY;
		
		int avg = getAverageSampleValue(predicate, statIsSelectivity, directionIsForward);
		
		if(avg == NO_SAMPLES_AVAILABLE)
			return getAverageStat(statIsSelectivity, directionIsForward);
		else
			return avg;
	}
	
	public int getAverageStat(boolean statIsSelectivity, boolean directionIsForward) throws IOException
	{
		if(!avgStatsInitialized) 
			initAverageStatValues();
		
		if(statIsSelectivity) {
			if(directionIsForward)
				return avgForwardSelectivity;
			else
				return avgReverseSelectivity;
		}
		else {
			if(directionIsForward)
				return avgForwardTime;
			else
				return avgReverseTime;
		}
	}
	
	private void initAverageStatValues() throws IOException 
	{
		String query = 
			"SELECT ?forwardSel ?reverseSel ?forwardTime ?reverseTime\n" +
			"FROM %u%\n" +
			"WHERE {\n" +
			"   %u% %u% ?forwardSel .\n" +
			"   %u% %u% ?reverseSel .\n" +
			"   %u% %u% ?forwardTime .\n" +
			"   %u% %u% ?reverseTime .\n" +
			"}";
		
		query = SPARQLStringUtils.strFromTemplate(query, 
				getGraphName(),
				getGraphName(),
				PredicateStats.PREDICATE_AVG_FORWARD_SELECTIVITY,
				getGraphName(),
				PredicateStats.PREDICATE_AVG_REVERSE_SELECTIVITY,
				getGraphName(),
				PredicateStats.PREDICATE_AVG_FORWARD_TIME,
				getGraphName(),
				PredicateStats.PREDICATE_AVG_REVERSE_TIME);
		
		List<Map<String,String>> results = selectQuery(query);
		
		if(results.size() == 0)
			throw new RuntimeException("Average values for selectivity and delay time are not present in the statistics DB");

		if(results.size() > 1)
			throw new RuntimeException("There is more than one value in the database for average selectivity and delay time.");
		
		Map<String,String> values = results.iterator().next();
		
		avgForwardSelectivity = Integer.valueOf(values.get("forwardSel"));
		avgReverseSelectivity = Integer.valueOf(values.get("reverseSel"));
		avgForwardTime = Integer.valueOf(values.get("forwardTime"));
		avgReverseTime = Integer.valueOf(values.get("reverseTime"));
		
		avgStatsInitialized = true;
	}

}
