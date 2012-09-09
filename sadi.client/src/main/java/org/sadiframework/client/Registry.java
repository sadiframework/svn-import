package org.sadiframework.client;

import java.io.IOException;
import java.util.Collection;

import org.sadiframework.SADIException;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * The main Registry interface.
 * @author Luke McCarthy
 */
public interface Registry
{
	/**
	 * Returns the Service corresponding to the specified service URI.
	 * The object returned will be populated using the service definition 
	 * cached in the registry; to avoid this, use 
	 * <code>new ServiceImpl(serviceURI)</code> instead.
	 * @param serviceURI the service URI
	 * @return a Service object corresponding to the specified service URI
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public Service getService(String serviceURI) throws SADIException;
	
	/**
	 * Returns all services in the registry.
	 * @return the complete collection of services registered in the registry
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public Collection<? extends Service> getAllServices() throws SADIException;

	/**
	 * Returns a subset of services in this registry.
	 * Return at most the specified number of services starting at the
	 * specified offset (services will be sorted alphabetically by name).
	 * @param limit maximum number of services to return in this query
	 * @param offset return services starting at this position
	 * @return a subset of services registered in the registry
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public Collection<Service> getAllServices(int limit, int offset) throws SADIException;
	
	/**
	 * Returns a subset of services in this registry.
	 * Return at most the specified number of services starting at the
	 * specified offset (services will be sorted as specified).
	 * @param limit maximum number of services to return in this query
	 * @param offset return services starting at this position
	 * @param sort a SortKey indicating how to order the services
	 * @return a subset of services registered in the registry
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public Collection<Service> getAllServices(int limit, int offset, SortKey sort) throws SADIException;
	
//	/**
//	 * Returns a subset of services in this registry.
//	 * Return at most the specified number of services starting at the
//	 * specified offset (services will be sorted alphabetically by name).
//	 * @param limit maximum number of services to return in this query
//	 * @param offset return services starting at this position
//	 * @return a subset of services registered in the registry
//	 * @throws SADIException if there is a problem communicating with the registry
//	 */
//	public Collection<? extends Service> getAllServices(int limit, int offset) throws SADIException;
	
	/**
	 * Returns the last known status of the service at the specified URI,
	 * or null if the service isn't registered.
	 * @param serviceURI the service URI
	 * @return the service status (see {@link ca.wilkinsonalb.sadi.client.ServiceStatus})
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public ServiceStatus getServiceStatus(String serviceURI) throws SADIException;
	
	/**
	 * Returns a collection of Services that can attach the specified Property.
	 * If the Property is an OntProperty, Services that can attach sub-properties
	 * will be returned as well.
	 * @param property the property
	 * @return a collection of matching services
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public Collection<? extends Service> findServicesByAttachedProperty(Property property) throws SADIException;
	
	/**
	 * 
	 * @param clazz the input class
	 * @return a collection of matching services
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public Collection<? extends Service> findServicesByInputClass(Resource clazz) throws SADIException;
	
	/**
	 * 
	 * @param clazz the connected class
	 * @return a collection of matching services
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public Collection<? extends Service> findServicesByConnectedClass(Resource clazz) throws SADIException;
	
	/**
	 * Returns a collection of services that can attach the specified predicate
	 * to the specified subject.
	 * @param subject the subject node
	 * @param predicate the predicate URI
	 * @return the collection of matching services
	 * @throws IOException if there is a problem communicating with the registry
	 * @deprecated Use {@link #findServices(RegistrySearchCriteria)} instead
	 */
	public Collection<? extends Service> findServices(Resource subject, String predicate) throws SADIException;
	
	/**
	 * Returns a collection of Services that can attach the specified Property.
	 * If the Property is an OntProperty, Services that can attach sub-properties
	 * will be returned as well.
	 * @param property the property
	 * @return a collection of matching services
	 * @throws IOException if there is a problem communicating with the registry
	 * @deprecated Use {@link #findServicesByAttachedProperty(Property)} instead
	 */
	public Collection<? extends Service> findServicesByPredicate(String predicate) throws SADIException;
	
	/**
	 * Returns a collection of predicates that are mapped to services that
	 * can take the specified subject as input.
	 * @param subject the subject URI
	 * @return a collection of matching predicates
	 * @throws IOException if there is a problem communicating with the registry
	 * @deprecated Use {@link #findAttachedProperties(RegistrySearchCriteria)} instead
	 */
	public Collection<String> findPredicatesBySubject(Resource subject) throws SADIException;
	
	/**
	 * Returns a collection of services that can attach properties to the
	 * specified subject.
	 * This process involves a great deal of reasoning and can take a very
	 * long time.
	 * @param subject the subject node
	 * @return a collection of matching services
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public Collection<? extends Service> discoverServices(Resource subject) throws SADIException;
	
	/**
	 * Returns a collection of service/input pairs discovered by searching the
	 * specified input model for instances of the classes consumed by services
	 * in the registry.
	 * This process involves a great deal of reasoning and can take a very
	 * long time.
	 * @param model the input model
	 * @return the collection of service/input pairs
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public Collection<? extends ServiceInputPair> discoverServices(Model model) throws SADIException;
	
	/**
	 * Returns a collection of Service objects that match the specified 
	 * RegistrySearchCriteria.
	 * @param criteria the search criteria
	 * @return a collection of Service objects that match the specified RegistrySearchCriteria
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public Collection<? extends Service> findServices(RegistrySearchCriteria criteria) throws SADIException;
	
	/**
	 * Returns a collection of Properties that are attached by services that
	 * match the specified RegistrySearchCriteria.
	 * @param criteria the search criteria
	 * @return a collection of Properties that are attached by services that match the specified RegistrySearchCriteria
	 * @throws SADIException if there is a problem communicating with the registry
	 */
	public Collection<Property> findAttachedProperties(RegistrySearchCriteria criteria) throws SADIException;
	
	/**
	 * 
	 * @author Luke McCarthy
	 */
	public enum SortKey
	{
		NAME("name", "?name"),
		NAME_REVERSE("name_r", "DESC(?name)"),
		URI("uri", "?serviceURI"),
		URI_REVERSE("uri_r", "DESC(?serviceURI)"),
		INPUT_CLASS("input", "inputClassURI"),
		INPUT_CLASS_REVERSE("input_r", "DESC(?inputClassURI)"),
		OUTPUT_CLASS("output", "inputClassURI"),
		OUTPUT_CLASS_REVERSE("output_r", "DESC(?outputClassURI)");
		
		private final String label;
		private final String clause;
		
		private SortKey(String label, String clause)
		{
			this.label = label;
			this.clause = clause;
		}
		
		public String getLabel()
		{
			return label;
		}

		public String getClause()
		{
			return clause;
		}
		
		public static SortKey getSortKeyByLabel(String label)
		{
			for (SortKey sort: SortKey.values()) {
				if (sort.label.equals(label))
					return sort;
			}
			return null;
		}
		
		public static SortKey getDefaultSortKey()
		{
			return NAME;
		}
	}
}
