package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInputPair;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.utils.PredicateUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;
import ca.wilkinsonlab.sadi.vocab.SPARQLRegistryOntology;
import ca.wilkinsonlab.sadi.vocab.W3C;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * 
 * @author Ben Vandervalk
 */
public class VirtuosoSPARQLRegistry extends VirtuosoSPARQLEndpoint implements SPARQLRegistry
{	
	public final static Log log = LogFactory.getLog(VirtuosoSPARQLRegistry.class);
	
	protected static final String ENDPOINT_CONFIG_KEY = "endpoint";
	protected static final String INDEX_GRAPH_CONFIG_KEY = "indexGraph";
	protected static final String ONTOLOGY_GRAPH_CONFIG_KEY = "ontologyGraph";
	protected static final String USERNAME_CONFIG_KEY = "username";
	protected static final String PASSWORD_CONFIG_KEY = "password";

	protected static final int ENDPOINT_QUERY_TIMEOUT = 20 * 1000; // in milliseconds
	//private OntModel predicateOntology;
	private String indexGraphURI;
	private String ontologyGraphURI;

	private Map<String, Collection<SPARQLEndpoint>> predicateToEndpointCache;
	private Map<String, String> subjectRegExMap;
	private Map<String, String> objectRegExMap;

	public VirtuosoSPARQLRegistry(Configuration config) throws HttpException, IOException
	{
		// NOTE: It may seem odd that a username/password is required here, since this class doesn't
		// actually do any write (update) queries.   However, in the future it *will* for two reasons:
		// 1) incrementally adding information to the index as it is learned,
		// 2) keeping the status of the SPARQL endpoints up to date (i.e. OK vs. SLOW vs. DEAD). 
		//  -- BV
		
		this(config.getString(ENDPOINT_CONFIG_KEY),
			config.getString(INDEX_GRAPH_CONFIG_KEY),
			config.getString(ONTOLOGY_GRAPH_CONFIG_KEY),
			config.getString(USERNAME_CONFIG_KEY),
			config.getString(PASSWORD_CONFIG_KEY));
	}
	
	public VirtuosoSPARQLRegistry(String URI, String indexGraphURI, String ontologyGraphURI, String username, String password) throws IOException
	{
		super(URI, username, password);
		this.indexGraphURI = indexGraphURI;
		this.ontologyGraphURI = ontologyGraphURI;
		predicateToEndpointCache = new Hashtable<String,Collection<SPARQLEndpoint>>();
		initRegExMaps();
	}

	public String getIndexGraphURI() 
	{
		return indexGraphURI;
	}

	public String getOntologyGraphURI()
	{
		return ontologyGraphURI;
	}

	public String getRegistryURI() { return this.getURI(); }

	public OntModel getPredicateOntology() 
	{
		log.trace("building predicate ontology");
		
		OntModel predicateOntology = ModelFactory.createOntologyModel();
		String predicateQuery = "SELECT ?s ?o FROM %u% WHERE { ?s %u% ?o }";
	
		List<Map<String,String>> results = null;
		try {
			predicateQuery = SPARQLStringUtils.strFromTemplate(predicateQuery, getOntologyGraphURI(), W3C.PREDICATE_RDF_TYPE);
			results = selectQuery(predicateQuery);
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}

		for(Map<String,String> binding : results) {
			String predicateURI = binding.get("s");
			String typeURI = binding.get("o");
			if(typeURI.equals(W3C.OWL_PREFIX + "DatatypeProperty"))
				predicateOntology.createDatatypeProperty(predicateURI);
			else
				predicateOntology.createObjectProperty(predicateURI);
		}
		return predicateOntology;
	}

	private void initRegExMaps() throws IOException
	{
		log.trace("loading regular expressions for subject/object URIs");
		initRegExMap(true);
		initRegExMap(false);
	}
	
	private void initRegExMap(boolean mapIsForSubject) throws IOException
	{
		String queryTemplate = 
				"SELECT * FROM %u% WHERE {\n" +
				"   ?endpoint %u% ?regex .\n" +
				"   ?endpoint %u% ?isComplete .\n" +
				"}";

		String regexPredicate, regexIsCompletePredicate;
		Map<String,String> regexMap;
		
		if(mapIsForSubject) {
			subjectRegExMap = new Hashtable<String,String>();
			regexMap = subjectRegExMap;
			regexPredicate = SPARQLRegistryOntology.PREDICATE_SUBJECT_REGEX;
			regexIsCompletePredicate = SPARQLRegistryOntology.PREDICATE_SUBJECT_REGEX_IS_COMPLETE;
		}
		else {
			objectRegExMap = new Hashtable<String,String>();
			regexMap = objectRegExMap;
			regexPredicate = SPARQLRegistryOntology.PREDICATE_OBJECT_REGEX;
			regexIsCompletePredicate = SPARQLRegistryOntology.PREDICATE_OBJECT_REGEX_IS_COMPLETE;
		}

		String query = SPARQLStringUtils.strFromTemplate(queryTemplate, 
									getIndexGraphURI(),
									regexPredicate,
									regexIsCompletePredicate);
		
		List<Map<String,String>> results = selectQuery(query); 

		for(Map<String,String> bindings: results) 
		{
			String endpointURI = bindings.get("endpoint");
			boolean regexIsComplete = bindings.get("isComplete").equals(String.valueOf(true));
			String regex = bindings.get("regex");

			if(regexMap.containsKey(endpointURI))
				throw new RuntimeException("registry is corrupt! " + endpointURI + " has more than one regex for its subject/object URIs");
			
			if(regexIsComplete)
				regexMap.put(endpointURI, regex);
		}
		
	}
	
	public Collection<SPARQLEndpoint> findEndpointsByPredicate(String predicate) throws IOException
	{
		if(PredicateUtils.isInverted(predicate))
			predicate = PredicateUtils.invert(predicate);
		
		if(predicateToEndpointCache.containsKey(predicate))
			return predicateToEndpointCache.get(predicate);

		Set<String> matchingEndpointURIs = findEndpointsByPredicateUsingIndex(predicate);

		// This step is too slow to be practical.
		//matchingEndpointURIs.addAll(findPartiallyIndexedEndpointsByPredicate(predicate, matchingEndpointURIs));

		Set<SPARQLEndpoint> matches = new HashSet<SPARQLEndpoint>();
		for(String uri : matchingEndpointURIs) 
			matches.add(getEndpoint(uri));

		predicateToEndpointCache.put(predicate, matches);
		
		return matches;
	}
	
	public Set<String> findEndpointsByPredicateUsingIndex(String predicate) throws IOException
	{
		Set<String> matchingEndpointURIs = new HashSet<String>();
		
		// Note: if no status is recorded for an endpoint, we assume it is dead.
		String predicatesQuery = 
			"SELECT ?endpoint ?status FROM %u% WHERE {\n" +
			"   ?endpoint %u% %u% .\n" +
			"   ?endpoint %u% ?status .\n" +
			"}";

		predicatesQuery = SPARQLStringUtils.strFromTemplate(predicatesQuery, 
							getIndexGraphURI(), 
							SPARQLRegistryOntology.PREDICATE_HASPREDICATE, predicate,
							SPARQLRegistryOntology.PREDICATE_ENDPOINTSTATUS);
		
		List<Map<String,String>> results = selectQuery(predicatesQuery);

		for(Map<String,String> binding : results) {
			String uri = binding.get("endpoint");
			if(ServiceStatus.valueOf(binding.get("status")) == ServiceStatus.DEAD)
				continue;
			matchingEndpointURIs.add(uri);
		}
		
		return matchingEndpointURIs;
	}
	
	public Set<String> findPartiallyIndexedEndpointsByPredicate(String predicate, Set<String> ignoreEndpointURIs) throws IOException
	{
		Set<String> matchingEndpointURIs = new HashSet<String>();

		// Note: if the index for an endpoint contains no indication about whether the
		// its predicate list is complete, we assume it is not.
		
		String endpointsQuery = 
			"SELECT ?endpoint ?status FROM %u% WHERE {\n" +
			"   OPTIONAL { ?endpoint %u% ?complete . } \n" +
			"   ?endpoint %u% ?status .\n" +
			"   FILTER (!bound(?complete) || (?complete = 'false'))\n" +
			"}";
		
		endpointsQuery = SPARQLStringUtils.strFromTemplate(endpointsQuery, 
							getIndexGraphURI(), 
							SPARQLRegistryOntology.PREDICATE_PREDICATE_LIST_IS_COMPLETE, 
							SPARQLRegistryOntology.PREDICATE_ENDPOINTSTATUS);
		
		List<Map<String,String>>results = selectQuery(endpointsQuery);
		
		String testForPredicateQuery = "SELECT * WHERE { ?s %u% ?o } LIMIT 1";
		testForPredicateQuery = SPARQLStringUtils.strFromTemplate(testForPredicateQuery, predicate);
		
		for(Map<String,String> binding : results) {
			
			String uri = binding.get("endpoint");
			ServiceStatus status = ServiceStatus.valueOf(binding.get("status"));

			if(ignoreEndpointURIs.contains(uri) || status == ServiceStatus.DEAD)
				continue;
			
			SPARQLEndpoint endpoint = getEndpoint(uri);
			List<Map<String,String>> testResults;
			try {
				testResults = endpoint.selectQuery(testForPredicateQuery, ENDPOINT_QUERY_TIMEOUT);
				if(testResults.size() > 0)
					matchingEndpointURIs.add(uri);
			} catch(Exception e) {
				log.warn("Query timed out when testing unindexed endpoint " + uri + " for triples containing " + predicate);
			}
		}
		
		return matchingEndpointURIs;
	}
	
	public Collection<SPARQLServiceWrapper> findServicesByPredicate(String predicate) throws IOException
	{
		boolean isInverted = false; 
		if(PredicateUtils.isInverted(predicate)) {
			predicate = PredicateUtils.invert(predicate);
			isInverted = true;
		}
		
		Set<SPARQLServiceWrapper> services = new HashSet<SPARQLServiceWrapper>();
		Collection<SPARQLEndpoint> endpoints = findEndpointsByPredicate(predicate);
		
		for(SPARQLEndpoint endpoint: endpoints)
			services.add(new SPARQLServiceWrapper(endpoint, this, predicate, isInverted));
		
		return services;
	}

	public EndpointType getEndpointType(String endpointURI) throws HttpException, IOException
	{
		if(!hasEndpoint(endpointURI))
			return null;
		String typeQuery = "SELECT ?type FROM %u% WHERE { %u% %u% ?type }";
		typeQuery = SPARQLStringUtils.strFromTemplate(typeQuery, getIndexGraphURI(), endpointURI, W3C.PREDICATE_RDF_TYPE);
		List<Map<String,String>> results = selectQuery(typeQuery);
		if(results.size() == 0) 
			throw new RuntimeException("No type found in registry for endpoint " + endpointURI);
		return EndpointType.valueOf(results.get(0).get("type"));
	}

	public boolean hasEndpoint(String endpointURI) throws HttpException, IOException 
	{
		String existsQuery = "SELECT * FROM %u% WHERE { %u% %u% ?o } LIMIT 1";
		existsQuery = SPARQLStringUtils.strFromTemplate(existsQuery, 
				getIndexGraphURI(), 
				endpointURI,
				SPARQLRegistryOntology.PREDICATE_ENDPOINTSTATUS);
		List<Map<String,String>> results = selectQuery(existsQuery);

		if(results.size() > 1) {
			throw new RuntimeException("SPARQL registry is corrupt! More than one triple with predicate " + 
					SPARQLRegistryOntology.PREDICATE_ENDPOINTSTATUS + " exists for " + endpointURI);
		}
		
		if(results.size() == 1)
			return true;
		else
			return false;
	}
	
	public List<SPARQLEndpoint> getAllEndpoints() throws IOException
	{
		List<SPARQLEndpoint> endpoints = new ArrayList<SPARQLEndpoint>();
		String endpointQuery = "SELECT DISTINCT ?endpoint ?type FROM %u% WHERE { ?endpoint %u% ?type }";
		endpointQuery = SPARQLStringUtils.strFromTemplate(endpointQuery, getIndexGraphURI(), W3C.PREDICATE_RDF_TYPE);
		List<Map<String,String>> results = selectQuery(endpointQuery);
		for(Map<String,String> binding : results) {
			EndpointType type = EndpointType.valueOf(binding.get("type"));
			endpoints.add(SPARQLEndpointFactory.createEndpoint(binding.get("endpoint"), type));
		}
		return endpoints;
	}
	
	public List<SPARQLServiceWrapper> getAllServices() throws IOException
	{
		List<SPARQLServiceWrapper> services = new ArrayList<SPARQLServiceWrapper>();
		for(SPARQLEndpoint endpoint: getAllEndpoints())
			services.add(new SPARQLServiceWrapper(endpoint, this));
		return services;
	}

	public List<String> getEndpointURIs() throws HttpException, IOException
	{
		List<String> endpoints = new ArrayList<String>();
		String endpointQuery = "SELECT DISTINCT ?endpoint ?type FROM %u% WHERE { ?endpoint %u% ?type }";
		endpointQuery = SPARQLStringUtils.strFromTemplate(endpointQuery, getIndexGraphURI(), W3C.PREDICATE_RDF_TYPE);
		List<Map<String,String>> results = selectQuery(endpointQuery);
		for(Map<String,String> binding : results)
			endpoints.add(binding.get("endpoint"));
		return endpoints;
	}
	
	public ServiceStatus getServiceStatus(String serviceURI) throws URIException, HttpException, HttpResponseCodeException, IOException 
	{
		String statusQuery = "SELECT ?status FROM %u% WHERE { %u% %u% ?status }";
		statusQuery = SPARQLStringUtils.strFromTemplate(statusQuery, getIndexGraphURI(), serviceURI, SPARQLRegistryOntology.PREDICATE_ENDPOINTSTATUS);
		List<Map<String,String>> results = selectQuery(statusQuery);
		if(results.size() == 0)
			throw new RuntimeException("Unable to obtain endpoint status for " + serviceURI + " from registry");
		return ServiceStatus.valueOf(results.get(0).get("status"));
	}
	
	public long getNumTriplesOrLowerBound(String endpointURI) throws IOException 
	{
		long numTriples = getNumTriples(endpointURI);
		if(numTriples == SPARQLRegistryOntology.NO_VALUE_AVAILABLE)
			numTriples = getNumTriplesLowerBound(endpointURI);
		return numTriples;
	}

	public long getNumTriples(String endpointURI) throws IOException
	{
		if(!hasEndpoint(endpointURI))
			throw new IllegalArgumentException("registry does not contain endpoint " + endpointURI);

		String query = "SELECT ?num FROM %u% WHERE { %u% %u% ?num }";
		query = SPARQLStringUtils.strFromTemplate(query, getIndexGraphURI(), endpointURI, SPARQLRegistryOntology.PREDICATE_NUMTRIPLES);
		List<Map<String,String>> results = selectQuery(query);
		
		if(results.size() == 0)
			return SPARQLRegistryOntology.NO_VALUE_AVAILABLE;
		String firstColumn = results.get(0).keySet().iterator().next();
		return Long.valueOf(results.get(0).get(firstColumn));
	}
	
	public long getNumTriplesLowerBound(String endpointURI) throws IOException
	{
		if(!hasEndpoint(endpointURI))
			throw new IllegalArgumentException("registry index does not contain endpoint " + endpointURI);

		String query = "SELECT ?num FROM %u% WHERE { %u% %u% ?num }";
		query = SPARQLStringUtils.strFromTemplate(query, getIndexGraphURI(), endpointURI, SPARQLRegistryOntology.PREDICATE_NUMTRIPLES_LOWER_BOUND);
		List<Map<String,String>> results = selectQuery(query);
		
		if(results.size() == 0)
			return SPARQLRegistryOntology.NO_VALUE_AVAILABLE;
		String firstColumn = results.get(0).keySet().iterator().next();
		return Long.valueOf(results.get(0).get(firstColumn));
	}
		
	public ServiceStatus getEndpointStatus(String endpointURI) throws IOException
	{
		if(!hasEndpoint(endpointURI))
			throw new IllegalArgumentException("The registry does not registry index does not contain: " + endpointURI);
			
		String query = "SELECT ?status FROM %u% WHERE { %u% %u% ?status }";
		query = SPARQLStringUtils	.strFromTemplate(query,
						getIndexGraphURI(),
						endpointURI, SPARQLRegistryOntology.PREDICATE_ENDPOINTSTATUS);
		
		List<Map<String,String>> results = selectQuery(query);
		
		if(results.size() == 0)
			throw new RuntimeException("Registry is corrupt! No endpoint status recorded for " + endpointURI);
		else if(results.size() > 1)
			throw new RuntimeException("Registry is corrupt! More than one endpoint status recorded for " + endpointURI);
		
		return ServiceStatus.valueOf(results.iterator().next().get("status"));
	}
	
	public boolean hasPredicate(String predicateURI) throws IOException
	{
		String query = "SELECT * FROM %u% WHERE { %u% ?p ?o } LIMIT 1";
		query = SPARQLStringUtils.strFromTemplate(query, getOntologyGraphURI(), predicateURI);
		List<Map<String,String>> results = selectQuery(query);
		if(results.size() > 0)
			return true;
		else 
			return false;
	}
	
	public boolean isDatatypeProperty(String predicateURI) throws IOException
	{
		String query = "SELECT ?o FROM %u% WHERE { %u% %u% ?o } LIMIT 1";
		query = SPARQLStringUtils.strFromTemplate(query,
				getOntologyGraphURI(),
				predicateURI,
				W3C.PREDICATE_RDF_TYPE);
		List<Map<String,String>> results = selectQuery(query);
		
		if(results.size() == 0) {
			throw new RuntimeException("The SPARQL registry have the predicate: " + predicateURI 
					+ ". You need to need to check with registry.hasPredicate() first.");
		}
		if(results.get(0).get("o").equals(W3C.TYPE_DATATYPE_PROPERTY))
			return true;
		else 
			return false;
	}
	
	public Collection<String> getPredicatesForEndpoint(String endpointURI) throws IOException
	{
		if(!hasEndpoint(endpointURI))
			throw new IllegalArgumentException("The registry does not registry index does not contain: " + endpointURI);

		Collection<String> predicates = new HashSet<String>();

		String query = "SELECT ?predicate FROM %u% WHERE { %u% %u% ?predicate }";
		query = SPARQLStringUtils.strFromTemplate(query, 
				getIndexGraphURI(),
				endpointURI, SPARQLRegistryOntology.PREDICATE_HASPREDICATE);
		List<Map<String,String>> results = selectQuery(query);

		for(Map<String,String> bindings: results)
			predicates.add(bindings.get("predicate"));

		return predicates;
	}

	public boolean subjectMatchesRegEx(String endpointURI, String uri) throws IOException
	{
		return matchesRegEx(endpointURI, uri, true);
	}
	
	public boolean objectMatchesRegEx(String endpointURI, String uri) throws IOException
	{
		return matchesRegEx(endpointURI, uri, false);
	}
	
	public boolean matchesRegEx(String endpointURI, String uri, boolean uriIsSubject) throws IOException
	{
		String regex = getRegEx(endpointURI, uriIsSubject);
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(uri);
		boolean matches = matcher.find();
		/*
		log.trace("endpoint: " + endpointURI);
		log.trace("regex: " + pattern.toString());
		log.trace("uri: " + uri);
		log.trace("matches?: " + String.valueOf(matches));
		*/
		return matches;
	}
	
	public String getSubjectRegEx(String endpointURI) throws IOException 
	{ 
		return getRegEx(endpointURI, true); 
	}
	
	public String getObjectRegEx(String endpointURI) throws IOException 
	{ 
		return getRegEx(endpointURI, false); 
	} 
	
	public String getRegEx(String endpointURI, boolean positionIsSubject) 
	{
		Map<String,String> regexMap = positionIsSubject ? subjectRegExMap : objectRegExMap;
		if(regexMap.containsKey(endpointURI))
			return regexMap.get(endpointURI);
		else
			return ".*";
	}
	
	public Collection<String> findPredicatesBySubject(String subject) throws IOException 
	{
		log.warn("This method is not implemented.");
		return new ArrayList<String>(0);
	}

	public Collection<SPARQLServiceWrapper> findServices(String subject, String predicate) throws URIException, HttpException, IOException
	{
		return findServicesByPredicate(predicate);
	}

	public SPARQLEndpoint getEndpoint(String endpointURI) throws IOException
	{
		if(!hasEndpoint(endpointURI))
			throw new IllegalArgumentException("The SPARQL registry does not contain an entry for the endpoint " + endpointURI);
		return SPARQLEndpointFactory.createEndpoint(endpointURI, getEndpointType(endpointURI));
	}
	
	public Service getService(String serviceURI) throws HttpException, IOException 
	{
		return new SPARQLServiceWrapper(getEndpoint(serviceURI), this);
	}

	public Collection<String> findPredicatesBySubject(Resource subject) throws IOException
	{
		return findPredicatesBySubject(subject.getURI());
	}

	public Collection<SPARQLServiceWrapper> findServices(Resource subject, String predicate) throws IOException
	{
		return findServices(subject.getURI(), predicate);
	}

	public Collection<ServiceInputPair> discoverServices(Model model) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public Collection<SPARQLServiceWrapper> findServices(Resource subject) throws IOException
	{
		throw new UnsupportedOperationException();
	}
	
	protected static class DeadEndpointCache {
		
	}
}
