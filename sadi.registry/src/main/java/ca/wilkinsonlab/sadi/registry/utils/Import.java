package ca.wilkinsonlab.sadi.registry.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.registry.Registry;
import ca.wilkinsonlab.sadi.utils.QueryExecutor;
import ca.wilkinsonlab.sadi.utils.QueryExecutorFactory;

import com.hp.hpl.jena.util.LocationMapper;

/**
 * Register all of the services listed in the old registry.
 * @author Luke McCarthy
 */
public class Import
{
	private static Logger log = Logger.getLogger(Import.class);
	
	public static void main(String[] args) throws Exception
	{
		String uriPrefix = "http://sadiframework.org/examples/";
		String altPrefix = "http://localhost:8080/sadi-examples/";
		log.info( String.format("mapping URI prefix %s to %s", uriPrefix, altPrefix) );
		LocationMapper.get().addAltPrefix(uriPrefix, altPrefix);
		
		OldRegistry oldRegistry = new OldRegistry();
		Registry newRegistry = Registry.getRegistry();
		for (String serviceURI: oldRegistry.getServiceURIs()) {
			log.info( String.format("found service %s", serviceURI) );
			try {
				newRegistry.registerService(serviceURI);
			} catch (SADIException e) {
				log.error(String.format("error registering service %s", serviceURI), e);
			}
		}
	}
	
	private static class OldRegistry
	{
		QueryExecutor backend;
		
		public OldRegistry() throws IOException
		{
			backend = QueryExecutorFactory.createVirtuosoSPARQLEndpointQueryExecutor("http://biordf.net/sparql", "http://sadiframework.org/registry");
		}
		
		public Collection<String> getServiceURIs() throws SADIException
		{
			Collection<String> serviceURIs = new ArrayList<String>();
			String query = 
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"PREFIX sadi: <http://sadiframework.org/ontologies/sadi.owl#> " +
				"SELECT ?service " +
				"WHERE { " +
					"?service rdf:type sadi:Service " +
				"}";
			try {
				for (Map<String, String> binding: backend.executeQuery(query)) {
					String serviceURI = binding.get("service");
					if (serviceURI != null)
						serviceURIs.add(serviceURI);
					else
						log.error( String.format("query binding had no value for service variable:\n%s", binding) );
				}
			} catch (SADIException e) {
				log.error("error querying old registry", e);
			}
			return serviceURIs;
		}
	}
}
