package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;

import ca.wilkinsonlab.sadi.client.Registry;

/**
 * A registry that holds SPARQL endpoints.
 * 
 * This is a "tagging" interface; it exists only because 
 * I needed to distinguish between SPARQL and non-SPARQL 
 * registries. For example, the RandomQueryGenerator and 
 * PredicateStatsDB classes only work on SPARQL registries.
 * -- BV   
 */

public interface SPARQLRegistry extends Registry 
{
	public long getNumTriples(String endpointURI) throws IOException;
	
	public boolean hasPredicate(String predicateURI) throws IOException;
	
	public List<SPARQLEndpoint> getAllEndpoints() throws IOException;
	public Collection<SPARQLEndpoint> findEndpointsByPredicate(String predicate) throws IOException;
	public Collection<SPARQLEndpoint> findEndpointsByTriplePattern(Triple triplePattern) throws IOException;

	public Collection<String> getPredicatesForEndpoint(String endpointURI) throws IOException;
	public Collection<String> getAllPredicates() throws IOException;

	public boolean subjectMatchesRegEx(String endpointURI, String uri) throws IOException;
	public boolean objectMatchesRegEx(String endpointURI, String uri) throws IOException;
	
	public String getSubjectRegEx(String endpointURI) throws IOException;
	public String getObjectRegEx(String endpointURI) throws IOException;
	
	public long getNumTriplesLowerBound(String endpointURI) throws IOException;
	public long getNumTriplesOrLowerBound(String endpointURI) throws IOException;

}