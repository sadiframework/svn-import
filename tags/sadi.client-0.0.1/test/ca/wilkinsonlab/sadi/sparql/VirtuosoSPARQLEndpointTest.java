package ca.wilkinsonlab.sadi.sparql;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.sparql.SPARQLEndpointFactory;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint.EndpointType;
import junit.framework.TestCase;

public class VirtuosoSPARQLEndpointTest extends TestCase
{
	public final static Log log = LogFactory.getLog(VirtuosoSPARQLEndpointTest.class);
	
	public final static String EXAMPLE_ENDPOINT1_URI = "http://omim.bio2rdf.org/sparql";
	public final static EndpointType EXAMPLE_ENDPOINT1_TYPE = EndpointType.VIRTUOSO;
	public final static String EXAMPLE_ENDPOINT2_URI = "http://pubmed.bio2rdf.org/sparql";
	public final static EndpointType EXAMPLE_ENDPOINT2_TYPE = EndpointType.VIRTUOSO;

	SPARQLEndpoint endpoint;
	
	protected void setUp() throws Exception {
		super.setUp();
		endpoint = SPARQLEndpointFactory.createEndpoint(EXAMPLE_ENDPOINT1_URI, EXAMPLE_ENDPOINT1_TYPE);		
	}

	public void testGetNamedGraphs() 
	{
		try {
			assertTrue(endpoint.getNamedGraphs().size() > 0);
		}
		catch(Exception e) {
			fail("FAILED to get named graphs from endpoint: " + e);
		}
	}

	public void testGetPredicates() 
	{
		try {
			assertTrue(endpoint.getPredicates().size() > 0);
		}
		catch(Exception e) {
			fail("FAILED to get predicates from endpoint: " + e);
		}
	}
	
	public void testGetPredicatesWithGraphURI() 
	{
		try {
			Set<String> graphURIs = endpoint.getNamedGraphs();
			assertTrue(graphURIs.size() > 0);
			String firstGraphURI = graphURIs.iterator().next();
			assertTrue(endpoint.getPredicates(firstGraphURI).size() > 0);
		}
		catch(Exception e) {
			fail("FAILED to get predicates from named graph: " + e);
		}
	}
	
	public void testPredicatesPartial() 
	{
		try {
			assertTrue(endpoint.getPredicatesPartial().size() > 0);
		}
		catch(Exception e) {
			fail("FAILED to get partial list of predicates from endpoint: " + e);
		}
	}
	
	public void testIsDatatypeProperty() 
	{
		try {
			Set<String> predicateURIs = endpoint.getPredicates();
			assertTrue(predicateURIs.size() > 0);
			String firstPredicateURI = predicateURIs.iterator().next();
			// This test just ensures that isDatatypeProperty() runs without throwing an exception.
			endpoint.isDatatypeProperty(firstPredicateURI);
		}
		catch(Exception e) {
			fail("FAILED to get property type for predicate: " + e);
		}
	}
	
	public void testGetResultsCountLowerBound()
	{
		try {
			SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(EXAMPLE_ENDPOINT2_URI, EXAMPLE_ENDPOINT2_TYPE);
			endpoint.getResultsCountLowerBound("SELECT * WHERE { ?s ?p ?o }", 1);
		}
		catch(Exception e) {
			fail("FAILED to get a lower bound on number of query results from " + EXAMPLE_ENDPOINT2_URI + ": " + e);
		}
	}

}
