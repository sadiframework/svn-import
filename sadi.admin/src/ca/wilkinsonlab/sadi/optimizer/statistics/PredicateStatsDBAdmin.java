package ca.wilkinsonlab.sadi.optimizer.statistics;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntModel;
import com.ibm.icu.text.DateFormat;

import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.vocab.PredicateStats;
import ca.wilkinsonlab.sadi.client.QueryClient;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.sparql.SPARQLRegistry;
import ca.wilkinsonlab.sadi.sparql.VirtuosoSPARQLEndpoint;
import ca.wilkinsonlab.sadi.share.Config;
import ca.wilkinsonlab.sadi.share.SHAREQueryClient;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class PredicateStatsDBAdmin extends VirtuosoSPARQLEndpoint
{
	public final static Log log = LogFactory.getLog(PredicateStatsDBAdmin.class);

	protected final static String CONFIG_ROOT = "share.statsdb";
	protected final static String ENDPOINT_CONFIG_KEY = "endpoint";
	protected final static String GRAPH_CONFIG_KEY = "graph";
	protected final static String USERNAME_CONFIG_KEY = "username";
	protected final static String PASSWORD_CONFIG_KEY = "password";
	
	private QueryClient queryClient = new SHAREQueryClient();

	protected final static String SPARQL_RESULTS_LIMIT_CONFIG_KEY = "sadi.sparql.resultsLimit";
	private static final int SPARQL_RESULTS_LIMIT = 500;
	
	// in milliseconds
	private static final int SADI_QUERY_TIMEOUT = 60 * 1000;
	/*
	private static final int ENDPOINT_QUERY_TIMEOUT = 30 * 1000;
	*/
	
	private String graphName;

	Map<String, Boolean> isDatatypePropertyCache = new HashMap<String, Boolean>();
	private SPARQLRegistry registry;

	public PredicateStatsDBAdmin() throws HttpException, IOException
	{
		super(Config.getConfiguration().subset(CONFIG_ROOT).getString(ENDPOINT_CONFIG_KEY), Config.getConfiguration().subset(CONFIG_ROOT).getString(USERNAME_CONFIG_KEY), Config.getConfiguration().subset(CONFIG_ROOT).getString(PASSWORD_CONFIG_KEY));

		setGraphName(Config.getConfiguration().subset(CONFIG_ROOT).getString(GRAPH_CONFIG_KEY));
		setRegistry(ca.wilkinsonlab.sadi.client.Config.getSPARQLRegistry());

		if (getRegistry() == null)
			throw new RuntimeException("generation of predicate stats requires use of the SPARQL registry, but the SPARQL registry was not successfully initialized.");
	}

	public String getGraphName() { return graphName; }
	public void setGraphName(String graphName) { this.graphName = graphName; }

	public SPARQLRegistry getRegistry() { return registry; }
	public void setRegistry(SPARQLRegistry registry) { 	this.registry = registry; }

	public void computeStatsForAllPredicates(int samplesPerPredicate, Date stalenessDate) throws IOException
	{
		OntModel ontology = registry.getPredicateOntology();
		Iterator it = ontology.listAllOntProperties();

		while (it.hasNext()) {
			String predicate = it.next().toString();
			computeStatsForPredicate(predicate, samplesPerPredicate, stalenessDate);
		}
		updateAverageStats();
	}

	public void computeStatsForEndpoint(String endpointURI, int numSamples, Date stalenessDate) throws IOException
	{
		Collection<String> predicates = getRegistry().getPredicatesForEndpoint(endpointURI);
		for (String predicate : predicates)
			computeStatsForPredicate(predicate, numSamples, stalenessDate);
		updateAverageStats();
	}

	public void computeStatsForPredicate(String predicate, int samplesPerPredicate, Date stalenessDate) throws IOException
	{
		log.trace("Sampling stats for predicate " + predicate);

		// Remove stale stats
		removeStatsForPredicate(predicate, stalenessDate);

		// Run as many queries as is necessary to make the 
		// number of samples of each type >= numSamples.
		int minSamples = getNumSamples(predicate, false, false);
		minSamples = Math.min(minSamples, getNumSamples(predicate, false, true));
		minSamples = Math.min(minSamples, getNumSamples(predicate, true, false));
		minSamples = Math.min(minSamples, getNumSamples(predicate, true, true));
		samplesPerPredicate = Math.max(samplesPerPredicate - minSamples, 0);

		// compute forward stats
		computeStatsForPredicate(predicate, samplesPerPredicate, true);
		// compute reverse stats
		computeStatsForPredicate(predicate, samplesPerPredicate, false);
	}

	public void computeStatsForPredicate(String predicate, int samplesPerPredicate, boolean directionIsForward)
	{
		InputSampler sampler = new InputSampler(getRegistry());

		String direction = directionIsForward ? "forward" : "reverse";
		String inputDesc = directionIsForward ? "subject" : "object";

		for (int i = 0; i < samplesPerPredicate; i++) {

			Node input = null;
			try {
				input = directionIsForward ? sampler.sampleSubject(predicate) : sampler.sampleObject(predicate);
			}
			catch (ExceededMaxAttemptsException e) {
				log.warn("aborted sampling of " + direction + " stats for " + predicate, e);
				break;
			}
			catch (NoSampleAvailableException e) {
				log.warn("aborted sampling of " + direction + " stats for " + predicate, e);
				break;
			}
			catch (IOException e) {
				log.warn("aborted sampling of " + direction + " stats for " + predicate, e);
				break;
			}

			try {
				computeStatsForSampleInput(input, predicate, directionIsForward);
			}
			catch (IOException e) {
				log.warn("failed to record stats for " + inputDesc + " " + input.toString() + " (predicate " + predicate + ")", e);
			}
	
		}

	}

	public void computeStatsForSampleInput(Node input, String predicate, boolean inputIsSubject) throws IOException
	{
		if (input.isBlank())
			throw new IllegalArgumentException("cannot gather stats for a blank node");

		String desc = inputIsSubject ? "subject" : "object";
		log.trace("sampling stats for " + desc + " " + input.toString() + " (predicate " + predicate + ")");

		Node var = NodeCreateUtils.create("?var");
		
		Triple pattern = null;
		Node p = NodeCreateUtils.create(predicate);
		
		if(inputIsSubject)
			pattern = new Triple(input, p, var);
		else
			pattern = new Triple(var, p, input);
		
		String query = "SELECT * WHERE { " + SPARQLStringUtils.getTriplePattern(pattern) + " }";
		
		/*
		if (inputIsSubject) {
			query = "SELECT * WHERE { %u% %u% ?o }";
			query = SPARQLStringUtils.strFromTemplate(query, input.getURI(), predicate);
		}
		else {

			if (input.isURI()) {
				query = "SELECT * WHERE { ?s %u% %u% }";
				query = SPARQLStringUtils.strFromTemplate(query, predicate, input.toString());
			}
			else if (input.isLiteral()) {
				query = "SELECT * WHERE { ?s %u% %s% }";
				query = SPARQLStringUtils.strFromTemplate(query, predicate, input.getLiteralValue().toString());
			}
			else
				throw new RuntimeException("unexpected node type");
		}
		*/
		
		recordStatsForQuery(query, predicate, inputIsSubject);
	}

	public void recordStatsForQuery(String query, String predicate, boolean directionIsForward) throws IOException
	{
		log.trace("executing test query: " + query);
		
		long startTime = getTime();
		List<Map<String, String>> results = SADIQuery(query); //queryClient.synchronousQuery(query, SADI_QUERY_TIMEOUT); 
		long endTime = getTime();
		
		Runtime.getRuntime().gc();
		
		int selectivity, time;

		if(results.size() == 0) {
			log.warn("zero query results, skipping recording of this stat for " + predicate);
			return;
		}
		else if(results.size() >= SPARQL_RESULTS_LIMIT) {
			time = PredicateStats.INFINITY;
			selectivity = PredicateStats.INFINITY;
		}
		else {
			time = (int) (endTime - startTime);
			selectivity = results.size();
		}

		recordSelectivitySample(predicate, selectivity, directionIsForward);
		recordTimeSample(predicate, time, directionIsForward);
	}

	private List<Map<String,String>> SADIQuery(String query) throws IOException 
	{
		ca.wilkinsonlab.sadi.client.Config config = ca.wilkinsonlab.sadi.client.Config.getConfiguration();

		// Temporarily limit the number of results for queries on individual endpoints
		boolean limitSet = config.containsKey(SPARQL_RESULTS_LIMIT_CONFIG_KEY);
		long origLimit = limitSet ? config.getLong(SPARQL_RESULTS_LIMIT_CONFIG_KEY) : -1;
		config.setProperty(SPARQL_RESULTS_LIMIT_CONFIG_KEY, SPARQL_RESULTS_LIMIT);
		
		List<Map<String, String>> results = queryClient.synchronousQuery(query);
		
		// Restore the original limit value, if any.
		if(limitSet)
			config.setProperty(SPARQL_RESULTS_LIMIT_CONFIG_KEY, origLimit);
		else
			config.clearProperty(SPARQL_RESULTS_LIMIT_CONFIG_KEY);
			
		return results; 
	}

	/*
	public void computeStatsForPredicate(String predicate, int selectivitySamples, int timeSamples, Date stalenessDate) throws IOException
	{
		log.trace("Sampling stats for: " + predicate);

		// Remove any samples that are equal to or older than the given staleness date. 
		// Then, take enough new samples to match the requested total number of 
		// selectivity/time samples.
		removeStatsForPredicate(predicate, stalenessDate);
		selectivitySamples = Math.max(selectivitySamples - getNumSelectivitySamples(predicate), 0);
		timeSamples = Math.max(timeSamples - getNumTimeSamples(predicate), 0);

		List<SPARQLEndpoint> endpoints = new ArrayList<SPARQLEndpoint>(registry.findEndpointsByPredicate(predicate));

		Map<String, Long> numTriples = new HashMap<String, Long>();
		RandomData generator = new RandomDataImpl();

		for (int i = 0; i < selectivitySamples; i++) {

			int endpointIndex = endpoints.size() > 1 ? generator.nextInt(0, endpoints.size() - 1) : 0;
			SPARQLEndpoint endpoint = endpoints.get(endpointIndex);
			String uri = endpoint.getURI();
			log.trace("Randomly selected endpoint " + uri + " for forward & reverse selectivity sample #" + String.valueOf(i));

			boolean endpointIsSlow = (getRegistry().getServiceStatus(uri) == ServiceStatus.SLOW);
			if (!numTriples.containsKey(uri))
				numTriples.put(uri, getNumTriplesForPredicate(endpoint, predicate, endpointIsSlow));
			long triples = numTriples.get(uri);
			try {
				sampleForwardSelectivity(predicate, 1, triples, endpoint);
				sampleReverseSelectivity(predicate, 1, triples, endpoint);
			}
			catch (Exception e) {
				log.error("failure while sampling selectivity for " + predicate + ", endpoint " + uri, e);
			}
		}

		for (int i = 0; i < timeSamples; i++) {
			int endpointIndex = endpoints.size() > 1 ? generator.nextInt(0, endpoints.size() - 1) : 0;
			SPARQLEndpoint endpoint = endpoints.get(endpointIndex);
			String uri = endpoint.getURI();
			log.trace("Randomly selected endpoint " + uri + " for forward & reverse selectivity sample #" + String.valueOf(i));

			boolean endpointIsSlow = (getRegistry().getServiceStatus(uri) == ServiceStatus.SLOW);
			if (!numTriples.containsKey(uri))
				numTriples.put(uri, getNumTriplesForPredicate(endpoint, predicate, endpointIsSlow));
			long triples = numTriples.get(uri);
			try {
				sampleForwardTime(predicate, 1, triples, endpoint);
				sampleReverseTime(predicate, 1, triples, endpoint);
			}
			catch (Exception e) {
				log.error("failure while sampling query time for " + predicate + ", endpoint " + uri, e);
			}
		}
	}
	*/
	
	/*
	public String getSubjectSample(long upperSampleLimit, String predicateURI, SPARQLEndpoint endpoint) throws URIException, HttpException, IOException
	{
		RandomData generator = new RandomDataImpl();
		long sampleIndex = upperSampleLimit > 1 ? generator.nextLong(0, upperSampleLimit - 1) : 0;
		String query = "CONSTRUCT { ?s %u% ?o } WHERE { ?s %u% ?o . FILTER (!isBlank(?s)) } OFFSET %v% LIMIT 1";
		query = SPARQLStringUtils.strFromTemplate(query, predicateURI, predicateURI, String.valueOf(sampleIndex));

		log.trace("Sampling subject " + sampleIndex + " for predicate " + predicateURI + ", endpoint " + endpoint.getURI());
		List<Map<String, String>> results = endpoint.selectQuery(query, ENDPOINT_QUERY_TIMEOUT);
		if (results.size() == 0) {
			throw new IllegalArgumentException("Caller asked for subject # " + sampleIndex + " for predicate " + predicateURI + ", but that number exceeds the number of distinct subjects for " + predicateURI);
		}
		return results.iterator().next().get("s");
	}

	public String getObjectSample(long upperSampleLimit, String predicateURI, SPARQLEndpoint endpoint) throws URIException, HttpException, IOException
	{
		RandomData generator = new RandomDataImpl();
		long sampleIndex = upperSampleLimit > 1 ? generator.nextLong(0, upperSampleLimit - 1) : 0;
		String query = "SELECT ?o WHERE { ?s %u% ?o . FILTER(!isBlank(?o)) } OFFSET %v% LIMIT 1";
		query = SPARQLStringUtils.strFromTemplate(query, predicateURI, String.valueOf(sampleIndex));

		log.trace("Sampling object " + sampleIndex + " for predicate " + predicateURI + ", endpoint " + endpoint.getURI());
		List<Map<String, String>> results = endpoint.selectQuery(query, ENDPOINT_QUERY_TIMEOUT);
		if (results.size() == 0) {
			throw new IllegalArgumentException("Caller asked for object # " + sampleIndex + " for predicate " + predicateURI + ", but that number exceeds the number of distinct object for " + predicateURI);
		}
		return results.iterator().next().get("o");
	}
	*/
	
	private int getNumSamples(String predicate, boolean sampleIsSelectivity, boolean directionIsForward) throws IOException
	{
		String samplePredicate = sampleIsSelectivity ? PredicateStats.PREDICATE_SELECTIVITYSAMPLE : PredicateStats.PREDICATE_TIMESAMPLE;

		String query = 
			"SELECT COUNT(*) FROM %u% \n" + 
			"WHERE {\n" + 
			"     %u% %u% ?sample .\n" + 
			"     ?sample %u% %s% .\n" + 
			" }";

		query = SPARQLStringUtils.strFromTemplate(query, 
					getGraphName(), 
					predicate, samplePredicate, 
					PredicateStats.PREDICATE_DIRECTION_IS_FORWARD, String.valueOf(directionIsForward));

		List<Map<String, String>> results = selectQuery(query);

		if (results.size() == 0)
			throw new RuntimeException("empty result set from COUNT query");

		Map<String, String> firstRow = results.iterator().next();
		String firstColumn = firstRow.keySet().iterator().next();

		return Integer.valueOf(firstRow.get(firstColumn));
	}

	/*
	private int getNumSelectivitySamples(String predicate) throws IOException
	{
		String query = "SELECT COUNT(?sample) FROM %u% WHERE { %u% %u% ?sample }";
		query = SPARQLStringUtils.strFromTemplate(query, getGraphName(), predicate, PredicateStats.PREDICATE_SELECTIVITYSAMPLE);
		List<Map<String, String>> results = selectQuery(query);
		if (results.size() == 0)
			throw new RuntimeException();
		String resultVar = results.get(0).keySet().iterator().next();
		return Integer.valueOf(results.get(0).get(resultVar));
	}

	private int getNumTimeSamples(String predicate) throws IOException
	{
		String query = "SELECT COUNT(?sample) FROM %u% WHERE { %u% %u% ?sample }";
		query = SPARQLStringUtils.strFromTemplate(query, getGraphName(), predicate, PredicateStats.PREDICATE_TIMESAMPLE);
		List<Map<String, String>> results = selectQuery(query);
		if (results.size() == 0)
			throw new RuntimeException();
		String resultVar = results.get(0).keySet().iterator().next();
		return Integer.valueOf(results.get(0).get(resultVar));
	}
	*/
	
	/*
	public void sampleForwardTime(String predicate, int numSamples, long upperSampleLimit, SPARQLEndpoint endpoint) throws HttpException, HttpResponseCodeException, IOException
	{
		log.trace("Sampling forward time of " + predicate + " in " + endpoint.getURI());
		for (int i = 0; i < numSamples && i <= upperSampleLimit; i++) {
			String s = getSubjectSample(upperSampleLimit, predicate, endpoint);
			String query = "SELECT * WHERE { %u% %u% ?o }";
			query = SPARQLStringUtils.strFromTemplate(query, s, predicate);
			long startTime = getTime();
			queryClient.synchronousQuery(query, SADI_QUERY_TIMEOUT);
			// in milliseconds
			int forwardTime = (int) (getTime() - startTime);
			log.debug("Stats for endpoint " + endpoint.getURI() + ", predicate " + predicate + ": Sample #" + i + ", subject " + s + " has forward time " + String.valueOf(forwardTime));
			recordTimeSample(predicate, forwardTime, true);
		}
	}

	public void sampleReverseTime(String predicate, int numSamples, long upperSampleLimit, SPARQLEndpoint endpoint) throws IOException, AmbiguousPropertyTypeException
	{
		log.trace("Sampling reverse time of " + predicate + " in " + endpoint.getURI());
		for (int i = 0; i < numSamples && i <= upperSampleLimit; i++) {
			String o = getObjectSample(upperSampleLimit, predicate, endpoint);
			String query;
			if (!isDatatypeProperty(predicate, endpoint))
				query = "SELECT * WHERE { ?s %u% %u% }";
			else {
				if (NumberUtils.isNumber(o))
					query = "SELECT * WHERE { ?s %u% %v% }";
				else
					query = "SELECT * WHERE { ?s %u% %s% }";
			}
			query = SPARQLStringUtils.strFromTemplate(query, predicate, o);
			long startTime = getTime();
			// queryClient.synchronousQuery(timeQuery);
			queryClient.synchronousQuery(query, SADI_QUERY_TIMEOUT);
			// in milliseconds
			int reverseTime = (int) (getTime() - startTime);
			log.debug("Stats for endpoint " + endpoint.getURI() + ", predicate " + predicate + ": Sample #" + i + ", object " + o + " has reverse time " + String.valueOf(reverseTime));
			recordTimeSample(predicate, reverseTime, false);
		}
	}
	*/
	
	public void recordTimeSample(String predicate, int timeSample, boolean directionIsForward) throws IOException
	{
		String direction = directionIsForward ? "forward" : "reverse";
		log.trace("recording " + direction + " query execution time " + timeSample + " for " + predicate);

		long time = getTime();

		String query = 
			"INSERT INTO GRAPH %u% { " + 
			"    %u% %u% %u% . " + 
			"    %u% %u% %s% . " + 
			"    %u% %u% %v% . " + 
			"    %u% %u% %v% . " + 
			"}";

		String sampleNodeURI = predicate + String.valueOf(time);

		query = SPARQLStringUtils.strFromTemplate(query, 
				getGraphName(), 
				predicate, PredicateStats.PREDICATE_TIMESAMPLE, sampleNodeURI, 
				sampleNodeURI, PredicateStats.PREDICATE_DIRECTION_IS_FORWARD, String.valueOf(directionIsForward), 
				sampleNodeURI, PredicateStats.PREDICATE_TIMESTAMP, String.valueOf(time), 
				sampleNodeURI, PredicateStats.PREDICATE_TIME, String.valueOf(timeSample));

		updateQuery(query);
	}

	/*
	public void sampleForwardSelectivity(String predicate, int numSamples, long upperSampleLimit, SPARQLEndpoint endpoint) throws IOException
	{
		log.trace("Sampling forward selectivity of " + predicate + " in " + endpoint.getURI());
		for (int i = 0; i < numSamples && i <= upperSampleLimit; i++) {
			String s = getSubjectSample(upperSampleLimit, predicate, endpoint);
			long forwardSelectivity = getPredicateCountForSubject(endpoint, s, predicate);

			log.debug("Stats for endpoint " + endpoint.getURI() + ", predicate " + predicate + ": Sample #" + i + ", subject " + s + " has forward selectivity " + String.valueOf(forwardSelectivity));
			recordSelectivitySample(predicate, forwardSelectivity, true);
		}
	}

	public void sampleReverseSelectivity(String predicate, int numSamples, long upperSampleLimit, SPARQLEndpoint endpoint) throws IOException, AmbiguousPropertyTypeException
	{
		log.trace("Sampling reverse selectivity of " + predicate + " in " + endpoint.getURI());
		for (int i = 0; i < numSamples && i <= upperSampleLimit; i++) {
			String o = getObjectSample(upperSampleLimit, predicate, endpoint);
			long reverseSelectivity = getPredicateCountForObject(endpoint, o, predicate);

			// (reverseSelectivity == 0) when the registry has
			// incorrectly identified a predicate as
			// an object property, whereas it is really a datatype
			// property that usually takes
			// strings which are URIs. (As far as I am aware, there
			// is no way for the registry to
			// automatically discern between these two cases.)
			// 
			// If this mistake is made by the registry, then we will
			// get no results back
			// from getPredicateCountForObject above. Give up and
			// carry on to the next predicate.

			if (reverseSelectivity == 0) {
				log.warn("The predicate " + predicate + " has been incorrectly typed by the registry as an object property.");
				return;
			}

			log.debug("Stats for endpoint " + endpoint.getURI() + ", predicate " + predicate + ": Sample #" + i + ", object " + o + " has reverse selectivity " + String.valueOf(reverseSelectivity));
			recordSelectivitySample(predicate, reverseSelectivity, false);
		}
	}
	*/
	
	public void recordSelectivitySample(String predicate, long selectivitySample, boolean directionIsForward) throws IOException
	{
		String direction = directionIsForward ? "forward" : "reverse";
		log.trace("recording " + direction + " selectivity " + selectivitySample + " for " + predicate);

		long time = getTime();

		String query = 
				"INSERT INTO GRAPH %u% { " + 
				"    %u% %u% %u% . " + 
				"    %u% %u% %s% . " + 
				"    %u% %u% %v% . " + 
				"    %u% %u% %v% . " + 
				"}";

		String sampleNodeURI = predicate + String.valueOf(time);

		query = SPARQLStringUtils.strFromTemplate(query, 
				getGraphName(), 
				predicate, PredicateStats.PREDICATE_SELECTIVITYSAMPLE,  sampleNodeURI, 
				sampleNodeURI, PredicateStats.PREDICATE_DIRECTION_IS_FORWARD, String.valueOf(directionIsForward), 
				sampleNodeURI, PredicateStats.PREDICATE_TIMESTAMP, String.valueOf(time), 
				sampleNodeURI, PredicateStats.PREDICATE_SELECTIVITY, String.valueOf(selectivitySample));
		
		updateQuery(query);
	}

	/**
	 * Get the number of triples which contain predicateURI in the given
	 * endpoint. This count excludes any triples with blank nodes in either
	 * the subject or object position.
	 * 
	 * @param endpoint
	 *                the endpoint to be queried
	 * @param predicateURI
	 *                the predicate to be queried
	 * @param slowEndpoint
	 *                if true, this indicates that the method should not
	 *                attempt to run query to determine the exact number of
	 *                triples, but should instead return a lower bound on
	 *                the number of matching triples
	 * @return number of triples containing predicateURI
	 * @throws IOException
	 */
	
	/*
	public long getNumTriplesForPredicate(SPARQLEndpoint endpoint, String predicateURI, boolean slowEndpoint) throws IOException
	{
		log.trace("Getting number of triples containing predicate " + predicateURI + " in " + endpoint.getURI());
		String query = "SELECT COUNT(?s) WHERE { ?s %u% ?o . FILTER (!isBlank(?s) && !isBlank(?o)) }";
		query = SPARQLStringUtils.strFromTemplate(query, predicateURI);
		List<Map<String, String>> results = null;
		if (!slowEndpoint) {
			try {
				results = endpoint.selectQuery(query, ENDPOINT_QUERY_TIMEOUT);
				Map<String, String> binding = results.iterator().next();
				return Long.parseLong(binding.get(binding.keySet().iterator().next()));
			}
			catch (IOException e) {
				log.trace("Failed to COUNT number of triples containing " + predicateURI + ". Trying for a lower bound instead.");
			}
		}
		String lowerBoundQuery = "SELECT ?s WHERE { ?s %u% ?o . FILTER (!isBlank(?s) && !isBlank(?o)) }";
		lowerBoundQuery = SPARQLStringUtils.strFromTemplate(lowerBoundQuery, predicateURI);
		return endpoint.getResultsCountLowerBound(lowerBoundQuery, 50000);
	}
	*/
	
	/**
	 * Return the number of (non distinct) subject URIs for the given
	 * predicate, within the given endpoint.
	 * 
	 * @param endpoint
	 * @param predicateURI
	 * @param slowEndpoint
	 *                if true, it indicates that the method should not
	 *                attempt to query for an exact number of subjects
	 *                (because the query will likely timeout). Instead, try
	 *                to get a lower bound on the number of subjects
	 *                instead.
	 * @return the number of (non distinct) subject URIs
	 */
	/*
	public long getNumSubjectsForPredicate(SPARQLEndpoint endpoint, String predicateURI, boolean slowEndpoint) throws IOException
	{
		log.trace("Querying for number of (non-distinct) subject URIs for " + predicateURI + " in " + endpoint.getURI());
		if (!slowEndpoint) {
			String query = "SELECT COUNT(?s) WHERE { ?s %u% ?o . FILTER isURI(?s) }";
			query = SPARQLStringUtils.strFromTemplate(query, predicateURI);
			try {
				List<Map<String, String>> results = endpoint.selectQuery(query, ENDPOINT_QUERY_TIMEOUT);
				Map<String, String> binding = results.iterator().next();
				return Long.parseLong(binding.get(binding.keySet().iterator().next()));
			}
			catch (IOException e) {
				log.trace("Failed to COUNT number of triples containing " + predicateURI + ". Trying for a lower bound instead.");
			}
		}
		String lowerBoundQuery = "SELECT ?s WHERE { ?s %u% ?o . FILTER !isURI(?s) }";
		lowerBoundQuery = SPARQLStringUtils.strFromTemplate(lowerBoundQuery, predicateURI);
		return endpoint.getResultsCountLowerBound(lowerBoundQuery, 50000);
	}
	*/
	
	/**
	 * Return the number of (non distinct) object values for the given
	 * predicate, within the given endpoint. The count excludes object
	 * values that are blank nodes.
	 * 
	 * @param endpoint
	 * @param predicateURI
	 * @param slowEndpoint
	 *                if true, it indicates that the method should not
	 *                attempt to query for an exact number of subjects
	 *                (because the query will likely timeout). Instead, try
	 *                to get a lower bound on the number of subjects
	 *                instead.
	 * @return the number of (non distinct) subject URIs
	 */
	/*
	public long getNumObjectsForPredicate(SPARQLEndpoint endpoint, String predicateURI, boolean slowEndpoint) throws IOException
	{
		log.trace("Querying for number of (non-distinct) object values for " + predicateURI + " in " + endpoint.getURI());
		if (!slowEndpoint) {
			String query = "SELECT COUNT(?s) WHERE { ?s %u% ?o . FILTER (!isBlank(?o)) }";
			query = SPARQLStringUtils.strFromTemplate(query, predicateURI);
			try {
				List<Map<String, String>> results = endpoint.selectQuery(query, ENDPOINT_QUERY_TIMEOUT);
				Map<String, String> binding = results.iterator().next();
				return Long.parseLong(binding.get(binding.keySet().iterator().next()));
			}
			catch (IOException e) {
				log.trace("Failed to COUNT number of triples containing " + predicateURI + ". Trying for a lower bound instead.");
			}
		}
		String lowerBoundQuery = "SELECT ?s WHERE { ?s %u% ?o . FILTER (!isBlank(?o)) }";
		lowerBoundQuery = SPARQLStringUtils.strFromTemplate(lowerBoundQuery, predicateURI);
		return endpoint.getResultsCountLowerBound(lowerBoundQuery, 50000);
	}
	*/
	
	/*
	public long getPredicateCountForSubject(SPARQLEndpoint endpoint, String subjectURI, String predicateURI) throws IOException
	{
		String queryPredCount = "SELECT COUNT(?o) WHERE { %u% %u% ?o }";
		queryPredCount = SPARQLStringUtils.strFromTemplate(queryPredCount, subjectURI, predicateURI);
		List<Map<String, String>> results;
		try {
			results = endpoint.selectQuery(queryPredCount, ENDPOINT_QUERY_TIMEOUT);
			if (results.size() == 0)
				throw new RuntimeException();
			Map<String, String> binding = results.iterator().next();
			return Integer.parseInt(binding.get(binding.keySet().iterator().next()));
		}
		catch (HttpResponseCodeException e) {
			if (!HttpUtils.isHTTPTimeout(e))
				throw e;
		}
		catch (IOException e) {
			if (!HttpUtils.isHTTPTimeout(e))
				throw e;
		}

		// If we fail to get a proper predicate count, try to get a
		// lower bound instead.
		String queryLowerBound = "SELECT ?o WHERE { %u% %u% ?o }";
		queryLowerBound = SPARQLStringUtils.strFromTemplate(queryLowerBound, subjectURI, predicateURI);
		long lowerBound = endpoint.getResultsCountLowerBound(queryLowerBound, 50000);
		return lowerBound;
	}


	public long getPredicateCountForObject(SPARQLEndpoint endpoint, String object, String predicateURI) throws IOException, AmbiguousPropertyTypeException
	{
		String queryPredCount;
		String queryLowerBound;
		if (!isDatatypeProperty(predicateURI, endpoint)) {
			queryPredCount = "SELECT COUNT(?s) WHERE { ?s %u% %u% }";
			queryLowerBound = "SELECT ?s WHERE { ?s %u% %u% }";
		}
		else {
			if (NumberUtils.isNumber(object)) {
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

		List<Map<String, String>> results;
		try {
			results = endpoint.selectQuery(queryPredCount, ENDPOINT_QUERY_TIMEOUT);
			if (results.size() == 0)
				throw new RuntimeException();
			Map<String, String> binding = results.iterator().next();
			return Integer.parseInt(binding.get(binding.keySet().iterator().next()));
		}
		catch (HttpResponseCodeException e) {
			if (!HttpUtils.isHTTPTimeout(e))
				throw e;
		}
		catch (IOException e) {
			if (!HttpUtils.isHTTPTimeout(e))
				throw e;
		}

		// If we fail to get a proper predicate count, try to get a
		// lower bound instead.
		long lowerBound = endpoint.getResultsCountLowerBound(queryLowerBound, 50000);
		return lowerBound;
	}
	*/
	
	private long getTime()
	{
		return new Date().getTime();
	}

	public void clearStats() throws IOException
	{
		String query = SPARQLStringUtils.strFromTemplate("CLEAR GRAPH %u%", getGraphName());
		updateQuery(query);
	}

	private boolean isDatatypeProperty(String property, SPARQLEndpoint endpoint) throws IOException, AmbiguousPropertyTypeException
	{
		if (isDatatypePropertyCache.containsKey(property))
			return isDatatypePropertyCache.get(property);

		boolean isDatatypeProperty = endpoint.isDatatypeProperty(property, true);
		isDatatypePropertyCache.put(property, Boolean.valueOf(isDatatypeProperty));

		return isDatatypeProperty;
	}

	public void updateAverageStats() throws IOException
	{
		// avg reverse time
		updateAverageStat(false, false);
		// avg forward time
		updateAverageStat(false, true);
		// avg reverse selectivity
		updateAverageStat(true, false);
		// avg forward selectivity
		updateAverageStat(true, true);
	}

	private void updateAverageStat(boolean statIsSelectivity, boolean directionIsForward) throws IOException
	{
		String stat = statIsSelectivity ? "selectivity" : "delay time";
		String direction = directionIsForward ? "forward" : "reverse";

		log.trace("updating average " + direction + " " + stat);
		
		String statPredicate;
		if (statIsSelectivity) {
			if (directionIsForward)
				statPredicate = PredicateStats.PREDICATE_AVG_FORWARD_SELECTIVITY;
			else
				statPredicate = PredicateStats.PREDICATE_AVG_REVERSE_SELECTIVITY;
		}
		else {
			if (directionIsForward)
				statPredicate = PredicateStats.PREDICATE_AVG_FORWARD_TIME;
			else
				statPredicate = PredicateStats.PREDICATE_AVG_REVERSE_TIME;
		}

		// Calculate new value
		int newValue = calcAverageStat(statIsSelectivity, directionIsForward);

		// Erase old value
		String deleteQuery = "DELETE FROM GRAPH %u% { %u% %u% ?o } WHERE { %u% %u% ?o }";
		deleteQuery = SPARQLStringUtils.strFromTemplate(deleteQuery, getGraphName(), getGraphName(), statPredicate, getGraphName(), statPredicate);
		updateQuery(deleteQuery);

		// Write new value
		String insertQuery = "INSERT INTO GRAPH %u% { %u% %u% %v% }";
		insertQuery = SPARQLStringUtils.strFromTemplate(insertQuery, getGraphName(), getGraphName(), statPredicate, String.valueOf(newValue));
		updateQuery(insertQuery);

	}

	private int calcAverageStat(boolean statIsSelectivity, boolean directionIsForward) throws IOException
	{
		String stat = statIsSelectivity ? "selectivity" : "delay time";
		String direction = directionIsForward ? "forward" : "reverse";
		
		String query = 
			"SELECT AVG(?stat) FROM %u%\n" + 
			"WHERE {\n" + 
			"   ?sample %u% %s% .\n" + 
			"   ?sample %u% ?stat .\n " +
			"   FILTER (?stat != %v%) \n" +
			"}";

		String statPredicate;
		if (statIsSelectivity)
			statPredicate = PredicateStats.PREDICATE_SELECTIVITY;
		else
			statPredicate = PredicateStats.PREDICATE_TIME;

		query = SPARQLStringUtils.strFromTemplate(query, 
					getGraphName(), 
					PredicateStats.PREDICATE_DIRECTION_IS_FORWARD, String.valueOf(directionIsForward), 
					statPredicate,
					String.valueOf(PredicateStats.INFINITY));

		List<Map<String, String>> results = selectQuery(query);

		if (results.size() == 0)
			throw new RuntimeException();

		/*
		 * NOTE: If the aggregate can't be computed (e.g. trying to take
		 * the AVG() of a list of URIs) or there are no matching entries
		 * in the database, Virtuoso returns a result set of size 1 with
		 * no bindings.
		 * 
		 * I don't know if this is standard behaviour across different
		 * types of SPARQL endpoints. -- BV
		 */
		if (!results.get(0).keySet().iterator().hasNext())
			throw new RuntimeException("error computing average for " + direction + " " + stat);

		String columnHeader = results.get(0).keySet().iterator().next();

		return Integer.valueOf(results.get(0).get(columnHeader));
	}

	public void removeStatsForPredicate(String predicate) throws IOException
	{
		deleteDirectedClosure(predicate, getGraphName());
	}

	public void removeStatsForPredicate(String predicate, Date stalenessDate) throws IOException
	{
		long timestamp = stalenessDate.getTime();

		String query =
				"DELETE FROM GRAPH %u% {\n" +
				"      %u% ?p ?sample .\n" +
				"      ?sample ?p2 ?o2 .\n" +
				"}\n" +
				"FROM %u%  WHERE {\n" +
				"      %u% ?p ?sample .\n" +
				"      ?sample %u% ?timestamp .\n" +
				"      ?sample ?p2 ?o2 .\n" +
				"      FILTER (?timestamp <= %v%)\n" +
				"}";

		query = SPARQLStringUtils.strFromTemplate(query,
				getGraphName(),
				predicate,
				getGraphName(),
				predicate,
				PredicateStats.PREDICATE_TIMESTAMP,
				String.valueOf(timestamp));
		
		updateQuery(query);
	}

	private static class InputSampler
	{

		protected SPARQLRegistry registry;
		protected static final int MAX_ATTEMPTS = 3;
		protected static final int QUERY_TIMEOUT = 30 * 1000; // in

		// milliseconds

		public InputSampler(SPARQLRegistry registry)
		{
			this.registry = registry;
		}

		public Node sampleSubject(String predicate) throws IOException, NoSampleAvailableException, ExceededMaxAttemptsException
		{
			return getSample(predicate, true);
		}

		public Node sampleObject(String predicate) throws IOException, NoSampleAvailableException, ExceededMaxAttemptsException
		{
			return getSample(predicate, false);
		}

		public Node getSample(String predicate, boolean positionIsSubject) throws IOException, NoSampleAvailableException, ExceededMaxAttemptsException
		{
			String desc = positionIsSubject ? "subject URI" : "object value";

			/* Choose an endpoint, and determine the number of candidates within that endpoint (upperSampleLimit) */
			List<SPARQLEndpoint> endpoints = new ArrayList<SPARQLEndpoint>(registry.findEndpointsByPredicate(predicate));

			RandomDataImpl generator = new RandomDataImpl();
			long upperSampleLimit = 0;
			int attempts = 0;
			SPARQLEndpoint endpoint = null;
			Node sample = null;
			
			while (attempts < MAX_ATTEMPTS) {

				if (endpoints.size() == 0)
					throw new NoSampleAvailableException("there are no non-blank-node " + desc + "s for " + predicate + " in the data");

				int endpointIndex = endpoints.size() > 1 ? generator.nextInt(0, endpoints.size() - 1) : 0;
				endpoint = endpoints.get(endpointIndex);
				
				try {
					upperSampleLimit = getUpperSampleLimit(endpoint, predicate, positionIsSubject);
					if(upperSampleLimit > 0) {
						try {
							long sampleIndex = upperSampleLimit > 1 ? generator.nextLong(0, upperSampleLimit - 1) : 0;
							return getSample(endpoint, predicate, sampleIndex, positionIsSubject);
						}
						catch(IOException e2) {
							log.warn("failed to retrieve sample " + desc, e2);
						}
						catch(NoSampleAvailableException e2) {
							log.warn("failed to retrieve sample " + desc, e2);
						}
					}
				}
				catch(IOException e) {
					log.warn("failed to determine upper sample limit", e);
				}
				
				endpoints.remove(endpointIndex);
				attempts++;
			}

			throw new ExceededMaxAttemptsException("exceeded " + MAX_ATTEMPTS + " attempts when trying to retrieve a non-blank-node " + desc + " for " + predicate);
		}

		protected Node getSample(SPARQLEndpoint endpoint, String predicate, long sampleIndex, boolean positionIsSubject) throws IOException, NoSampleAvailableException
		{
			String filter = positionIsSubject ? "FILTER (!isBlank(?s))" : "FILTER (!isBlank(?o))";
			String desc = positionIsSubject ? "subject URI" : "object URI/literal";

			log.trace("retrieving " + desc + " #" + sampleIndex + " from " + endpoint.getURI());

			String query = "CONSTRUCT { ?s %u% ?o } WHERE { ?s %u% ?o . " + filter + " } OFFSET %v% LIMIT 1";
			query = SPARQLStringUtils.strFromTemplate(query, predicate, predicate, String.valueOf(sampleIndex));
			Collection<Triple> triples = endpoint.constructQuery(query);
			
			if (triples.size() == 0)
				throw new RuntimeException("no " + desc + " #" + sampleIndex + " exists in " + endpoint.getURI());

			Triple triple = triples.iterator().next();
			Node sample = positionIsSubject ? triple.getSubject() : triple.getObject();

			// Sanity check. If the index is out of date with the data, the sample may not satisfy
			// the regular expressions for the subject/object URIs. (In which case downstream queries
			// will return no results.) The simplest thing to do in that case is just to fail and take another 
			// sample.
			if(sample.isURI()) {
				String endpointURI = endpoint.getURI();
				String uri = sample.toString();

				boolean matches = positionIsSubject ? registry.subjectMatchesRegEx(endpointURI, uri) : registry.objectMatchesRegEx(endpointURI, uri);
				if(!matches) {
					String pos = positionIsSubject ? "subject" : "object";
					throw new NoSampleAvailableException("sample " + pos + " uri " + uri + 
							" does not match the regular expression for " + endpointURI + " (from which it was sampled)");
				}
			}
			return sample;
		}
		

		protected long getUpperSampleLimit(SPARQLEndpoint endpoint, String predicate, boolean positionIsSubject) throws IOException
		{
			String filter = positionIsSubject ? "FILTER (!isBlank(?s))" : "FILTER (!isBlank(?o))";
			String desc = positionIsSubject ? "subject URIs" : "object URIs/literals";

			String uri = endpoint.getURI();
			ServiceStatus status = registry.getServiceStatus(uri);

			if (status == ServiceStatus.DEAD)
				throw new IllegalArgumentException("status of " + uri + " is DEAD");

			log.trace("determining number of triples with " + desc + " in " + uri);
			
			if (status != ServiceStatus.SLOW) {
				try {
					String query = "SELECT COUNT(*) WHERE { ?s %u% ?o . " + filter + " }";
					query = SPARQLStringUtils.strFromTemplate(query, predicate);
					List<Map<String, String>> results = endpoint.selectQuery(query, QUERY_TIMEOUT);

					Map<String, String> firstRow = results.iterator().next();
					String firstColumn = firstRow.keySet().iterator().next();

					return Long.parseLong(firstRow.get(firstColumn));
				}
				catch (IOException e) {
					log.warn("failed to COUNT number of " + desc + " for " + predicate + " in " + uri + ", trying for a lower bound instead.");
				}
			}
			String lowerBoundQuery = "SELECT * WHERE { ?s %u% ?o . " + filter + " }";
			lowerBoundQuery = SPARQLStringUtils.strFromTemplate(lowerBoundQuery, predicate);
			return endpoint.getResultsCountLowerBound(lowerBoundQuery, 50000);
		}
	}
	
	private static class CommandLineOptions
	{
		public enum OperationType {
			UPDATE_STATS_FOR_PREDICATE, 
			REMOVE_STATS_FOR_PREDICATE, 
			UPDATE_STATS_FOR_ENDPOINT, 
			UPDATE_STATS_FOR_ALL_PREDICATES,
			UPDATE_AVERAGES,
		};

		public static class Operation
		{
			String arg;
			OperationType opType;

			public Operation(String arg, OperationType opType)
			{
				this.arg = arg;
				this.opType = opType;
			}
		};

		public List<Operation> operations = new ArrayList<Operation>();

		@Option(name = "-p", usage = "Gather and record stats for the given predicate")
		public void updateStatsForPredicate(String predicate) { 	operations.add(new Operation(predicate, OperationType.UPDATE_STATS_FOR_PREDICATE)); }
		
		@Option(name = "-r", usage = "Remove all stats for the given predicate")
		public void removeStatsForPredicate(String predicate) { operations.add(new Operation(predicate, OperationType.REMOVE_STATS_FOR_PREDICATE)); }
		
		@Option(name = "-e", usage = "Compute statistics for all predicates used in a given SPARQL endpoint (argument is a URI)")
		public void updateStatsForEndpoint(String endpointURI) { operations.add(new Operation(endpointURI, OperationType.UPDATE_STATS_FOR_ENDPOINT)); }

		@Option(name = "-a", usage = "Compute statistics for all known predicates")
		public void updateStatsForAllPredicates(boolean unused) { operations.add(new Operation(null, OperationType.UPDATE_STATS_FOR_ALL_PREDICATES)); }

		@Option(name = "-A", usage = "Compute averages for each type of stat (e.g. forward selectivity)")
		public void updateAverages(boolean unused) { operations.add(new Operation(null, OperationType.UPDATE_AVERAGES)); }

		@Option(name = "-n", usage = "Number of samples to take of each type (e.g. forward selectivity), for each predicate")
		int samplesPerPredicate = 3;
		
		@Option(name = "-R", usage = "For predicate being sampled, erase any existing stats. (Existing stats are kept by default.)")
		boolean eraseExistingStats = false;

		@Option(name = "-d", usage = "Staleness date (e.g. '6/30/09' for June 30, 2009). All samples recorded before or on this date will be replaced.  (By default, all samples are kept.)")
		public void setStalenessDate(String dateString) throws ParseException
		{
			DateFormat formatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
			stalenessDate = (Date) formatter.parse(dateString);
		}

		public Date stalenessDate = new Date(0); // default to time zero (Jan 1, 1970)

	}

	public static void main(String[] args) throws IOException
	{

		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser cmdLineParser = new CmdLineParser(options);

		try {
			cmdLineParser.parseArgument(args);
			PredicateStatsDBAdmin statsDB = new PredicateStatsDBAdmin();

			if (options.eraseExistingStats)
				options.stalenessDate = new Date(); // stalenessDate = NOW

			if (options.operations.size() == 0)
				throw new CmdLineException("No action specified.  To compute statistics for all known predicates, use the -a switch.");

			for (CommandLineOptions.Operation op : options.operations) {

				try {
					switch (op.opType) {
					case UPDATE_STATS_FOR_PREDICATE:
						statsDB.computeStatsForPredicate(op.arg, options.samplesPerPredicate, options.stalenessDate);
						break;
					case UPDATE_STATS_FOR_ENDPOINT:
						statsDB.computeStatsForEndpoint(op.arg, options.samplesPerPredicate, options.stalenessDate);
						break;
					case REMOVE_STATS_FOR_PREDICATE:
						statsDB.removeStatsForPredicate(op.arg);
						break;
					case UPDATE_STATS_FOR_ALL_PREDICATES:
						statsDB.computeStatsForAllPredicates(options.samplesPerPredicate, options.stalenessDate);
						break;
					case UPDATE_AVERAGES:
						statsDB.updateAverageStats();
						break;
					default:
						throw new RuntimeException("Unknown operation requested");
					}
				}
				catch (Exception e) {
					log.error("operation " + op.opType + " failed on " + op.arg, e);
				}

			}

		}
		catch (CmdLineException e) {
			log.error(e.getMessage());
			log.error("Usage: statsdb [-p predicate] [-s numSelectivitySamples] [-t numTimeSamples]\n");
			cmdLineParser.printUsage(System.err);
		}
	}

}
