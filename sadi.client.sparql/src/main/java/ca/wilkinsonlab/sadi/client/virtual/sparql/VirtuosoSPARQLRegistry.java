package ca.wilkinsonlab.sadi.client.virtual.sparql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.RegistrySearchCriteria;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInputPair;
import ca.wilkinsonlab.sadi.client.ServiceStatus;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.vocab.SPARQLRegistryOntology;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * VirtuosoSPARQLRegistry.
 * @author Ben Vandervalk
 */
public class VirtuosoSPARQLRegistry extends VirtuosoSPARQLEndpoint implements SPARQLRegistry
{	
	public final static Logger log = Logger.getLogger(VirtuosoSPARQLRegistry.class);
	
	protected static final String ENDPOINT_CONFIG_KEY = "endpoint";
	protected static final String INDEX_GRAPH_CONFIG_KEY = "indexGraph";
	protected static final String USERNAME_CONFIG_KEY = "username";
	protected static final String PASSWORD_CONFIG_KEY = "password";

	private String indexGraphURI;

	private Map<String, String> subjectRegExMap;
	private Map<String, String> objectRegExMap;

	public VirtuosoSPARQLRegistry(Configuration config) throws IOException
	{
		// NOTE: It may seem odd that a username/password is required here, since this class doesn't
		// actually do any write (update) queries.   However, in the future it *will* for two reasons:
		// 1) incrementally adding information to the index as it is learned,
		// 2) keeping the status of the SPARQL endpoints up to date (i.e. OK vs. SLOW vs. DEAD). 
		//  -- BV
		
		this(config.getString(ENDPOINT_CONFIG_KEY),
			config.getString(INDEX_GRAPH_CONFIG_KEY),
			config.getString(USERNAME_CONFIG_KEY),
			config.getString(PASSWORD_CONFIG_KEY));
	}
	
	public VirtuosoSPARQLRegistry(String URI, String indexGraphURI) throws IOException
	{
		this(URI, indexGraphURI, null, null);
	}
	
	public VirtuosoSPARQLRegistry(String URI, String indexGraphURI, String username, String password) throws IOException
	{
		super(URI, username, password);
		this.indexGraphURI = indexGraphURI;
		initRegExMaps();
	}

	public String getIndexGraphURI() 
	{
		return indexGraphURI;
	}

	public String getRegistryURI() { return this.getURI(); }

	public Collection<String> getAllPredicates() throws IOException 
	{
		log.trace("retrieving list of all predicates from SPARQL registry");
		
		String predicateQuery = "SELECT DISTINCT ?o FROM %u% WHERE { ?s %u% ?o }";
		predicateQuery = SPARQLStringUtils.strFromTemplate(predicateQuery, getIndexGraphURI(), SPARQLRegistryOntology.HAS_PREDICATE);

		Set<String> predicates = new HashSet<String>();
		for(Map<String,String> binding : selectQuery(predicateQuery)) {
			predicates.add(binding.get("o"));
		}
		
		return predicates;
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
			regexPredicate = SPARQLRegistryOntology.SUBJECT_REGEX;
			regexIsCompletePredicate = SPARQLRegistryOntology.SUBJECT_REGEX_IS_COMPLETE;
		}
		else {
			objectRegExMap = new Hashtable<String,String>();
			regexMap = objectRegExMap;
			regexPredicate = SPARQLRegistryOntology.OBJECT_REGEX;
			regexIsCompletePredicate = SPARQLRegistryOntology.OBJECT_REGEX_IS_COMPLETE;
		}

		String query = SPARQLStringUtils.strFromTemplate(queryTemplate, 
									getIndexGraphURI(),
									regexPredicate,
									regexIsCompletePredicate);
		
		List<Map<String,String>> results = selectQuery(query); 

		for(Map<String,String> bindings: results) 
		{
			// Note: For performance reasons, we intentionally ignore the regExIsComplete property here.
			// (All regexes which are not guaranteed to be complete are close to complete.)
			
			String endpointURI = bindings.get("endpoint");
			String regex = bindings.get("regex");

			if(regexMap.containsKey(endpointURI))
				throw new RuntimeException("registry is corrupt! " + endpointURI + " has more than one regex for its subject/object URIs");
			
			regexMap.put(endpointURI, regex);
		}
		
	}
	
	public Collection<SPARQLEndpoint> findSPARQLEndpointsByTriplePattern(Triple triplePattern) throws SADIException
	{
		Node s = triplePattern.getSubject();
		Node p = triplePattern.getPredicate();
		Node o = triplePattern.getObject();

		if(s.isBlank() || p.isBlank() || o.isBlank()) {
			throw new IllegalArgumentException("blank nodes are not allowed in any position of the triple pattern");
		}
		
		Collection<SPARQLEndpoint> unfiltered = p.isVariable() ? getAllSPARQLEndpoints() : findEndpointsByPredicate(p.getURI());
		Collection<SPARQLEndpoint> matches = new ArrayList<SPARQLEndpoint>();  
		
		for(SPARQLEndpoint endpoint : unfiltered) {
			boolean subjectMatches = (s.isVariable() || (s.isURI() && subjectMatchesRegEx(endpoint.getURI(), s.getURI())));
			boolean objectMatches = (o.isVariable() || (o.isURI() && objectMatchesRegEx(endpoint.getURI(), o.getURI())));
			if(subjectMatches && objectMatches) {
				matches.add(endpoint);
			}
		}
		return matches;
	}
	
	public Collection<SPARQLServiceWrapper> findServicesByTriplePattern(Triple pattern, boolean patternIsInverted) throws SADIException
	{
		Collection<SPARQLEndpoint> endpoints = findSPARQLEndpointsByTriplePattern(pattern);
		Collection<SPARQLServiceWrapper> services = new ArrayList<SPARQLServiceWrapper>();
		
		for(SPARQLEndpoint endpoint: endpoints)
			services.add(new SPARQLServiceWrapper(endpoint, this, patternIsInverted));
		
		return services;
	}
	
	public Collection<SPARQLEndpoint> findEndpointsByPredicate(String predicate) throws SADIException
	{
		Set<String> matchingEndpointURIs = findEndpointsByPredicateUsingIndex(predicate);

		Set<SPARQLEndpoint> matches = new HashSet<SPARQLEndpoint>();
		for(String uri : matchingEndpointURIs) 
			matches.add(getSPARQLEndpoint(uri));

		return matches;
	}
	
	public Set<String> findEndpointsByPredicateUsingIndex(String predicate) throws SADIException
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
							SPARQLRegistryOntology.HAS_PREDICATE, predicate,
							SPARQLRegistryOntology.ENDPOINT_STATUS);
		
		try {
			
			List<Map<String,String>> results = selectQuery(predicatesQuery);

			for(Map<String,String> binding : results) {
				String uri = binding.get("endpoint");
				if(ServiceStatus.valueOf(binding.get("status")) == ServiceStatus.DEAD)
					continue;
				matchingEndpointURIs.add(uri);
			}
		
		} catch(IOException e) {
			throw new SADIException(e);
		}
		
		return matchingEndpointURIs;
	}
	
	public Collection<SPARQLServiceWrapper> findServicesByPredicate(String predicate) throws SADIException
	{
		Node s = NodeCreateUtils.create("?var1");
		Node p = NodeCreateUtils.create(predicate);
		Node o = NodeCreateUtils.create("?var2");
		
		// TODO: Remove this hack; this will only be possible when "predicate" is being
		// passed in as a Jena Property rather than as a String.  (Having access to the predicate
		// as a Property allows for the retrieval of synonyms and inverses.) -- BV

		boolean isInverted = false;
		if(predicate.endsWith("-inverse")) {
			p = NodeCreateUtils.create(StringUtils.substringBeforeLast(predicate, "-inverse"));
			Node tmp = s;
			s = o;
			o = tmp;
			isInverted = true;
		} 

		Triple pattern = new Triple(s, p, o);
		return findServicesByTriplePattern(pattern, isInverted);
	}

	public EndpointType getEndpointType(String endpointURI) throws IOException
	{
		if(!hasEndpoint(endpointURI)) 
			return null;
		String typeQuery = "SELECT ?type FROM %u% WHERE { %u% %u% ?type }";
		typeQuery = SPARQLStringUtils.strFromTemplate(typeQuery, getIndexGraphURI(), endpointURI, RDF.type.getURI());
		List<Map<String,String>> results = selectQuery(typeQuery);
		if(results.size() == 0) 
			throw new RuntimeException("No type found in registry for endpoint " + endpointURI);
		return EndpointType.valueOf(results.get(0).get("type"));
	}

	public long getResultsLimit(String endpointURI) throws IOException 
	{
		if(!hasEndpoint(endpointURI)) {
			throw new IllegalArgumentException(String.format("registry does not contain endpoint %s", endpointURI));
		}
		String query = "SELECT ?limit FROM %u% WHERE { %u% %u% ?limit }";
		query = SPARQLStringUtils.strFromTemplate(query, getIndexGraphURI(), endpointURI, SPARQLRegistryOntology.RESULTS_LIMIT);
		List<Map<String,String>> results = selectQuery(query);
		if(results.size() == 0) { 
			log.warn(String.format("no results limit was found for %s, returning NO_RESULTS_LIMIT", endpointURI));
			return SPARQLEndpoint.NO_RESULTS_LIMIT;
		}
		return Long.valueOf(results.get(0).get("limit"));
	}
	
	public boolean hasEndpoint(String endpointURI) throws IOException 
	{
		String existsQuery = "SELECT * FROM %u% WHERE { %u% %u% ?o } LIMIT 1";
		existsQuery = SPARQLStringUtils.strFromTemplate(existsQuery, 
				getIndexGraphURI(), 
				endpointURI,
				SPARQLRegistryOntology.ENDPOINT_STATUS);
		List<Map<String,String>> results = selectQuery(existsQuery);

		if(results.size() > 1) {
			throw new RuntimeException("SPARQL registry is corrupt! More than one triple with predicate " + 
					SPARQLRegistryOntology.ENDPOINT_STATUS + " exists for " + endpointURI);
		}
		
		if(results.size() == 1)
			return true;
		else
			return false;
	}
	
	public List<SPARQLEndpoint> getAllSPARQLEndpoints() throws SADIException
	{
		List<SPARQLEndpoint> endpoints = new ArrayList<SPARQLEndpoint>();

		try {
			String endpointQuery = "SELECT DISTINCT ?endpoint ?type FROM %u% WHERE { ?endpoint %u% ?type }";
			endpointQuery = SPARQLStringUtils.strFromTemplate(endpointQuery, getIndexGraphURI(), RDF.type.getURI());
			List<Map<String,String>> results = selectQuery(endpointQuery);
			for(Map<String,String> binding : results) {
				EndpointType type = EndpointType.valueOf(binding.get("type"));
				String endpointURI = binding.get("endpoint");
				SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
				endpoint.setResultsLimit(getResultsLimit(endpointURI));
				endpoints.add(endpoint);
			}
		} catch(IOException e) {
			throw new SADIException(e);
		}
		
		return endpoints;
	}
	
	public List<SPARQLServiceWrapper> getAllServices() throws SADIException
	{
		List<SPARQLServiceWrapper> services = new ArrayList<SPARQLServiceWrapper>();

		for(SPARQLEndpoint endpoint: getAllSPARQLEndpoints()) {
			// return both inverted and non-inverted versions of the SPARQL service
			services.add(new SPARQLServiceWrapper(endpoint, this, false));
			services.add(new SPARQLServiceWrapper(endpoint, this, true));
		}
		return services;
	}

	public List<String> getEndpointURIs() throws IOException
	{
		List<String> endpoints = new ArrayList<String>();
		String endpointQuery = "SELECT DISTINCT ?endpoint ?type FROM %u% WHERE { ?endpoint %u% ?type }";
		endpointQuery = SPARQLStringUtils.strFromTemplate(endpointQuery, getIndexGraphURI(), RDF.type.getURI());
		List<Map<String,String>> results = selectQuery(endpointQuery);
		for(Map<String,String> binding : results)
			endpoints.add(binding.get("endpoint"));
		return endpoints;
	}
	
	public ServiceStatus getServiceStatus(String serviceURI) throws SADIException 
	{
		String statusQuery = "SELECT ?status FROM %u% WHERE { %u% %u% ?status }";
		statusQuery = SPARQLStringUtils.strFromTemplate(statusQuery, getIndexGraphURI(), serviceURI, SPARQLRegistryOntology.ENDPOINT_STATUS);
		try {
			List<Map<String,String>> results = selectQuery(statusQuery);
			// if the endpoint is registered, it is guaranteed to have a status...
			if(results.size() == 0)
				return null;
//			else if(results.size() > 1) // this shouldn't happen if everyone uses setServiceStatus below...
//				throw new SADIException("Unable to obtain endpoint status for " + serviceURI + " from registry");
			else
				return ServiceStatus.valueOf(results.get(0).get("status"));
		} catch (IOException e) {
			throw new SADIException(e.toString());
		}
	}
	
	public void setServiceStatus(String serviceURI, ServiceStatus newStatus) throws IOException 
	{
		String deleteQuery = "DELETE FROM GRAPH %u% { %u% %u% ?status } FROM %u% WHERE { %u% %u% ?status }";
		deleteQuery = SPARQLStringUtils.strFromTemplate(deleteQuery, 
				getIndexGraphURI(), 
				serviceURI, 
				SPARQLRegistryOntology.ENDPOINT_STATUS,
				getIndexGraphURI(),
				serviceURI,
				SPARQLRegistryOntology.ENDPOINT_STATUS);
		updateQuery(deleteQuery);
		
		String insertQuery = "INSERT INTO GRAPH %u% { %u% %u% %s% }";
		insertQuery = SPARQLStringUtils.strFromTemplate(insertQuery,
				getIndexGraphURI(),
				serviceURI,
				SPARQLRegistryOntology.ENDPOINT_STATUS,
				newStatus.toString());
		updateQuery(insertQuery);
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
		query = SPARQLStringUtils.strFromTemplate(query, getIndexGraphURI(), endpointURI, SPARQLRegistryOntology.NUM_TRIPLES);
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
		query = SPARQLStringUtils.strFromTemplate(query, getIndexGraphURI(), endpointURI, SPARQLRegistryOntology.NUM_TRIPLES_LOWER_BOUND);
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
						endpointURI, SPARQLRegistryOntology.ENDPOINT_STATUS);
		
		List<Map<String,String>> results = selectQuery(query);
		
		if(results.size() == 0)
			throw new RuntimeException("Registry is corrupt! No endpoint status recorded for " + endpointURI);
		else if(results.size() > 1)
			throw new RuntimeException("Registry is corrupt! More than one endpoint status recorded for " + endpointURI);
		
		return ServiceStatus.valueOf(results.iterator().next().get("status"));
	}
	
	public boolean hasPredicate(String predicateURI) throws IOException
	{
		String query = "SELECT * FROM %u% WHERE { ?s %u% %u% } LIMIT 1";
		query = SPARQLStringUtils.strFromTemplate(query, getIndexGraphURI(), SPARQLRegistryOntology.HAS_PREDICATE, predicateURI);
		List<Map<String,String>> results = selectQuery(query);
		if(results.size() > 0)
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
				endpointURI, SPARQLRegistryOntology.HAS_PREDICATE);
		List<Map<String,String>> results = selectQuery(query);

		for(Map<String,String> bindings: results)
			predicates.add(bindings.get("predicate"));

		return predicates;
	}

	public boolean subjectMatchesRegEx(String endpointURI, String uri) 
	{
		return matchesRegEx(endpointURI, uri, true);
	}
	
	public boolean objectMatchesRegEx(String endpointURI, String uri) 
	{
		return matchesRegEx(endpointURI, uri, false);
	}
	
	public boolean matchesRegEx(String endpointURI, String uri, boolean uriIsSubject)
	{
		String regex = getRegEx(endpointURI, uriIsSubject);
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(uri);
		boolean matches = matcher.find();
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
	
	public SPARQLEndpoint getSPARQLEndpoint(String endpointURI) throws SADIException 
	{
		SPARQLEndpoint endpoint = null;
		try {
			if(!hasEndpoint(endpointURI))
				throw new IllegalArgumentException("The SPARQL registry does not contain an entry for the endpoint " + endpointURI);
			endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, getEndpointType(endpointURI));
			endpoint.setResultsLimit(getResultsLimit(endpointURI));
		} catch(IOException e) {
			throw new SADIException(e);
		}
		return endpoint;
	}
	
	public Service getService(String serviceURI) throws SADIException 
	{
		return new SPARQLServiceWrapper(getSPARQLEndpoint(serviceURI), this);
	}

	public Collection<String> findPredicatesBySubject(Resource subject)
	{
		// Note: This method could be implemented by returning all predicates 
		// from every endpoint with a matching subject regex.  However this 
		// highly inaccurate and probably not wise.
		
		return Collections.emptyList();
	}

	public Collection<SPARQLServiceWrapper> findServices(Resource subject, String predicate) throws SADIException
	{
		Node s = subject.asNode();
		Node p = NodeCreateUtils.create(predicate);
		Node o = NodeCreateUtils.create("?var");
		
		// TODO: Remove this hack; this will only be possible when "predicate" is being
		// passed in as a Jena Property rather than as a String.  (Having access to the predicate
		// as a Property allows for the retrieval of synonyms and inverses.) -- BV

		boolean isInverted = false;
		if(predicate.endsWith("-inverse")) {
			p = NodeCreateUtils.create(StringUtils.substringBeforeLast(predicate, "-inverse"));
			Node tmp = s;
			s = o;
			o = tmp;
			isInverted = true;
		} 
		
		Triple pattern = new Triple(s, p, o);
		return findServicesByTriplePattern(pattern, isInverted);
	}
	
	public Collection<ServiceInputPair> discoverServices(Model model) throws SADIException
	{
		// At the current time, the only thing that act as input to a SPARQLServiceWrapper is a bare URI.
		// Hence, we just iterate through all Resources in the model and find endpoints that might
		// have some triples about them. We exclude literals from consideration here, because each literal 
		// would match *all* SPARQL endpoints.
		
		Collection<ServiceInputPair> serviceInputPairs = new ArrayList<ServiceInputPair>();

		Set<Resource> resourcesSeen = new HashSet<Resource>();

		for(ResIterator i = model.listSubjects(); i.hasNext(); ) {
			Resource input = i.next();
			if(!resourcesSeen.contains(input)) {
				for(SPARQLServiceWrapper service : findServicesByInputInstance(input)) {
					serviceInputPairs.add(new ServiceInputPair(service, input));
				}
				resourcesSeen.add(input);
			}
		}

		for(NodeIterator i = model.listObjects(); i.hasNext(); ) {
			RDFNode node = i.next();
			if(node.isResource()) {
				Resource input = node.as(Resource.class);
				if(!resourcesSeen.contains(input)) {
					for(SPARQLServiceWrapper service : findServicesByInputInstance(input)) {
						serviceInputPairs.add(new ServiceInputPair(service, input));
					}
					resourcesSeen.add(input);
				}
			}
		}
		
		return serviceInputPairs;
	}

	public Collection<SPARQLServiceWrapper> findServicesByInputInstance(Resource subject) throws SADIException
	{
		Node s = subject.asNode();
		Node p = NodeCreateUtils.create("?var1");
		Node o = NodeCreateUtils.create("?var2");
		
		Collection<SPARQLServiceWrapper> services = new HashSet<SPARQLServiceWrapper>();
		
		services.addAll(findServicesByTriplePattern(new Triple(s, p, o), false));
		services.addAll(findServicesByTriplePattern(new Triple(o, p, s), true));
		return services;
	}

	@Override
	public Collection<? extends Service> findServicesByAttachedProperty(Property property) throws SADIException {
		Set<String> propertyURIs= new HashSet<String>();
		if (property.isURIResource())
			propertyURIs.add(property.getURI());
		if (property.canAs(OntProperty.class)) {
			for (Iterator<? extends Property> i = property.as(OntProperty.class).listSubProperties(); i.hasNext(); ) {
				Property p = i.next();
				if (p.isURIResource())
					propertyURIs.add(p.getURI());
			}
		}
		Collection<SPARQLServiceWrapper> services = new ArrayList<SPARQLServiceWrapper>();
		for (String propertyURI: propertyURIs) {
			services.addAll(findServicesByPredicate(propertyURI));
		}
		return services;
	}

	@Override
	public Collection<? extends Service> findServicesByInputClass(Resource clazz) throws SADIException
	{
		// these services don't have input classes...
		return Collections.emptyList();
	}

	@Override
	public Collection<? extends Service> findServicesByConnectedClass(Resource clazz) throws SADIException
	{
		Set<String> classURIs = new HashSet<String>();
		if (clazz.isURIResource())
			classURIs.add(clazz.getURI());
		if (clazz.canAs(OntClass.class)) {
			for (Iterator<? extends OntClass> i = clazz.as(OntClass.class).listSubClasses(); i.hasNext(); ) {
				OntClass c = i.next();
				if (c.isURIResource())
					classURIs.add(c.getURI());
			}
		}
		/* FIXME
		 * use the assembled collection of class URIs to find triples matching 
		 * 	?s ?p o . ?o rdf:type ?classURI
		 */
		return Collections.emptyList();
	}

	@Override
	public Collection<? extends Service> discoverServices(Resource subject) throws SADIException
	{
		return findServicesByInputInstance(subject);
	}

	@Override
	public Collection<? extends Service> findServices(RegistrySearchCriteria criteria) throws SADIException
	{
		// FIXME
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Property> findAttachedProperties(RegistrySearchCriteria criteria) throws SADIException
	{
		// FIXME
		throw new UnsupportedOperationException();
	}
}
