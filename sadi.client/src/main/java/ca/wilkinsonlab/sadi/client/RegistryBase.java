package ca.wilkinsonlab.sadi.client;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.utils.QueryExecutor;
import ca.wilkinsonlab.sadi.utils.QueryExecutorFactory;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.ModelFactory;

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
		try {
			return SPARQLStringUtils.strFromTemplate(
				SPARQLStringUtils.readFully(getClass().getResource(template)),
				args
			);
		} catch (IOException e) {
			throw new SADIException(e.toString());
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
	
	@Override
	public Collection<? extends Service> findServicesByInputClass(OntClass clazz) throws SADIException
	{
		return findServicesByInputClass(clazz, true);
	}
	
	@Override
	public Collection<? extends Service> findServicesByConnectedClass(OntClass clazz) throws SADIException
	{
		return findServicesByConnectedClass(clazz, true);
	}
	
	/**
	 * Returns the log4j Logger associated with the concrete registry.
	 * @return the log4j Logger associated with the concrete registry
	 */
	protected abstract Logger getLog();
	
	/**
	 * Creates a new service instance corresponding the specified URI.
	 * @param serviceURI
	 * @return a new service instance corresponding the specified URI
	 */
	protected abstract Service createService(String serviceURI) throws SADIException;
}
