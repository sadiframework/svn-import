package ca.wilkinsonlab.sadi.optimizer;

import gnu.getopt.Getopt;

import java.io.IOException;
import java.rmi.AccessException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.ontology.OntModel;

import ca.wikinsonlab.sadi.client.QueryClient;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.HttpUtils;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;
import ca.wilkinsonlab.sadi.vocab.SPARQLRegistryOntology;
import ca.wilkinsonlab.sadi.vocab.PredicateStats;
import ca.wilkinsonlab.sadi.pellet.PelletClient;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.sparql.SPARQLService;

public class PredicateStatsDBAdmin extends PredicateStatsDB {

	public final static Log LOGGER = LogFactory.getLog(PredicateStatsDBAdmin.class);
	
	private QueryClient queryClient = new PelletClient();

	// in milliseconds
	private final int SADI_QUERY_TIMEOUT = 60 * 1000;
	private final int ENDPOINT_QUERY_TIMEOUT = 30 * 1000;
	
	public PredicateStatsDBAdmin() throws HttpException, IOException
	{
		super(PredicateStats.DEFAULT_PREDSTATSDB_URI, SPARQLRegistryOntology.DEFAULT_REGISTRY_ENDPOINT);
	}
	
	public void computeStats(int selectivitySamplesPerPredicate, int timeSamplesPerPredicate, boolean resume) throws IOException 
	{
		OntModel ontology = registry.getPredicateOntology();
		Iterator it = ontology.listAllOntProperties();
		while(it.hasNext()) {
			String predicate = it.next().toString();
			LOGGER.trace("Sampling stats for: " + predicate);
			int numSelectivitySamples = 0;
			int numTimeSamples = 0;
			if(resume) {
				numSelectivitySamples = getNumSelectivitySamples(predicate);
				numTimeSamples = getNumTimeSamples(predicate);
				
				if(numSelectivitySamples > selectivitySamplesPerPredicate && numTimeSamples > timeSamplesPerPredicate)
					continue;
			}

			try {
				computeStatsForPredicate(predicate, selectivitySamplesPerPredicate, timeSamplesPerPredicate);
			}
			catch(Exception e) {
				LOGGER.error("Failure during statistics collection for predicate " + predicate + ": " + e);
			}
			
		}
	}
	
	
	public void computeStatsForPredicate(String predicate, int selectivitySamples, int timeSamples) throws HttpException, HttpResponseCodeException, IOException
	{
		Collection<Service> endpoints = registry.findServicesByPredicate(predicate);

		/*
		int selectivitySamplesPerEndpoint = (int)Math.ceil( ((double)selectivitySamples)/ ((double)endpoints.size()) );
		int timeSamplesPerEndpoint = (int)Math.ceil( ((double)timeSamples)/ ((double)endpoints.size()) );
		*/
		
		Map<String,Long> numTriples = new HashMap<String,Long>();
		long totalTriples = 0;

		// Sample each endpoint proportionately to the number of triples it contains with the predicate of interest
		for(Service endpoint : endpoints) {
			String endpointURI = endpoint.getServiceURI();
			boolean endpointIsSlow = (registry.getServiceStatus(endpointURI) == ServiceStatus.SLOW);
			long triples = getNumTriplesForPredicate((SPARQLService)endpoint, predicate, endpointIsSlow);
			numTriples.put(endpointURI, Long.valueOf(triples));
			totalTriples += triples;
		}
		
		for(Service endpoint : endpoints) {
			String endpointURI = endpoint.getServiceURI();
			long triples = numTriples.get(endpointURI);
			int selectivitySamplesForEndpoint = (int)Math.ceil( ((float)triples * selectivitySamples) / totalTriples );  
			int timeSamplesForEndpoint =  (int)Math.ceil( ((float)triples * timeSamples) / totalTriples );
			
			LOGGER.trace("Sampling endpoint " + endpointURI + " for stats about " + predicate);
			if(registry.getServiceStatus(endpointURI) == ServiceStatus.DEAD)
				continue;

			try {
				sampleForwardSelectivity(predicate, selectivitySamplesForEndpoint, triples, (SPARQLService)endpoint);
				sampleReverseSelectivity(predicate, selectivitySamplesForEndpoint, triples, (SPARQLService)endpoint);
				sampleForwardTime(predicate, timeSamplesForEndpoint, triples, (SPARQLService)endpoint);
				sampleReverseTime(predicate, timeSamplesForEndpoint, triples, (SPARQLService)endpoint);
			}
			catch(Exception e) {
				LOGGER.error("Failure during statistics collection for predicate " + predicate 
						+ ", endpoint " + endpointURI + ": " + e);
			}
		}
	}

	public String getSubjectSample(long upperSampleLimit, String predicateURI, SPARQLService endpoint) throws URIException, HttpException, IOException 
	{
		RandomData generator = new RandomDataImpl();
		long sampleIndex = upperSampleLimit > 1 ? generator.nextLong(0, upperSampleLimit) : 0;
		String querySubject = "SELECT ?s WHERE { ?s %u% ?o } OFFSET %v% LIMIT 1";
		querySubject = SPARQLStringUtils.strFromTemplate(querySubject, predicateURI, String.valueOf(sampleIndex));
		LOGGER.trace("Sampling subject " + sampleIndex + " for predicate " + predicateURI + ", endpoint " + endpoint.getServiceURI());
		LOGGER.debug("query: " + querySubject);
		List<Map<String,String>> results = endpoint.selectQuery(querySubject, ENDPOINT_QUERY_TIMEOUT);
		if(results.size() == 0) {
			throw new IllegalArgumentException("Caller asked for subject # " + sampleIndex + " for predicate " 
					+ predicateURI + ", but that number exceeds the number of distinct subjects for " + predicateURI );
		}
		return results.iterator().next().get("s");
	}

	public String getObjectSample(long upperSampleLimit, String predicateURI, SPARQLService endpoint) throws URIException, HttpException, IOException 
	{
		RandomData generator = new RandomDataImpl();
		long sampleIndex = upperSampleLimit > 1 ? generator.nextLong(0, upperSampleLimit) : 0;
		String queryObject = "SELECT ?o WHERE { ?s %u% ?o } OFFSET %v% LIMIT 1";
		queryObject = SPARQLStringUtils.strFromTemplate(queryObject, predicateURI, String.valueOf(sampleIndex));
		LOGGER.trace("Sampling object " + sampleIndex + " for predicate " + predicateURI + ", endpoint " + endpoint.getServiceURI());
		LOGGER.debug("query: " + queryObject);
		List<Map<String,String>> results = endpoint.selectQuery(queryObject, ENDPOINT_QUERY_TIMEOUT);
		if(results.size() == 0) {
			throw new IllegalArgumentException("You asked fors object # " + sampleIndex + " for predicate " 
					+ predicateURI + ", but that number exceeds the number of distinct object for " + predicateURI );
		}
		return results.iterator().next().get("o");
	}
	
	private int getNumSelectivitySamples(String predicate) throws URIException, HttpException, HttpResponseCodeException, IOException
	{
		String query = "SELECT COUNT(?sample) FROM %u% WHERE { %u% %u% ?sample }";
		query = SPARQLStringUtils.strFromTemplate(query, 
				PredicateStats.GRAPH_PREDSTATS,
				predicate,
				PredicateStats.PREDICATE_SELECTIVITYSAMPLE);
		LOGGER.debug("query: " + query);
		List<Map<String,String>> results = selectQuery(query);
		if(results.size() == 0)
			throw new RuntimeException();
		String resultVar = results.get(0).keySet().iterator().next();
		return Integer.valueOf(results.get(0).get(resultVar));
	}

	private int getNumTimeSamples(String predicate) throws URIException, HttpException, HttpResponseCodeException, IOException
	{
		String query = "SELECT COUNT(?sample) FROM %u% WHERE { %u% %u% ?sample }";
		query = SPARQLStringUtils.strFromTemplate(query, 
				PredicateStats.GRAPH_PREDSTATS,
				predicate,
				PredicateStats.PREDICATE_TIMESAMPLE);
		LOGGER.debug("query: " + query);
		List<Map<String,String>> results = selectQuery(query);
		if(results.size() == 0)
			throw new RuntimeException();
		String resultVar = results.get(0).keySet().iterator().next();
		return Integer.valueOf(results.get(0).get(resultVar));
	}
	
	public void sampleForwardTime(String predicate, int numSamples, long upperSampleLimit, SPARQLService endpoint) throws HttpException, HttpResponseCodeException, IOException
	{
		LOGGER.trace("Sampling forward time of " + predicate + " in " + endpoint.getServiceURI());
		for(int i = 0; i < numSamples && i <= upperSampleLimit; i++) {
			String s = getSubjectSample(upperSampleLimit, predicate, endpoint);
			String query = "SELECT * WHERE { %u% %u% ?o }";
			query = SPARQLStringUtils.strFromTemplate(query, s, predicate);
			long startTime =  getTime(); 
			LOGGER.debug("SADI query: " + query);
			queryClient.synchronousQuery(query, SADI_QUERY_TIMEOUT);
			// in milliseconds
			int forwardTime = (int)(getTime() - startTime);
			LOGGER.debug("Stats for endpoint " + endpoint.getURI() + ", predicate " + predicate + 
					": Sample #" + i + ", subject " + s + " has forward time " + String.valueOf(forwardTime));
			recordTimeSample(predicate, forwardTime, true);
		}
	}

	public void sampleReverseTime(String predicate, int numSamples, long upperSampleLimit, SPARQLService endpoint) throws HttpException, HttpResponseCodeException, IOException
	{
		LOGGER.trace("Sampling reverse time of " + predicate + " in " + endpoint.getServiceURI());
		for(int i = 0; i < numSamples && i <= upperSampleLimit; i++) {
			String o = getObjectSample(upperSampleLimit, predicate, endpoint);
			String query;
			if(!registry.isDatatypeProperty(predicate))
				query =  "SELECT * WHERE { ?s %u% %u% }";
			else {
				if(NumberUtils.isNumber(o))
					query = "SELECT * WHERE { ?s %u% %v% }";
				else
					query = "SELECT * WHERE { ?s %u% %s% }";
			}
			query = SPARQLStringUtils.strFromTemplate(query, predicate, o);
			long startTime = getTime();
			LOGGER.debug("SADI query: " + query);
			//queryClient.synchronousQuery(timeQuery);
			queryClient.synchronousQuery(query, SADI_QUERY_TIMEOUT);
			// in milliseconds
			int reverseTime = (int)(getTime() - startTime);
			LOGGER.debug("Stats for endpoint " + endpoint.getURI() + ", predicate " + predicate + 
					": Sample #" + i + ", object " + o + " has reverse time " + String.valueOf(reverseTime));
			recordTimeSample(predicate, reverseTime, false);
		}
	}
	
	public void recordTimeSample(String predicate, int timeSample, boolean directionIsForward) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException
	{
		long time = getTime();
		
		String query = "INSERT INTO GRAPH %u% { " +
				"%u% %u% %u% . " +
				"%u% %u% %s% . " +
				"%u% %u% %v% . " +
				"%u% %u% %v% . }";
		
		String sampleNodeURI = predicate + String.valueOf(time);
		
		query = SPARQLStringUtils.strFromTemplate(query, PredicateStats.GRAPH_PREDSTATS,
				predicate, PredicateStats.PREDICATE_TIMESAMPLE, sampleNodeURI,
				sampleNodeURI, PredicateStats.PREDICATE_DIRECTION_IS_FORWARD, String.valueOf(directionIsForward),
				sampleNodeURI, PredicateStats.PREDICATE_TIMESTAMP, String.valueOf(time),
				sampleNodeURI, PredicateStats.PREDICATE_TIME, String.valueOf(timeSample));

		LOGGER.debug("query: " + query);
		updateQuery(query);
	}

	public void sampleForwardSelectivity(String predicate, int numSamples, long upperSampleLimit, SPARQLService endpoint) throws HttpException, HttpResponseCodeException, IOException
	{
		LOGGER.trace("Sampling forward selectivity of " + predicate + " in " + endpoint.getServiceURI());
		//for(int i = 0; i < numSamples && i <= samplingLimits.getSubjectUpperLimit(predicate, endpoint.getServiceURI()); i++) {
		for(int i = 0; i < numSamples && i <= upperSampleLimit; i++) {
			String s = getSubjectSample(upperSampleLimit, predicate, endpoint);
			//String s = getSubjectSample(predicate, endpoint);
			long forwardSelectivity = getPredicateCountForSubject(endpoint, s, predicate);
			LOGGER.debug("Stats for endpoint " + endpoint.getURI() + ", predicate " + predicate + 
					": Sample #" + i + ", subject " + s + " has forward selectivity " + String.valueOf(forwardSelectivity));
			recordSelectivitySample(predicate, forwardSelectivity, true);
		}
	}

	public void sampleReverseSelectivity(String predicate, int numSamples, long upperSampleLimit, SPARQLService endpoint) throws HttpException, HttpResponseCodeException, IOException
	{
		LOGGER.trace("Sampling reverse selectivity of " + predicate + " in " + endpoint.getServiceURI());
		for(int i = 0; i < numSamples && i <= upperSampleLimit; i++) {
			String o = getObjectSample(upperSampleLimit, predicate, endpoint);
			long reverseSelectivity = getPredicateCountForObject(endpoint, o, predicate);
			
			// (reverseSelectivity == 0) when the registry has incorrectly identified a predicate as
			// an object property, whereas it is really a datatype property that usually takes
			// strings which are URIs.  (As far as I am aware, there is no way for the registry to 
			// automatically discern between these two cases.)
			// 
			// If this mistake is made by the registry, then we will get no results back
			// from getPredicateCountForObject above.  Give up and carry on to the next predicate.
			
			if(reverseSelectivity == 0) {
				LOGGER.warn("The predicate " + predicate + " has been incorrectly typed by the registry as an object property.");
				return;
			}
			
			LOGGER.debug("Stats for endpoint " + endpoint.getURI() + ", predicate " + predicate + 
					": Sample #" + i + ", object " + o + " has reverse selectivity " + String.valueOf(reverseSelectivity));
			recordSelectivitySample(predicate, reverseSelectivity, false);
		}
	}

	public void recordSelectivitySample(String predicate, long selectivitySample, boolean directionIsForward) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException
	{
		long time = getTime();
		
		String query = "INSERT INTO GRAPH %u% { " +
				"%u% %u% %u% . " +
				"%u% %u% %s% . " +
				"%u% %u% %v% . " +
				"%u% %u% %v% . }";
		
		String sampleNodeURI = predicate + String.valueOf(time);
		
		query = SPARQLStringUtils.strFromTemplate(query, PredicateStats.GRAPH_PREDSTATS,
				predicate, PredicateStats.PREDICATE_SELECTIVITYSAMPLE, sampleNodeURI,
				sampleNodeURI, PredicateStats.PREDICATE_DIRECTION_IS_FORWARD, String.valueOf(directionIsForward),
				sampleNodeURI, PredicateStats.PREDICATE_TIMESTAMP, String.valueOf(time),
				sampleNodeURI, PredicateStats.PREDICATE_SELECTIVITY, String.valueOf(selectivitySample));

		LOGGER.debug("query: " + query);
		updateQuery(query);
	}
	
	public long getNumTriplesForPredicate(SPARQLService endpoint, String predicateURI, boolean slowEndpoint) throws HttpException, HttpResponseCodeException, IOException
	{
		LOGGER.trace("Getting number of triples containing predicate " + predicateURI);
		String queryNumSubjects = "SELECT COUNT(?s) WHERE { ?s %u% ?o }";
		queryNumSubjects = SPARQLStringUtils.strFromTemplate(queryNumSubjects, predicateURI);
		List<Map<String,String>> results = null;
		if(!slowEndpoint) {
			try {
				results = endpoint.selectQuery(queryNumSubjects, ENDPOINT_QUERY_TIMEOUT);
				Map<String,String> binding = results.iterator().next();
				return Long.parseLong(binding.get(binding.keySet().iterator().next()));
			} 
			catch(IOException e) {
				LOGGER.trace("Failed to COUNT number of triples containing " + predicateURI + ". Trying for a lower bound instead.");
			}
		}
		String lowerBoundQuery = "SELECT ?s WHERE { ?s %u% ?o }";
		lowerBoundQuery = SPARQLStringUtils.strFromTemplate(lowerBoundQuery, predicateURI);
		return endpoint.getResultsCountLowerBound(lowerBoundQuery, 50000);
	}

	public long getPredicateCountForSubject(SPARQLService endpoint, String subjectURI, String predicateURI) throws HttpException, HttpResponseCodeException, IOException 
	{
		String queryPredCount = "SELECT COUNT(?o) WHERE { %u% %u% ?o }";
		queryPredCount = SPARQLStringUtils.strFromTemplate(queryPredCount, subjectURI, predicateURI);
		LOGGER.debug("query: " + queryPredCount);
		List<Map<String,String>> results;
		try {
			results = endpoint.selectQuery(queryPredCount, ENDPOINT_QUERY_TIMEOUT);
			if(results.size() == 0)
				throw new RuntimeException();
			Map<String,String> binding = results.iterator().next();
			return Integer.parseInt(binding.get(binding.keySet().iterator().next()));
		}
		catch(HttpResponseCodeException e) {
			if(!HttpUtils.isHTTPTimeout(e))
				throw e;
		}
		catch(IOException e) {
			if(!HttpUtils.isHTTPTimeout(e))
				throw e;
		}
		
		// If we fail to get a proper predicate count, try to get a lower bound instead.
		String queryLowerBound = "SELECT ?o WHERE { %u% %u% ?o }";
		queryLowerBound = SPARQLStringUtils.strFromTemplate(queryLowerBound, subjectURI, predicateURI);
		long lowerBound = endpoint.getResultsCountLowerBound(queryLowerBound, 50000);
		return lowerBound;  
	}
	
	public long getPredicateCountForObject(SPARQLService endpoint, String object, String predicateURI) throws HttpException, HttpResponseCodeException, IOException 
	{
		String queryPredCount;
		String queryLowerBound;
		if(!registry.isDatatypeProperty(predicateURI)) {
			queryPredCount =  "SELECT COUNT(?s) WHERE { ?s %u% %u% }";
			queryLowerBound = "SELECT ?s WHERE { ?s %u% %u% }";
		}
		else {
			if(NumberUtils.isNumber(object)) {
				queryPredCount = "SELECT COUNT(?s) WHERE { ?s %u% %v% }";
				queryLowerBound = "SELECT ?s WHERE { ?s %u% %v% }";
			}
			else {
				queryPredCount = "SELECT COUNT(?s) WHERE { ?s %u% %s% }";
				queryLowerBound = "SELECT ?s WHERE { ?s %u% %s% }";
			}
		}
		queryPredCount = SPARQLStringUtils.strFromTemplate(queryPredCount, predicateURI, object);
		queryLowerBound = SPARQLStringUtils.strFromTemplate(queryLowerBound, predicateURI, object);

		LOGGER.debug("query: " + queryPredCount);
		List<Map<String,String>> results;
		try {
			results = endpoint.selectQuery(queryPredCount, ENDPOINT_QUERY_TIMEOUT);
			if(results.size() == 0)
				throw new RuntimeException();
			Map<String,String> binding = results.iterator().next();
			return Integer.parseInt(binding.get(binding.keySet().iterator().next()));
		}
		catch(HttpResponseCodeException e) {
			if(!HttpUtils.isHTTPTimeout(e))
				throw e;
		}
		catch(IOException e) {
			if(!HttpUtils.isHTTPTimeout(e))
				throw e;
		}

		// If we fail to get a proper predicate count, try to get a lower bound instead.
		long lowerBound = endpoint.getResultsCountLowerBound(queryLowerBound, 50000);
		return lowerBound;  
	}
	
	private long getTime() { return new Date().getTime(); }
	
	public void clearStats() throws HttpException, HttpResponseCodeException, IOException, AccessException 
	{
		String query = SPARQLStringUtils.strFromTemplate("CLEAR GRAPH %u%", PredicateStats.GRAPH_PREDSTATS);
		updateQuery(query);
	}

	public static void main(String[] args) 
	{
		try {
			Getopt options = new Getopt("predstats", args, "p:a");
			PredicateStatsDBAdmin statsDB;
			statsDB = new PredicateStatsDBAdmin();
			int option;
			String arg; 
			while ((option = options.getopt()) != -1) { 
				switch(option) 
				{ 
				case 'p': 
					arg = options.getOptarg();
					statsDB.computeStatsForPredicate(arg, 3, 3);
					break;
				case 'a':
					statsDB.computeStats(5, 3, true);
					break;
				case '?': 
					break; // getopt() already printed an error 
				default: 
					System.out.println("unrecognized option: -" + option);
				} 
			}		
		}
		catch(Exception e) {
			System.out.println("Error:");
			e.printStackTrace();
		}
		
	}

}
