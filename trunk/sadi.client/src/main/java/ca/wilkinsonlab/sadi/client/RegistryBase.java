package ca.wilkinsonlab.sadi.client;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.utils.QueryExecutor;
import ca.wilkinsonlab.sadi.utils.QueryExecutorFactory;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Luke McCarthy
 */
public abstract class RegistryBase implements Registry
{
	protected static final String DRIVER_KEY = "driver";
	protected static final String SPARQL_ENDPOINT_KEY = "endpoint";
	protected static final String SPARQL_GRAPH_KEY = "graph";
	protected static final String MYSQL_DSN_KEY = "dsn";
	protected static final String MYSQL_USERNAME_KEY = "username";
	protected static final String MYSQL_PASSWORD_KEY = "password";
	
	protected QueryExecutor backend;
	protected Map<String, Service> serviceCache;
	
	/**
	 * Construct a new SADI registry from the specified configuration.
	 * @param config the configuration
	 * @throws IOException if there is an error contacting the registry
	 */
	protected RegistryBase(Configuration config) throws IOException
	{
		String endpointURL = config.getString(SPARQL_ENDPOINT_KEY);
		String graphName = config.getString(SPARQL_GRAPH_KEY);
		String dsn = config.getString(MYSQL_DSN_KEY);
		String file = config.getString("file");
		if (endpointURL != null) {
			getLog().info(String.format("creating Virtuoso-backed registry model from %s(%s)", endpointURL, graphName));
			backend = QueryExecutorFactory.createVirtuosoSPARQLEndpointQueryExecutor(endpointURL, graphName);
		} else if (dsn != null) {
			getLog().info(String.format("creating JDBC-backed registry model from %s", dsn));
			backend = QueryExecutorFactory.createJDBCJenaModelQueryExecutor(config.getString(DRIVER_KEY), dsn, config.getString(MYSQL_USERNAME_KEY), config.getString(MYSQL_PASSWORD_KEY), graphName);
		} else if (file != null) {
			getLog().info(String.format("creating file-backed registry model from %s", file));
			backend = QueryExecutorFactory.createFileModelQueryExecutor(file);
		} else {
			getLog().warn("no configuration found; creating transient registry model");
			/* TODO this file isn't automatically reread when updated;
			 * maybe reopen models for each query?
			 */
			backend = QueryExecutorFactory.createJenaModelQueryExecutor(ModelFactory.createDefaultModel());
		}
		
		serviceCache = new HashMap<String, Service>();
	}
	
	/**
	 * Construct a new SADI registry from the specified back end.
	 * Used by the unit tests.
	 * @param backend
	 */
	protected RegistryBase(QueryExecutor backend)
	{
		this.backend = backend;
	}
	
	/**
	 * Returns true if the registry caches service instances by URI.
	 * @return true if the registry caches service instances by URI
	 */
	protected boolean isCacheEnabled()
	{
		return serviceCache != null;
	}
	
	/**
	 * 
	 * @param template
	 * @param args
	 * @return
	 * @throws SADIException
	 */
	protected String buildQuery(String template, String ... args) throws SADIException
	{
		String query = "";
		try {
			query = SPARQLStringUtils.readFully(getClass().getResource(template));
		} catch (IOException e) {
			throw new SADIException(e.toString());
		}
		if (args.length > 0) {
			return SPARQLStringUtils.strFromTemplate(query, args);
		} else {
			return query;
		}
	}
	
	/**
	 * Returns a list of variable bindings satisfying a SPARQL query.
	 * Just a pass-through to the back end because I'm lazy.
	 * @param query the SPARQL query
	 * @return a list of variable bindings satisfying the SPARQL query
	 * @throws SADIException
	 */
	protected List<Map<String, String>> executeQuery(String query) throws SADIException
	{
		return backend.executeQuery(query);
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Registry#getService(java.lang.String)
     */
	public Service getService(String serviceURI) throws SADIException
	{
		/* TODO replace this caching mechanism with something more
		 * sophisticated (probably ehCache...)
		 */
		if (isCacheEnabled()) {
			Service service = serviceCache.get(serviceURI);
			if (service == null) {
				service = createService(serviceURI);
				serviceCache.put(serviceURI, service);
			}
			return service;
		} else {
			return createService(serviceURI);
		}
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#findServicesByAttachedProperty(com.hp.hpl.jena.rdf.model.Property)
	 */
	@Override
	public Collection<? extends Service> findServicesByAttachedProperty(Property property) throws SADIException
	{
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
		return findServicesByAttachedProperty(propertyURIs);
		// TODO
//		return findServices(RegistrySearchCriteria.findService().addAttachedProperty(property));
	}
	
	@Override
	public Collection<? extends Service> findServicesByInputClass(Resource clazz) throws SADIException
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
		return findServicesByInputClass(classURIs);
		// TODO
//		return findServices(RegistrySearchCriteria.findService().addInputClass(clazz));
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#findServicesByConnectedClass(com.hp.hpl.jena.rdf.model.Resource)
	 */
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
		return findServicesByConnectedClass(classURIs);
		// TODO
//		return findServices(RegistrySearchCriteria.findService().addConnectedClass(clazz));
	}
	
	/**
	 * Creates a new service instance corresponding the specified URI.
	 * @param serviceURI
	 * @return a new service instance corresponding the specified URI
	 */
	protected abstract Service createService(String serviceURI) throws SADIException;
	
	/**
	 * Returns the log4j Logger associated with the concrete registry.
	 * @return the log4j Logger associated with the concrete registry
	 */
	protected abstract Logger getLog();

	protected abstract Collection<? extends Service> findServicesByAttachedProperty(Iterable<String> propertyURIs) throws SADIException;
	protected abstract Collection<? extends Service> findServicesByConnectedClass(Iterable<String> classURIs) throws SADIException;
	protected abstract Collection<? extends Service> findServicesByInputClass(Iterable<String> classURIs) throws SADIException;
}
