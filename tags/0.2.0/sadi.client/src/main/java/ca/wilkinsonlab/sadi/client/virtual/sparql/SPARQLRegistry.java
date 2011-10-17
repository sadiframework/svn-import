package ca.wilkinsonlab.sadi.client.virtual.sparql;

import java.util.Collection;

import com.hp.hpl.jena.graph.Triple;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.Registry;

/**
 * A registry that holds SPARQL endpoints.  
 */

public interface SPARQLRegistry extends Registry 
{
	public Collection<SPARQLEndpoint> getAllSPARQLEndpoints() throws SADIException;
	public SPARQLEndpoint getSPARQLEndpoint(String uri) throws SADIException;

	public Collection<SPARQLEndpoint> findSPARQLEndpointsByTriplePattern(Triple triplePattern) throws SADIException;

	public boolean subjectMatchesRegEx(String endpointURI, String uri) throws SADIException;
	public boolean objectMatchesRegEx(String endpointURI, String uri) throws SADIException;
}