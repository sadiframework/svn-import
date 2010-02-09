package ca.wilkinsonlab.sadi.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.utils.QueryExecutor;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

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
public class RegistryImpl extends RegistryBase
{
	private static final Logger log = Logger.getLogger(RegistryImpl.class);
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.RegistryBase#(java.lang.String)
	 */
	public RegistryImpl(Configuration config) throws IOException
	{
		super(config);
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.RegistryBase#(ca.wilkinsonlab.sadi.utils.QueryExecutor)
	 */
	RegistryImpl(QueryExecutor backend)
	{
		super(backend);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#getAllServices()
	 */
	@Override
	public Collection<ServiceImpl> getAllServices() throws SADIException
	{
		Collection<ServiceImpl> services = new ArrayList<ServiceImpl>();
		String query = "";
		try {
			query = SPARQLStringUtils.readFully(RegistryImpl.class.getResource("getAllServices.sparql"));
		} catch (IOException e) {
			throw new SADIException(e.toString());
		}
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
	@Override
	public Collection<ServiceImpl> findServicesByPredicate(String predicate) throws SADIException
	{
		Collection<ServiceImpl> services = new ArrayList<ServiceImpl>();
		String query = buildQuery("findServicesByPredicate.sparql", predicate);
		for (Map<String, String> binding: executeQuery(query)) {
			try {
				services.add(createService(binding));
			} catch (SADIException e) {
				log.error(String.format("error creating service from registry data %s", binding), e);
			}
		}
		return services;
	}
	
	public Collection<ServiceImpl> findServicesByInputClass(OntClass clazz) throws SADIException
	{
		return findServicesByInputClass(clazz, true);
	}

	public Collection<ServiceImpl> findServicesByInputClass(OntClass clazz, boolean withReasoning) throws SADIException
	{
		Collection<ServiceImpl> services = new ArrayList<ServiceImpl>();
		if (withReasoning) {
			services.addAll(findServicesByInputClass(clazz, false));
			for (Iterator<OntClass> superClasses = clazz.listSuperClasses(); superClasses.hasNext(); ) {
				services.addAll(findServicesByInputClass(superClasses.next(), false));
			}
		} else {
			if (!clazz.isURIResource())
				return services;
			String query = buildQuery("findServicesByInputClass.sparql", clazz.getURI());
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
	
	public Collection<ServiceImpl> findServicesByConnectedClass(OntClass clazz) throws SADIException
	{
		return findServicesByConnectedClass(clazz, true);
	}
	
	public Collection<ServiceImpl> findServicesByConnectedClass(OntClass clazz, boolean withReasoning) throws SADIException
	{
		Collection<ServiceImpl> services = new ArrayList<ServiceImpl>();
		if (withReasoning) {
			services.addAll(findServicesByConnectedClass(clazz, false));
			for (Iterator<OntClass> subClasses = clazz.listSubClasses(); subClasses.hasNext(); ) {
				services.addAll(findServicesByConnectedClass(subClasses.next(), false));
			}
		} else {
			if (!clazz.isURIResource())
				return services;
			String query = buildQuery("findServicesByConnectedClass.sparql", clazz.getURI());
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
	public Collection<ServiceImpl> findServicesByOutputClass(OntClass clazz)
	throws SADIException
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
	public Collection<ServiceImpl> findServicesByOutputClass(OntClass clazz, boolean withReasoning)
	throws SADIException
	{
		Collection<ServiceImpl> services = new ArrayList<ServiceImpl>();
		if (withReasoning) {
			services.addAll(findServicesByOutputClass(clazz, false));
			for (Iterator<OntClass> subClasses = clazz.listSubClasses(); subClasses.hasNext(); ) {
				services.addAll(findServicesByOutputClass(subClasses.next(), false));
			}
		} else {
			if (!clazz.isURIResource())
				return services;
			String query = buildQuery("findServicesByOutputClass.sparql", clazz.getURI());
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
	@Override
	public Collection<ServiceImpl> findServicesByInputInstance(Resource subject) throws SADIException
	{
		return findServices(subject, false);
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#findServices(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
     */
	@Override
	public Collection<ServiceImpl> findServices(Resource subject, String predicate) throws SADIException
	{
		return findServices(subject, predicate, false);
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#discoverServices(com.hp.hpl.jena.rdf.model.Model)
     */
	@Override
	public Collection<ServiceInputPair> discoverServices(Model inputModel) throws SADIException
	{
		Collection<ServiceInputPair> pairs = new ArrayList<ServiceInputPair>();
		for (ServiceImpl service: getAllServices()) {
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
	@Override
	public Collection<String> findPredicatesBySubject(Resource subject) throws SADIException
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
	public Collection<ServiceImpl> findServices(Resource subject, boolean direct) throws SADIException
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
	public Collection<ServiceImpl> findServices(Resource subject, String predicate, boolean direct) throws SADIException
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
	public Collection<String> findPredicatesBySubject(Resource subject, boolean direct) throws SADIException
	{
		Collection<String> predicates = new ArrayList<String>();
		for (ServiceImpl service: findServices(subject, direct)) {
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
	private Collection<ServiceImpl> filterServicesByInput(Collection<ServiceImpl> services, Resource input, boolean direct)
	{
		Collection<ServiceImpl> filteredServices = new ArrayList<ServiceImpl>(services.size());
		if (direct) {
			/* TODO if this is too slow, we can create an ontology model that
			 * contains only the direct properties of the input node...
			 */
			OntModel base = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF, input.getModel() );
			for (ServiceImpl service: services) {
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
			for (ServiceImpl service: services) {
				if (service.isInputInstance(input))
					filteredServices.add(service);
			}
		}
		return filteredServices;
	}

	public ServiceStatus getServiceStatus(String serviceURI) throws SADIException
	{
		throw new UnsupportedOperationException();
	}
	
	public String toString()
	{
		return backend.toString();
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.RegistryBase#getLog()
	 */
	@Override
	protected Logger getLog()
	{
		return log;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.RegistryBase#createService(java.lang.String)
	 */
	@Override
	public ServiceImpl createService(String serviceURI) throws SADIException
	{
		String query = buildQuery("getService.sparql", serviceURI);
		List<Map<String, String>> bindings = executeQuery(query);
		if (bindings.isEmpty()) {
			throw new SADIException(String.format("no such service %s in this registry", serviceURI));
		} else if (bindings.size() > 1) {
			throw new SADIException(String.format("URI %s maps to more than one service in this registry", serviceURI));
		}
		return createService(bindings.get(0));
	}

	/**
	 * Create a Service object from the details in the supplied map.
	 * This map must contain values for all the required service fields.
	 * In practice, the map will be a variable binding for an appropriate 
	 * SPARQL query on the registry endpoint, which allows the registry to 
	 * return an immediately useful Service object without having to fetch 
	 * the full definition every time.
	 * @param binding
	 * @return 
	 * @throws SADIException
	 */
	private ServiceImpl createService(Map<String, String> binding) throws SADIException
	{
		return new ServiceImpl(binding);
	}
}
