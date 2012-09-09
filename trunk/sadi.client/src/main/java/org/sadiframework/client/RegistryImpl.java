package org.sadiframework.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.sadiframework.SADIException;
import org.sadiframework.beans.RestrictionBean;
import org.sadiframework.utils.OwlUtils;
import org.sadiframework.utils.QueryExecutor;


import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * A registry of SADI services.
 * @author Luke McCarthy
 */
public class RegistryImpl extends RegistryBase
{
	private static final Logger log = Logger.getLogger(RegistryImpl.class);
	
	private Model model;
	
	/* (non-Javadoc)
	 * @see org.sadiframework.client.RegistryBase#(java.lang.String)
	 */
	public RegistryImpl(Configuration config)
	{
		super(config);
		
		model = ModelFactory.createDefaultModel();
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.client.RegistryBase#(org.sadiframework.utils.QueryExecutor)
	 */
	public RegistryImpl(QueryExecutor backend)
	{
		super(backend);
		
		model = ModelFactory.createDefaultModel();
	}

	/* (non-Javadoc)
	 * @see org.sadiframework.client.Registry#getAllServices()
	 */
	@Override
	public Collection<Service> getAllServices() throws SADIException
	{
		return getServicesByQuery("getAllServices.sparql");
	}

	/* (non-Javadoc)
	 * @see org.sadiframework.client.Registry#getAllServices(int, int)
	 */
	@Override
	public Collection<Service> getAllServices(int limit, int offset) throws SADIException
	{
		return getAllServices(limit, offset, null);
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.client.Registry#getAllServices(int, int, org.sadiframework.client.Registry.SortKey)
	 */
	@Override
	public Collection<Service> getAllServices(int limit, int offset, SortKey sort) throws SADIException
	{
		if (sort == null)
			sort = SortKey.getDefaultSortKey();
		return getServicesByQuery("getAllServicesLimitOffset.sparql",
				 sort.getClause(), String.valueOf(limit), String.valueOf(offset));
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.client.Registry#getServiceStatus(java.lang.String)
	 */
	@Override
	public ServiceStatus getServiceStatus(String serviceURI) throws SADIException
	{
		/* this query should order by the date the status was reported,
		 * so the most recent status will be first; if there are results,
		 * but no status, the service is registered and probably ok; if 
		 * there are no results, the service isn't registered.
		 */
		String query = buildQuery("getServiceStatus.sparql", serviceURI, serviceURI);
		List<Map<String, String>> bindings = executeQuery(query);
		if (bindings.isEmpty()) {
			return null;
		} else {
			Map<String, String> binding = bindings.get(0);
			String status = binding.get("status");
			if (status == null)
				return ServiceStatus.OK;
			else
				return ServiceStatus.valueOf(status);
		}
	}

	/* (non-Javadoc)
     * @see org.sadiframework.client.Registry#findServices(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
     */
	@Override
	public Collection<Service> findServices(Resource subject, String predicate) throws SADIException
	{
		return findServices(subject, predicate, false);
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.client.Registry#findServicesByPredicate(java.lang.String)
	 */
	@Override
	public Collection<Service> findServicesByPredicate(String predicate) throws SADIException
	{
		return getServicesByQuery("findServicesByPredicate.sparql", predicate);
	}

	/* (non-Javadoc)
     * @see org.sadiframework.client.Registry#findPredicatesBySubject(com.hp.hpl.jena.rdf.model.Resource)
     */
	@Override
	public Collection<String> findPredicatesBySubject(Resource subject) throws SADIException
	{
		return findPredicatesBySubject(subject, false);
	}
	
	/* (non-Javadoc)
     * @see org.sadiframework.client.Registry#discoverServices(com.hp.hpl.jena.rdf.model.Resource)
     */
	@Override
	public Collection<Service> discoverServices(Resource subject) throws SADIException
	{
		return findServices(subject, false);
	}
	
	/* (non-Javadoc)
     * @see org.sadiframework.client.Registry#discoverServices(com.hp.hpl.jena.rdf.model.Model)
     */
	@Override
	public Collection<ServiceInputPair> discoverServices(Model inputModel) throws SADIException
	{
		Collection<ServiceInputPair> pairs = new ArrayList<ServiceInputPair>();
		for (Service service: getAllServices()) {
			try {
				Collection<Resource> inputInstances = service.discoverInputInstances(inputModel);
				if (!inputInstances.isEmpty()) {
					pairs.add(new ServiceInputPair(service, inputInstances));
				}
			} catch (Exception e) {
				log.error(String.format("error finding input instances for %s", service), e);
			}
		}
		return pairs;
	}

	/* (non-Javadoc)
	 * @see org.sadiframework.client.Registry#findServices(org.sadiframework.client.RegistrySearchCriteria)
	 */
	@Override
	public Collection<? extends Service> findServices(RegistrySearchCriteria criteria) throws SADIException
	{
		// FIXME
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.sadiframework.client.Registry#findAttachedProperties(org.sadiframework.client.RegistrySearchCriteria)
	 */
	@Override
	public Collection<Property> findAttachedProperties(RegistrySearchCriteria criteria) throws SADIException
	{
		// FIXME
		throw new UnsupportedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return backend.toString();
	}
	
	/**
	 * Returns a collection of services that can consume the specified input
	 * data, optionally considering only the direct types of the input node.
	 * @param subject the input data
	 * @param direct if true, consider only the direct types of the input node;
	 *               if false, consider inferred types as well
	 * @return the collection of matching services
	 * @deprecated
	 */
	public Collection<Service> findServices(Resource subject, boolean direct) throws SADIException
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
	 * @deprecated
	 */
	public Collection<Service> findServices(Resource subject, String predicate, boolean direct) throws SADIException
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
	 * @deprecated
	 */
	public Collection<String> findPredicatesBySubject(Resource subject, boolean direct) throws SADIException
	{
		Collection<String> predicates = new ArrayList<String>();
		for (Service service: findServices(subject, direct)) {
			for (RestrictionBean restriction: service.getRestrictionBeans())
					predicates.add(restriction.getOnPropertyURI());
		}
		return predicates;
	}
	
	/**
	 * 
	 * @return
	 * @throws SADIException
	 * @deprecated Use {@link #findAttachedProperties(RegistrySearchCriteria)} instead
	 */
	public Collection<String> listPredicates() throws SADIException
	{
		Collection<String> predicates = new ArrayList<String>();
		String query = buildQuery("listPredicates.sparql");
		for (Map<String, String> binding: executeQuery(query)) {
			predicates.add(binding.get("p"));
		}
		return predicates;
	}

	/**
	 * 
	 * @return
	 * @throws SADIException
	 * @deprecated Use {@link #findAttachedProperties(RegistrySearchCriteria)} instead
	 */
	public Collection<Property> listPredicatesByInputClass(String inputClassURI) throws SADIException
	{
		Collection<Property> properties = new HashSet<Property>();
		String query = buildQuery("listPredicatesByInputClass.sparql", inputClassURI);
		for (Map<String, String> binding: executeQuery(query)) {
			Property p = model.getProperty(binding.get("p"));
			if (!p.hasProperty(RDFS.label)) {
				String label = binding.get("label");
				if (label != null)
					p.addLiteral(RDFS.label, label);
				else
					p.addLiteral(RDFS.label, p.getLocalName());
			}
			properties.add(p);
		}
		return properties;
	}
	
	public Collection<Service> findServicesByAttachedPropertyLabel(String propertyLabel) throws SADIException
	{
		return getServicesByQuery("findServicesByAttachedPropertyLabel.sparql", propertyLabel);
	}
	
	public Collection<Service> findServicesByConnectedClassLabel(String classLabel) throws SADIException
	{
		return getServicesByQuery("findServicesByConnectedClassLabel.sparql", classLabel);
	}
	
	private Collection<Service> getServicesByQuery(String template, String ... args) throws SADIException
	{
		Collection<Service> services = new ArrayList<Service>();
		String query = buildQuery(template, args);
		for (Map<String, String> binding: executeQuery(query)) {
			services.add(createService(binding));
		}
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
	 * @throws SADIException
	 */
	private Collection<Service> filterServicesByInput(Collection<Service> services, Resource input, boolean direct) throws SADIException
	{
		Collection<Service> filteredServices = new ArrayList<Service>(services.size());
		if (direct) {
			/* TODO if this is too slow, we can create an ontology model that
			 * contains only the direct properties of the input node...
			 */
			OntModel base = ModelFactory.createOntologyModel( OwlUtils.getDefaultReasonerSpec(), input.getModel() );
			for (Service service: services) {
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
			for (Service service: services) {
				if (service.isInputInstance(input))
					filteredServices.add(service);
			}
		}
		return filteredServices;
	}

	/* (non-Javadoc)
	 * @see org.sadiframework.client.RegistryBase#getLog()
	 */
	@Override
	protected Logger getLog()
	{
		return log;
	}

	/* (non-Javadoc)
	 * @see org.sadiframework.client.RegistryBase#createService(java.lang.String)
	 */
	@Override
	protected ServiceImpl createService(String serviceURI) throws SADIException
	{
		// FIXME use consruct and ServiceOntologyHelper...
		String query = buildQuery("getService.sparql", serviceURI);
		List<Map<String, String>> bindings = executeQuery(query);
		if (bindings.isEmpty()) {
			throw new SADIException(String.format("no such service %s in this registry", serviceURI));
		} else if (bindings.size() > 1) {
			throw new SADIException(String.format("URI %s maps to more than one service in this registry", serviceURI));
		}
		return createService(bindings.get(0));
	}

	@Override
	protected Collection<? extends Service> findServicesByAttachedProperty(Iterable<String> propertyURIs) throws SADIException
	{
		Collection<Service> services = new ArrayList<Service>();
		for (String p: propertyURIs) {
			services.addAll(findServicesByPredicate(p));
		}
		return services;
	}

	@Override
	protected Collection<? extends Service> findServicesByConnectedClass(Iterable<String> classURIs) throws SADIException
	{
		Collection<Service> services = new ArrayList<Service>();
		for (String c: classURIs) {
			services.addAll(getServicesByQuery("findServicesByConnectedClass.sparql", c));
		}
		return services;
	}

	@Override
	protected Collection<? extends Service> findServicesByInputClass(Iterable<String> classURIs) throws SADIException
	{
		Collection<Service> services = new ArrayList<Service>();
		for (String c: classURIs) {
			services.addAll(getServicesByQuery("findServicesByInputClass.sparql", c));
		}
		return services;
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
		ServiceImpl service = new ServiceImpl();
		service.setURI(binding.get("serviceURI"));
		service.setName(binding.get("name"));
		service.setDescription(binding.get("description"));
		service.setInputClassURI(binding.get("inputClassURI"));
		service.setInputClassLabel(binding.get("inputClassLabel"));
		service.setOutputClassURI(binding.get("outputClassURI"));
		service.setOutputClassLabel(binding.get("outputClassLabel"));
		addRestrictions(service);
		return service;
	}
	
	private void addRestrictions(ServiceImpl service) throws SADIException
	{
		String query = buildQuery("getRestrictions.sparql", service.getURI());
		for (Map<String, String> binding: executeQuery(query)) {
			RestrictionBean restriction = new RestrictionBean();
			restriction.setOnPropertyURI(binding.get("onPropertyURI"));
			restriction.setOnPropertyLabel(binding.get("onPropertyLabel"));
			restriction.setValuesFromURI(binding.get("valuesFromURI"));
			restriction.setValuesFromLabel(binding.get("valuesFromLabel"));
			if (!service.getRestrictionBeans().contains(restriction))
				service.getRestrictionBeans().add(restriction);
		}
	}
	
//	private String createQuery(RegistrySearchCriteria criteria)
//	{
//		StringBuilder query = new StringBuilder();
//		query.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
//		query.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
//		query.append("PREFIX owl: <http://www.w3.org/2002/07/owl#>\n");
//		query.append("PREFIX sadi: <http://sadiframework.org/ontologies/sadi.owl#>\n");
//		query.append("PREFIX mygrid: <http://www.mygrid.org.uk/mygrid-moby-service#>\n");
//		query.append("SELECT *\n");
//		query.append("WHERE {\n");
//		query.append("    ?serviceURI mygrid:hasOperation ?op .\n");
//		query.append("    ?serviceURI mygrid:hasServiceNameText ?name .\n");
//		query.append("    ?serviceURI mygrid:hasServiceDescriptionText ?description .\n");
//		query.append("    ?op mygrid:inputParameter ?input .\n");
//		query.append("    ?input mygrid:objectType ?inputClassURI .\n");
//		query.append("    ?op mygrid:outputParameter ?output .\n");
//		query.append("    ?output mygrid:objectType ?outputClassURI .\n");
//		query.append("    OPTIONAL {\n");
//		if (criteria.getTarget().equals(Service.class)) {
//			query.append();
//		} else if (criteria.getTarget().equals(Property.class)) {
//			
//		}
//	}
}
