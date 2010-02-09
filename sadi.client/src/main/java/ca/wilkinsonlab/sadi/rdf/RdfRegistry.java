package ca.wilkinsonlab.sadi.rdf;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.Registry;
import ca.wilkinsonlab.sadi.client.ServiceInputPair;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.virtuoso.VirtuosoRegistry;

import com.hp.hpl.jena.ontology.OntClass;
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
	private static final Logger log = Logger.getLogger(RdfRegistry.class);
	
	static final String ENDPOINT_CONFIG_KEY = "endpoint";
	static final String GRAPH_CONFIG_KEY = "graph";
	static final boolean CACHE_ENABLED = true;
	
	private Map<String, RdfService> serviceCache;
	
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
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#getService(java.lang.String)
     */
	public RdfService getService(String serviceURI)
	throws IOException
	{
		RdfService service = serviceCache.get(serviceURI);
		if (service == null) {
			String query = SPARQLStringUtils.strFromTemplate(
					SPARQLStringUtils.readFully(RdfRegistry.class.getResource("getService.sparql")),
					serviceURI
			);
			List<Map<String, String>> bindings = executeQuery(query);
			/* TODO remove this try/catch and just throw the exception,
			 * once the registry methods all do...
			 */ 
			try {
			if (bindings.isEmpty()) {
				throw new SADIException(String.format("no such service %s in this registry", serviceURI));
			} else if (bindings.size() > 1) {
				throw new SADIException(String.format("URI %s maps to more than one service in this registry", serviceURI));
			}
			service = createService(bindings.get(0));
			} catch (SADIException e) {
				throw new IOException(e);
			}
		}
		return service;
	}
	
	/**
	 * Create a Service object from the details in the supplied map.  This map
	 * should contain values for all the required service fields.  In practice,
	 * the map be a variable binding for an appropriate SPARQL query on the
	 * registry endpoint, which allows the registry to return an immediately
	 * useful Service object without having to fetch the full definition every
	 * time.
	 * @param binding
	 * @return 
	 * @throws SADIException
	 */
	RdfService createService(Map<String, String> binding) throws SADIException
	{
		String serviceURI = binding.get("serviceURI");
		RdfService service = serviceCache.get(serviceURI);
		if (service == null) {
			service = new RdfService(binding);
			if (CACHE_ENABLED)
				serviceCache.put(serviceURI, service);
		}
		return service;
	}

	/**
	 * Returns a collection of all services in the registry.
	 * @return a collection of all services in the registry
	 * @throws IOException
	 */
	public Collection<RdfService> getAllServices()
	throws IOException
	{
		Collection<RdfService> services = new ArrayList<RdfService>();
		String query = SPARQLStringUtils.readFully(RdfRegistry.class.getResource("getAllServices.sparql"));
		for (Map<String, String> binding: executeQuery(query)) {
			try {
				services.add(createService(binding));
			} catch (SADIException e) {
				log.error(String.format("error creating service from registry data %s", binding), e);
			}
		}
		return services;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#findServicesByPredicate(java.lang.String)
     */
	public Collection<RdfService> findServicesByPredicate(String predicate)
	throws IOException
	{
		Collection<RdfService> services = new ArrayList<RdfService>();
		String query = SPARQLStringUtils.strFromTemplate(
				SPARQLStringUtils.readFully(RdfRegistry.class.getResource("findServicesByPredicate.sparql")),
				predicate
		);
		for (Map<String, String> binding: executeQuery(query)) {
			try {
				services.add(createService(binding));
			} catch (SADIException e) {
				log.error(String.format("error creating service from registry data %s", binding), e);
			}
		}
		return services;
	}
	
	public Collection<RdfService> findServicesByInputClass(OntClass clazz)
	throws IOException
	{
		return findServicesByInputClass(clazz, true);
	}
	
	public Collection<RdfService> findServicesByInputClass(OntClass clazz, boolean withReasoning)
	throws IOException
	{
		Collection<RdfService> services = new ArrayList<RdfService>();
		if (withReasoning) {
			services.addAll(findServicesByInputClass(clazz, false));
			for (Iterator<OntClass> superClasses = clazz.listSuperClasses(); superClasses.hasNext(); ) {
				services.addAll(findServicesByInputClass(superClasses.next(), false));
			}
		} else {
			if (!clazz.isURIResource())
				return services;
			String query = SPARQLStringUtils.strFromTemplate(
					SPARQLStringUtils.readFully(RdfRegistry.class.getResource("findServicesByInputClass.sparql")),
					clazz.getURI()
			);
			for (Map<String, String> binding: executeQuery(query)) {
				try {
					services.add(createService(binding));
				} catch (SADIException e) {
					log.error(String.format("error creating service from registry data %s", binding), e);
				}
			}
		}
		return services;
	}
	
	public Collection<RdfService> findServicesByConnectedClass(OntClass clazz)
	throws IOException
	{
		return findServicesByConnectedClass(clazz, true);
	}
	
	public Collection<RdfService> findServicesByConnectedClass(OntClass clazz, boolean withReasoning)
	throws IOException
	{
		Collection<RdfService> services = new ArrayList<RdfService>();
		if (withReasoning) {
			services.addAll(findServicesByConnectedClass(clazz, false));
			for (Iterator<OntClass> subClasses = clazz.listSubClasses(); subClasses.hasNext(); ) {
				services.addAll(findServicesByConnectedClass(subClasses.next(), false));
			}
		} else {
			if (!clazz.isURIResource())
				return services;
			String query = SPARQLStringUtils.strFromTemplate(
					SPARQLStringUtils.readFully(RdfRegistry.class.getResource("findServicesByConnectedClass.sparql")),
					clazz.getURI()
			);
			for (Map<String, String> binding: executeQuery(query)) {
				try {
					services.add(createService(binding));
				} catch (SADIException e) {
					log.error(String.format("error creating service from registry data %s", binding), e);
				}
		
			}
		}
		return services;
	}
	
	/**
	 * @deprecated not useful
	 * @param clazz
	 * @return
	 * @throws IOException
	 */
	public Collection<RdfService> findServicesByOutputClass(OntClass clazz)
	throws IOException
	{
		return findServicesByOutputClass(clazz, true);
	}
	
	/**
	 * @deprecated not useful
	 * @param clazz
	 * @param withReasoning
	 * @return
	 * @throws IOException
	 */
	public Collection<RdfService> findServicesByOutputClass(OntClass clazz, boolean withReasoning)
	throws IOException
	{
		Collection<RdfService> services = new ArrayList<RdfService>();
		if (withReasoning) {
			services.addAll(findServicesByOutputClass(clazz, false));
			for (Iterator<OntClass> subClasses = clazz.listSubClasses(); subClasses.hasNext(); ) {
				services.addAll(findServicesByOutputClass(subClasses.next(), false));
			}
		} else {
			if (!clazz.isURIResource())
				return services;
			String query = SPARQLStringUtils.strFromTemplate(
					SPARQLStringUtils.readFully(RdfRegistry.class.getResource("findServicesByOutputClass.sparql")),
					clazz.getURI()
			);
			for (Map<String, String> binding: executeQuery(query)) {
				try {
					services.add(createService(binding));
				} catch (SADIException e) {
					log.error(String.format("error creating service from registry data %s", binding), e);
				}
			}
		}
		return services;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#findServices(com.hp.hpl.jena.rdf.model.Resource)
     */
	public Collection<RdfService> findServicesByInputInstance(Resource subject)
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
		for (RdfService service: findServices(subject, direct)) {
			try {
				predicates.addAll(service.getPredicates());
			} catch (SADIException e) {
				log.error(String.format("error determining predicates attached by service %s", service), e);
			}
		}
		return predicates;
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
			OntModel base = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF, input.getModel() );
			for (RdfService service: services) {
				try {
					if (base.getIndividual(input.getURI()).hasRDFType(service.getInputClass(), true))
						filteredServices.add(service);
				} catch (SADIException e) {
					log.error(String.format("error loading input class %s for service %s", service.getInputClassURI(), service), e);
				}
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
