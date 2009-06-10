package ca.wilkinsonlab.sadi.optimizer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.sparql.VirtuosoSPARQLEndpoint;
import ca.wilkinsonlab.sadi.sparql.VirtuosoSPARQLRegistry;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.vocab.PredicateStats;
import ca.wilkinsonlab.sadi.vocab.SPARQLRegistryOntology;

public class PredicateStatsDB extends VirtuosoSPARQLEndpoint {

	public final static Log LOGGER = LogFactory.getLog(PredicateStatsDB.class);

	// The registry of SPARQL endpoints.
	protected VirtuosoSPARQLRegistry registry;
	
	private int avgForwardSelectivity;
	private int avgReverseSelectivity;
	private int avgForwardTime;
	private int avgReverseTime;
	
	public PredicateStatsDB() throws HttpException, IOException
	{
		this(PredicateStats.DEFAULT_PREDSTATSDB_URI, SPARQLRegistryOntology.DEFAULT_REGISTRY_ENDPOINT);
	}
	
	public PredicateStatsDB(String statsDBURI, String sparqlRegistryURI) throws HttpException, IOException 
	{
		super(statsDBURI);
		registry = new VirtuosoSPARQLRegistry(sparqlRegistryURI);
		
		LOGGER.trace("Calculating average forward selectivity over all predicates");
		avgForwardSelectivity = calcAverageStat(true, true);
		LOGGER.trace("Calculating average reverse selectivity over all predicates");
		avgReverseSelectivity = calcAverageStat(true, false);
		LOGGER.trace("Calculating average forward time over all predicates");
		avgForwardTime = calcAverageStat(false, true);
		LOGGER.trace("Calculating average reverse time over all predicates");
		avgReverseTime = calcAverageStat(false, false);
		
	}
	
	public int getPredicateStat(String predicate, boolean statIsSelectivity, boolean directionIsForward) throws URIException, HttpException, IOException 
	{
		String query = 
			"SELECT AVG(?stat) FROM %u% " +
			"WHERE { " +
			"   %u% %u% ?sample . " +
			"   ?sample %u% %s% . " +
			"   ?sample %u% ?stat . " +
			"}";
		
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
		
		query = SPARQLStringUtils.strFromTemplate(query, 
				PredicateStats.GRAPH_PREDSTATS,
				predicate,
				samplePredicate,
				PredicateStats.PREDICATE_DIRECTION_IS_FORWARD,
				String.valueOf(directionIsForward),
				statPredicate);
		
		List<Map<String,String>> results = selectQuery(query);
		
		if(results.size() == 0)
			throw new RuntimeException();
		if(!results.get(0).keySet().iterator().hasNext())
			return getAverageStat(statIsSelectivity, directionIsForward);

		String columnHeader = results.get(0).keySet().iterator().next();
		return Integer.valueOf(results.get(0).get(columnHeader));
	}
	
	public int getAverageStat(boolean statIsSelectivity, boolean directionIsForward)
	{
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
	
	private int calcAverageStat(boolean statIsSelectivity, boolean directionIsForward) throws URIException, HttpException, IOException
	{
		String query = 
			"SELECT AVG(?stat) FROM %u% " +
			"WHERE { " +
			"   ?sample %u% %s% . " +
			"   ?sample %u% ?stat . " +
			"}";
		
		String statPredicate;
		if(statIsSelectivity)
			statPredicate = PredicateStats.PREDICATE_SELECTIVITY;
		else
			statPredicate = PredicateStats.PREDICATE_TIME;
		
		query = SPARQLStringUtils.strFromTemplate(query, 
				PredicateStats.GRAPH_PREDSTATS,
				PredicateStats.PREDICATE_DIRECTION_IS_FORWARD,
				String.valueOf(directionIsForward),
				statPredicate);
		
		List<Map<String,String>> results = selectQuery(query);
		
		/**
		 * NOTE: If the aggregate can't be computed 
		 * (e.g. trying to take the AVG() of a list of URIs)
		 * or there are no matching entries in the database,
		 * Virtuoso returns a result set of size 1 with no bindings. 
		 * 
		 * I don't know if this is standard behaviour across different
		 * types of SPARQL endpoints. -- BV
		 */
		if(results.size() == 0)
			throw new RuntimeException();
		if(!results.get(0).keySet().iterator().hasNext())
			return 0;
			
		String columnHeader = results.get(0).keySet().iterator().next();
		return Integer.valueOf(results.get(0).get(columnHeader));
	}

}
