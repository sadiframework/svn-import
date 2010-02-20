package ca.wilkinsonlab.sadi.optimizer.statistics;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.ibm.icu.text.DateFormat;

import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.vocab.PredicateStats;
import ca.wilkinsonlab.sadi.client.QueryClient;
import ca.wilkinsonlab.sadi.client.Registry;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLRegistry;
import ca.wilkinsonlab.sadi.client.virtual.sparql.VirtuosoSPARQLEndpoint;
import ca.wilkinsonlab.sadi.common.SADIException;
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

	protected final static Configuration config = Config.getConfiguration().subset(CONFIG_ROOT);
	private QueryClient queryClient = new SHAREQueryClient();

	protected final static String SPARQL_RESULTS_LIMIT_CONFIG_KEY = "sadi.sparql.resultsLimit";
	private static final int SPARQL_RESULTS_LIMIT = 500;
	
	protected String graphName;
	protected SPARQLRegistry registry;
	protected InputSampler sampler;
	
	public PredicateStatsDBAdmin(String endpointURI, String graphURI, String username, String password) throws HttpException, IOException
	{
		super(endpointURI, username, password);
		setGraphName(graphURI);
		
		if (getSPARQLRegistry() != null) {
			setRegistry(getSPARQLRegistry());
		} else {
			throw new RuntimeException("cannot access SPARQL endpoint registry");
		}
		
		setInputSampler(new InputSampler(getRegistry()));
	}
	
	/** 
	 * Return the SADI SPARQL registry.  For now, we assume that there is exactly
	 * one such registry. 
	 * 
	 * @return the SADI SPARQL registry
	 */
	public static SPARQLRegistry getSPARQLRegistry() 
	{
		for(Registry r: ca.wilkinsonlab.sadi.client.Config.getConfiguration().getRegistries()) {
			if(r instanceof SPARQLRegistry)
				return (SPARQLRegistry)r;
		}
		return null;
	}
	
	public String getGraphName() { 
		return graphName; 
	}
	
	public void setGraphName(String graphName) { 
		this.graphName = graphName; 
	}

	public SPARQLRegistry getRegistry() { 
		return registry; 
	}
	
	public void setRegistry(SPARQLRegistry registry) { 	
		this.registry = registry; 
	}
	
	public InputSampler getInputSampler() {
		return sampler;
	}

	protected void setInputSampler(InputSampler sampler) {
		this.sampler = sampler;
	}

	public void computeStatsForAllPredicates(int samplesPerPredicate, Date stalenessDate) throws SADIException, IOException
	{
		for(String predicate : getRegistry().getAllPredicates()) {
			computeStatsForPredicate(predicate, samplesPerPredicate, stalenessDate);
		}
		updateAverageStats();
	}

	public void computeStatsForEndpoint(String endpointURI, int numSamples, Date stalenessDate) throws SADIException, IOException
	{
		Collection<String> predicates = getRegistry().getPredicatesForEndpoint(endpointURI);
		for (String predicate : predicates)
			computeStatsForPredicate(predicate, numSamples, stalenessDate);
		updateAverageStats();
	}

	public void computeStatsForPredicate(String predicate, int numSamples, Date stalenessDate) throws SADIException, IOException
	{
		log.trace("Sampling stats for predicate " + predicate);

		// remove stale stats
		removeStatsForPredicate(predicate, stalenessDate);

		// for each type of sample (e.g. forward selectivity), determine how many more samples we need to obtain
		int numForwardSelectivitySamples = Math.max(0, numSamples - getNumSamples(predicate, true, true));
		int numForwardTimeSamples = Math.max(0, numSamples - getNumSamples(predicate, false, true));
		int numReverseSelectivitySamples = Math.max(0, numSamples - getNumSamples(predicate, true, false));
		int numReverseTimeSamples = Math.max(0, numSamples - getNumSamples(predicate, false, false));

		computeStatsForPredicate(predicate, numForwardSelectivitySamples, numForwardTimeSamples, true);
		computeStatsForPredicate(predicate, numReverseSelectivitySamples, numReverseTimeSamples, false);
	}
	
	protected void computeStatsForPredicate(String predicate, int numSelectivitySamples, int numTimeSamples, boolean directionIsForward) throws SADIException
	{
		int numQueries = Math.max(numSelectivitySamples, numTimeSamples);
		List<Map<String,String>> results;
		String query;
		long startTime, endTime;
		int selectivity, time;
		
		String direction = directionIsForward ? "forward" : "reverse";
		for(int i = 0; i < numQueries; i++) {

			Node input = null;
			try {
				input = directionIsForward ? getInputSampler().sampleSubject(predicate) : getInputSampler().sampleObject(predicate);
				query = getQuery(input, predicate, directionIsForward);
				
				startTime = getTime();
				results = SADIQuery(query);
				endTime = getTime();

				if(results.size() == 0) {
					log.warn("zero query results, skipping recording of stats for " + predicate);
					continue;
				}

				if(i < numSelectivitySamples) {
					if(results.size() >= SPARQL_RESULTS_LIMIT) {
						selectivity = PredicateStats.INFINITY;
					} else {
						selectivity = results.size();
					}
					recordSelectivitySample(predicate, selectivity, directionIsForward);
				}
				
				if(i < numTimeSamples) {
					if(results.size() >= SPARQL_RESULTS_LIMIT) {
						time = PredicateStats.INFINITY;
					} else {
						time = (int)(endTime - startTime);
					}
					recordTimeSample(predicate, time, directionIsForward);
				}
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
		}
	}
	
	protected String getQuery(Node input, String predicate, boolean inputIsSubject) {
		
		if (input.isBlank())
			throw new IllegalArgumentException("cannot gather stats for a blank node");

		Node var = NodeCreateUtils.create("?var");
		
		Triple pattern = null;
		Node p = NodeCreateUtils.create(predicate);
		
		if(inputIsSubject)
			pattern = new Triple(input, p, var);
		else
			pattern = new Triple(var, p, input);
		
		//return SPARQLStringUtils.getConstructQuery(Collections.singletonList(pattern), Collections.singletonList(pattern));
		//return "SELECT * WHERE { " + SPARQLStringUtils.getTriplePattern(pattern) + " }";
		return getSelectStarQuery(pattern);
	}
	

	protected String getSelectStarQuery(Triple triplePattern) 
	{
		Query query = new Query();
		query.setQuerySelectType();

		ElementTriplesBlock queryPattern = new ElementTriplesBlock();
		queryPattern.addTriple(triplePattern);
		query.setQueryPattern(queryPattern);		

		// Indicates a "*" in the SELECT clause.
		query.setQueryResultStar(true);

		return query.serialize();
	}
	
	private List<Map<String,String>> SADIQuery(String query) throws IOException 
	{
		log.trace("running query: " + query);

		ca.wilkinsonlab.sadi.client.Config config = ca.wilkinsonlab.sadi.client.Config.getConfiguration();

		// Temporarily limit the number of results for queries on individual endpoints
		boolean limitSet = config.containsKey(SPARQL_RESULTS_LIMIT_CONFIG_KEY);
		long origLimit = limitSet ? config.getLong(SPARQL_RESULTS_LIMIT_CONFIG_KEY) : -1;
		config.setProperty(SPARQL_RESULTS_LIMIT_CONFIG_KEY, SPARQL_RESULTS_LIMIT);
		
		queryClient = new SHAREQueryClient();
		List<Map<String, String>> results = queryClient.synchronousQuery(query);
		
		// Restore the original limit value, if any.
		if(limitSet)
			config.setProperty(SPARQL_RESULTS_LIMIT_CONFIG_KEY, origLimit);
		else
			config.clearProperty(SPARQL_RESULTS_LIMIT_CONFIG_KEY);
			
		return results; 
	}

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
	
	public void recordTimeSample(String predicate, int timeSample, boolean directionIsForward) throws IOException
	{
		String direction = directionIsForward ? "forward" : "reverse";
		String value = (timeSample == PredicateStats.INFINITY) ? "INFINITY" : String.valueOf(timeSample);
		log.trace("recording " + direction + " query execution time " + value + " for " + predicate);

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

	public void recordSelectivitySample(String predicate, long selectivitySample, boolean directionIsForward) throws IOException
	{
		String direction = directionIsForward ? "forward" : "reverse";
		String value = (selectivitySample == PredicateStats.INFINITY) ? "INFINITY" : String.valueOf(selectivitySample);
		log.trace("recording " + direction + " selectivity " + value + " for " + predicate);

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

	private long getTime()
	{
		return new Date().getTime();
	}

	public void clearStats() throws IOException
	{
		String query = SPARQLStringUtils.strFromTemplate("CLEAR GRAPH %u%", getGraphName());
		updateQuery(query);
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
		protected Set<String> deadEndpoints = new HashSet<String>();
		protected UpperSampleLimitCache subjectSampleLimitCache = new UpperSampleLimitCache();
		protected UpperSampleLimitCache objectSampleLimitCache = new UpperSampleLimitCache();
		
		// milliseconds

		public InputSampler(SPARQLRegistry registry) {
			this.registry = registry;
			//HACK: this endpoint generates invalid RDF/XML in response to construct queries, so skip it
			deadEndpoints.add("http://go.bio2rdf.org/sparql");
		}

		public Node sampleSubject(String predicate) throws SADIException, IOException, NoSampleAvailableException, ExceededMaxAttemptsException {
			return getSample(predicate, true);
		}

		public Node sampleObject(String predicate) throws SADIException, IOException, NoSampleAvailableException, ExceededMaxAttemptsException {
			return getSample(predicate, false);
		}

		public Node getSample(String predicate, boolean positionIsSubject) throws SADIException, IOException, NoSampleAvailableException, ExceededMaxAttemptsException
		{
			String desc = positionIsSubject ? "subject URI" : "object value";

			/* Choose an endpoint, and determine the number of candidates within that endpoint (upperSampleLimit) */
			List<SPARQLEndpoint> endpoints = new ArrayList<SPARQLEndpoint>(registry.findEndpointsByPredicate(predicate));
			
			/* Filter out endpoints we already know are dead */
			for(Iterator<SPARQLEndpoint> i = endpoints.iterator(); i.hasNext(); ) {
				SPARQLEndpoint endpoint = i.next();
				if(deadEndpoints.contains(endpoint.getURI())) {
					log.trace("skipping dead endpoint " + endpoint);
					i.remove();
				}
			}
			
			RandomDataImpl generator = new RandomDataImpl();
			long upperSampleLimit = 0;
			int attempts = 0;
			SPARQLEndpoint endpoint = null;
			
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
							deadEndpoints.add(endpoint.getURI());
						}
						catch(NoSampleAvailableException e2) {
							log.warn("failed to retrieve sample " + desc, e2);
						}
					}
				}
				catch(IOException e) {
					log.warn("failed to determine upper sample limit", e);
					deadEndpoints.add(endpoint.getURI());
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

			log.trace("retrieving " + desc + " #" + sampleIndex + " for " + predicate + " from " + endpoint.getURI());

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

		protected long getUpperSampleLimit(SPARQLEndpoint endpoint, String predicate, boolean positionIsSubject) throws SADIException, IOException
		{
			String filter = positionIsSubject ? "FILTER (!isBlank(?s))" : "FILTER (!isBlank(?o))";
			String desc = positionIsSubject ? "subject URIs" : "object URIs/literals";
			
			String uri = endpoint.getURI();
			ServiceStatus status = registry.getServiceStatus(uri);

			if (status == ServiceStatus.DEAD)
				throw new IllegalArgumentException("status of " + uri + " is DEAD");

			log.trace("determining number of triples with " + desc + " in " + uri);

			// check for a cached value first
			UpperSampleLimitCache upperSampleLimitCache = positionIsSubject ? subjectSampleLimitCache : objectSampleLimitCache;
			if(upperSampleLimitCache.contains(uri, predicate)) {
				log.trace("using previously cached value for upper sample limit");
				return upperSampleLimitCache.get(uri, predicate);
			}
			
			if (status != ServiceStatus.SLOW) {
				try {
					String query = "SELECT COUNT(*) WHERE { ?s %u% ?o . " + filter + " }";
					query = SPARQLStringUtils.strFromTemplate(query, predicate);
					List<Map<String, String>> results = endpoint.selectQuery(query);

					Map<String, String> firstRow = results.iterator().next();
					String firstColumn = firstRow.keySet().iterator().next();
					
					long limit = Long.parseLong(firstRow.get(firstColumn));
					upperSampleLimitCache.put(uri, predicate, limit);
					return limit;
				}
				catch (IOException e) {
					log.warn("failed to COUNT number of " + desc + " for " + predicate + " in " + uri + ", trying for a lower bound instead.");
				}
			}
			String lowerBoundQuery = "SELECT * WHERE { ?s %u% ?o . " + filter + " }";
			lowerBoundQuery = SPARQLStringUtils.strFromTemplate(lowerBoundQuery, predicate);
			long limit = endpoint.getResultsCountLowerBound(lowerBoundQuery, 50000); 
			upperSampleLimitCache.put(uri, predicate, limit);
			return limit;
		}
		
		protected static class UpperSampleLimitCache {
			
			protected Map<String,Long> cache = new HashMap<String,Long>();

			public void put(String endpointURI, String predicate, long upperSampleLimit) {
				cache.put(getKey(endpointURI, predicate), upperSampleLimit);
			}
			public Long get(String endpointURI, String predicate) {
				return cache.get(getKey(endpointURI, predicate));
			}
			public boolean contains(String endpointURI, String predicate) {
				return cache.containsKey(getKey(endpointURI, predicate));
			}
			protected String getKey(String endpointURI, String predicate) {
				return endpointURI + ":" + predicate;
			}
		}
	}
	
	@SuppressWarnings("unused")
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

		@Option(name = "-u", usage = "Username (for authenticating with stats DB)")
		String username = config.getString(USERNAME_CONFIG_KEY);
		
		@Option(name = "-p", usage = "Password (for authenticating with stats DB)")
		String password = config.getString(PASSWORD_CONFIG_KEY);
		
		@Option(name = "-r", usage = "Registry URI (e.g. http://localhost:8890/sparql-auth)")
		String registryURI = config.getString(ENDPOINT_CONFIG_KEY);
		
		@Option(name = "-g", usage = "URI of named graph in which to store stats")
		String graphURI = config.getString(GRAPH_CONFIG_KEY);
		
		@Option(name = "-P", usage = "Gather and record stats for the given predicate")
		public void updateStatsForPredicate(String predicate) { 	operations.add(new Operation(predicate, OperationType.UPDATE_STATS_FOR_PREDICATE)); }
		
		@Option(name = "-d", usage = "Remove all stats for the given predicate")
		public void removeStatsForPredicate(String predicate) { operations.add(new Operation(predicate, OperationType.REMOVE_STATS_FOR_PREDICATE)); }
		
		@Option(name = "-e", usage = "Compute stats for all predicates used in a given SPARQL endpoint (argument is a URI)")
		public void updateStatsForEndpoint(String endpointURI) { operations.add(new Operation(endpointURI, OperationType.UPDATE_STATS_FOR_ENDPOINT)); }

		@Option(name = "-a", usage = "Compute stats for all known predicates")
		public void updateStatsForAllPredicates(boolean unused) { operations.add(new Operation(null, OperationType.UPDATE_STATS_FOR_ALL_PREDICATES)); }

		@Option(name = "-A", usage = "Update stored averages for each type of stat (e.g. forward selectivity)")
		public void updateAverages(boolean unused) { operations.add(new Operation(null, OperationType.UPDATE_AVERAGES)); }

		@Option(name = "-n", usage = "Total number of samples to obtain for each predicate, for each sample type (e.g. forward selectivity)." +
				"Samples that are already exist in the database and that are newer than the staleness date (as specified by -d) will" +
				"count towards the total.")
		int samplesPerPredicate = 3;
		
		@Option(name = "-x", usage = "For predicate being sampled, erase any existing stats. (Existing stats are kept by default.)")
		boolean eraseExistingStats = false;

		@Option(name = "-D", usage = "Staleness date (e.g. '6/30/09' for June 30, 2009). All samples recorded before or on this date will be replaced.  (By default, all samples are kept.)")
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
			PredicateStatsDBAdmin statsDB = new PredicateStatsDBAdmin(options.registryURI, options.graphURI, options.username, options.password);

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
					if(op.arg != null) {
						log.error("operation " + op.opType + " failed on " + op.arg, e);
					} else {
						log.error("operation " + op.opType + " failed", e);
					}
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
