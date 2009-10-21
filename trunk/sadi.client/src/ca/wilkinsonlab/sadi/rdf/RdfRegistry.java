package ca.wilkinsonlab.sadi.rdf;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.client.Registry;
import ca.wilkinsonlab.sadi.client.ServiceInputPair;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.utils.PredicateUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.virtuoso.VirtuosoRegistry;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.ResourceUtils;

/**
 * A registry of SADI native RDF services.
 * @author Luke McCarthy
 */
public class RdfRegistry extends VirtuosoRegistry implements Registry
{
	private static final Log log = LogFactory.getLog(RdfRegistry.class);
	
	static final String ENDPOINT_CONFIG_KEY = "endpoint";
	static final String GRAPH_CONFIG_KEY = "graph";
	static final boolean CACHE_ENABLED = true;
	
	private Map<String, RdfService> serviceCache;
	private OntModel predicateOntology;
	
	/**
	 * Construct a new RdfRegistry from the specified Configuration.
	 * @param config the Configuration
	 * @throws IOException if there is an error contacting the registry
	 */
	public RdfRegistry(Configuration config) throws IOException
	{
		this(config.getString(ENDPOINT_CONFIG_KEY), config.getString(GRAPH_CONFIG_KEY));
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.virtuoso.VirtuosoRegistry#VirtuosoRegistry(java.lang.String, java.lang.String)
     */
	public RdfRegistry(String sparqlEndpoint, String graphName) throws IOException
	{
		this(new URL(sparqlEndpoint), graphName);
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.virtuoso.VirtuosoRegistry#VirtuosoRegistry(java.lang.String)
     */
	public RdfRegistry(String sparqlEndpoint) throws IOException
	{
		this(sparqlEndpoint, null);
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.virtuoso.VirtuosoRegistry#VirtuosoRegistry(java.net.URL, java.lang.String)
     */
	public RdfRegistry(URL sparqlEndpoint, String graphName) throws IOException
	{
		super(sparqlEndpoint, graphName);
		
		/* TODO replace this with some more sophisticated caching mechanism;
		 * I assume EHCache.
		 */
		serviceCache = new HashMap<String, RdfService>();
		
		predicateOntology = createPredicateOntology();
	}
	
	/**
	 * Returns the set of predicates used to annotate services in the registry.
	 * @return the set of predicates used to annotate services in the registry
	 * @throws IOException
	 */
	private OntModel createPredicateOntology() throws IOException
	{
		// TODO do we need more reasoning here?
		OntModel predicateOntology = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );

//		String query = SPARQLStringUtils.readFully(RdfRegistry.class.getResource("resources/select.predicate.all.sparql"));
//		for (Map<String, String> binding: executeQuery(query))
//			OwlUtils.loadOntologyForUri(predicateOntology, binding.get("p"));

		return predicateOntology;
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#getPredicateOntology()
     */
	public OntModel getPredicateOntology()
	{
		return predicateOntology;
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#getService(java.lang.String)
     */
	public RdfService getService(String serviceURI)
	throws IOException
	{
		return getService(serviceURI, false);
	}
	
	private RdfService getService(String serviceURI, boolean withCheck)
	throws IOException
	{
		RdfService service = serviceCache.get(serviceURI);
		if (service != null)
			return service;
		
		if (withCheck) {
			String query = SPARQLStringUtils.strFromTemplate(
					SPARQLStringUtils.readFully(RdfRegistry.class.getResource("resources/select.service.byuri.sparql")),
					serviceURI, serviceURI
			);
			if (executeQuery(query).isEmpty())
				return null;
		}
		
		service = new RdfService(serviceURI);
		service.sourceRegistry = this;
		
		if (CACHE_ENABLED)
			serviceCache.put(serviceURI, service);
		
		return service;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#findServicesByPredicate(java.lang.String)
     */
	public Collection<RdfService> findServicesByPredicate(String predicate)
	throws IOException
	{
		Collection<RdfService> services = new ArrayList<RdfService>();

		/* 
		 * Registries must recognize predicates of the form "inv(predicate)".  
		 * However, this form of predicate will cause SPARQLStringUtils.strFromTemplate()
		 * to throw a URIException. -- BV
		 */
		if(PredicateUtils.isInverted(predicate))
			return services;
		
		String query = SPARQLStringUtils.strFromTemplate(
			SPARQLStringUtils.readFully(RdfRegistry.class.getResource("resources/select.service.bypredicate.sparql")),
			predicate
		);

		for (Map<String, String> binding: executeQuery(query))
			services.add(getService(binding.get("service")));
		
		return services;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#findServices(com.hp.hpl.jena.rdf.model.Resource)
     */
	public Collection<RdfService> findServices(Resource subject)
	throws IOException
	{
		return findServices(subject, false);
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#findServices(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
     */
	public Collection<RdfService> findServices(Resource subject, String predicate)
	throws IOException
	{
		return findServices(subject, predicate, false);
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#discoverServices(com.hp.hpl.jena.rdf.model.Model)
     */
	public Collection<ServiceInputPair> discoverServices(Model inputModel)
	throws IOException
	{
		Collection<ServiceInputPair> pairs = new ArrayList<ServiceInputPair>();
		for (RdfService service: getAllServices()) {
			try {
				for (Resource input: service.discoverInputInstances(inputModel)) {
					pairs.add(new ServiceInputPair(service, input));
				}
			} catch (Exception e) {
				log.error(String.format("error finding input instances for %s", service), e);
			}
		}
		return pairs;
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#findPredicatesBySubject(com.hp.hpl.jena.rdf.model.Resource)
     */
	public Collection<String> findPredicatesBySubject(Resource subject)
	throws IOException
	{
		return findPredicatesBySubject(subject, false);
	}
	
	/**
	 * Returns a collection of services that can consume the specified input
	 * data, optionally considering only the direct types of the input node.
	 * @param subject the input data
	 * @param direct if true, consider only the direct types of the input node;
	 *               if false, consider inferred types as well
	 * @return the collection of matching services
	 */
	public Collection<RdfService> findServices(Resource subject, boolean direct) throws IOException
	{
		/* TODO in the direct case, this could be done more efficiently by
		 * querying the input classes stored in the registry...
		 */
		return filterServicesByInput(getAllServices(), subject, direct);
	}

	/**
	 * Returns a collection of services that can attach the specified predicate
	 * to the specified subject, optionally considering only the direct types
	 * of the input node.
	 * @param subject the input data
	 * @param predicate the predicate
	 * @param direct if true, consider only the direct types of the input node;
	 *               if false, consider inferred types as well
	 * @return the collection of matching services
	 */
	public Collection<RdfService> findServices(Resource subject, String predicate, boolean direct)
	throws IOException
	{
		/* TODO in the direct case, this could be done more efficiently by
		 * querying the input class stored in the registry...
		 */
		return filterServicesByInput(findServicesByPredicate(predicate), subject, direct);
	}
	
	/**
	 * Returns a collection of predicates that are mapped to services that
	 * can take the specified subject as input, optionally considering only
	 * the direct types of the input node.
	 * @param subject the input data
	 * @param direct if true, consider only the direct types of the input node;
	 *               if false, consider inferred types as well
	 * @return the collection of matching predicates
	 * @throws IOException
	 */
	public Collection<String> findPredicatesBySubject(Resource subject, boolean direct)
	throws IOException
	{
		Collection<String> predicates = new ArrayList<String>();
		for (RdfService service: findServices(subject, direct))
			predicates.addAll(service.getPredicates());
		return predicates;
	}

	/**
	 * Returns a collection of all services in the registry.
	 * @return a collection of all services in the registry
	 * @throws IOException
	 */
	public Collection<RdfService> getAllServices()
	throws IOException
	{
		String query = SPARQLStringUtils.readFully(RdfRegistry.class.getResource("resources/select.service.all.sparql"));
		
		Collection<RdfService> services = new ArrayList<RdfService>();
		for (Map<String, String> binding: executeQuery(query))
			services.add(getService(binding.get("service")));
		
		return services;
	}

	/**
	 * Filter the specified list of services, returning only those that will
	 * accept the specified Resource as input.
	 * @param services the unfiltered list of services
	 * @param input the input data
	 * @param direct if true, consider only the direct types of the input node;
	 *               if false, consider inferred types as well
	 * @return the filtered list of services
	 */
	Collection<RdfService> filterServicesByInput(Collection<RdfService> services, Resource input, boolean direct)
	{
		Collection<RdfService> filteredServices = new ArrayList<RdfService>(services.size());
		if (direct) {
			/* TODO if this is too slow, we can create an ontology model that
			 * contains only the direct properties of the input node...
			 */
			OntModel base = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM, input.getModel() );
			for (RdfService service: services) {
				if (base.getIndividual(input.getURI()).hasRDFType(service.getInputClass(), true))
					filteredServices.add(service);
			}
		} else {
			Model base = ResourceUtils.reachableClosure((Resource)input);
			input = base.getResource(input.getURI());
			for (RdfService service: services) {
				if (service.isInputInstance(input))
					filteredServices.add(service);
			}
		}
		return filteredServices;
	}

	public ServiceStatus getServiceStatus(String serviceURI) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	public String toString()
	{
		return String.format("%s:%s", sparqlEndpoint, graphName);
	}
}
