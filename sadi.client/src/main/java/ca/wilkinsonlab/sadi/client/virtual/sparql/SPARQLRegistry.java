package ca.wilkinsonlab.sadi.client.virtual.sparql;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;

import ca.wilkinsonlab.sadi.client.Registry;

/**
 * A registry that holds SPARQL endpoints.  
 */

public interface SPARQLRegistry extends Registry 
{
	public List<SPARQLEndpoint> getAllEndpoints() throws IOException;
	public SPARQLEndpoint getEndpoint(String uri) throws IOException;

	public Collection<SPARQLEndpoint> findEndpointsByTriplePattern(Triple triplePattern) throws IOException;

	public boolean subjectMatchesRegEx(String endpointURI, String uri) throws IOException;
	public boolean objectMatchesRegEx(String endpointURI, String uri) throws IOException;
}