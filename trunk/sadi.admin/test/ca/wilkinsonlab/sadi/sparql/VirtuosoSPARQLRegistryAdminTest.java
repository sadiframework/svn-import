package ca.wilkinsonlab.sadi.sparql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Triple;

import junit.framework.TestCase;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

public class VirtuosoSPARQLRegistryAdminTest extends TestCase {

	public final static Log LOGGER = LogFactory.getLog(VirtuosoSPARQLRegistryAdminTest.class);
	public final static String EXAMPLE_ENDPOINT1_URI = "http://omim.bio2rdf.org/sparql";
	public final static SPARQLEndpointType EXAMPLE_ENDPOINT1_TYPE = SPARQLEndpointType.VIRTUOSO;
	public final static String EXAMPLE_ENDPOINT2_URI = "http://kegg.bio2rdf.org/sparql";
	public final static SPARQLEndpointType EXAMPLE_ENDPOINT2_TYPE = SPARQLEndpointType.VIRTUOSO;
	
	VirtuosoSPARQLRegistryAdmin registry;
	
	public void setUp() {
		try {
			// Best to run these tests on a local copy of the registry.
			registry = new VirtuosoSPARQLRegistryAdmin("http://localhost:8890/sparql");
		}
		catch(Exception e) {
			fail("Failed to initialize registry: " + e);
		}
	}
	
	public void testClearRegistry()
	{
		try {
			registry.clearRegistry();
		} 
		catch(Exception e) {
			fail("Failed to clear registry: " + e);
		}
		assertTrue(graphIsEmpty(registry.getIndexGraphURI()));
		assertTrue(graphIsEmpty(registry.getOntologyGraphURI()));
	}
	
	public void testClearIndexes() 
	{
		try {
			registry.clearIndexes();
		}
		catch(Exception e) {
			fail("Failed to clear indexes: " + e);
		}
		assertTrue(graphIsEmpty(registry.getIndexGraphURI()));
	}
	
	private boolean graphIsEmpty(String graphURI)
	{
		String query = "SELECT * FROM %u% WHERE { ?s ?p ?o } LIMIT 1";
		query = strFromTemplate(query, graphURI);
		return !selectQueryHasResults(query);
	}
	
	public void testClearOntology()
	{
		try {
			registry.clearOntology();
		}
		catch(Exception e) {
			fail("Failed to clear predicate ontology: " + e);
		}
		assertTrue(graphIsEmpty(registry.getOntologyGraphURI()));
	}
	
	private String strFromTemplate(String template, String ... substStrings) {
		String answer = null;
		try {
			answer = SPARQLStringUtils.strFromTemplate(template, substStrings);
		}
		catch(Exception e) {
			fail("Failed to build query from string template: " + e);
		}
		return answer;
	}
	
	private boolean selectQueryHasResults(String query) 
	{
		List<Map<String, String>> results = new ArrayList<Map<String,String>>(); 
		try {
			results = registry.selectQuery(query);
		}
		catch(Exception e) {
			LOGGER.error("Failed to query registry: " + e);
			return false;
		}
		return (results.size() > 0);
	}

	public void testAddEndpoint()
	{
		try {
			registry.addEndpoint(EXAMPLE_ENDPOINT1_URI, EXAMPLE_ENDPOINT1_TYPE);
		}
		catch(Exception e) {
			fail("Failed to add endpoint to registry: " + e);
		}
	}
	
	public void testAddAndIndexEndpoint() 
	{
		testAddAndIndexEndpoint(EXAMPLE_ENDPOINT1_URI, EXAMPLE_ENDPOINT1_TYPE);
	}

	public void testAddAndIndexEndpoint(String URI, SPARQLEndpointType type) 
	{
		try {
			registry.addAndIndexEndpoint(URI, type);
		}
		catch(Exception e) {
			fail("Failed to update endpoint data in registry: " + e);
		}
	}
	
	public void testRemoveEndpoint() 
	{
		// Add an endpoint, then remove it, then make sure no trace of it remains.
		testAddAndIndexEndpoint(EXAMPLE_ENDPOINT1_URI, EXAMPLE_ENDPOINT1_TYPE);
		try {
			registry.removeEndpoint(EXAMPLE_ENDPOINT1_URI);
		}
		catch(Exception e) {
			fail("Failed to delete endpoint: " + e);
		}
		
		String query = "SELECT * FROM %u% WHERE { %u% ?p ?o } LIMIT 1";
		query = strFromTemplate(query, registry.getIndexGraphURI(), EXAMPLE_ENDPOINT1_URI);
		assertTrue(!selectQueryHasResults(query));
	}
	
	private SPARQLEndpoint createSPARQLEndpoint(String endpointURI, SPARQLEndpointType type)
	{
		SPARQLEndpoint endpoint = null;
		try {
			endpoint = SPARQLEndpointFactory.createEndpoint(EXAMPLE_ENDPOINT1_URI, EXAMPLE_ENDPOINT1_TYPE);
		}
		catch(Exception e) {
			fail("Failed to create SPARQLEndpoint object: " + e);
		}
		return endpoint;
	}
	
	

	public void testAddPredicateToOntology() 
	{
		try {
			SPARQLEndpoint endpoint = createSPARQLEndpoint(EXAMPLE_ENDPOINT1_URI, EXAMPLE_ENDPOINT1_TYPE);		
			Set<String> predicateURIs = endpoint.getPredicates();
			assertTrue(predicateURIs.size() > 0);
			String firstPredicateURI = predicateURIs.iterator().next();
			boolean isDatatypeProperty = endpoint.isDatatypeProperty(firstPredicateURI);
			registry.addPredicateToOntology(firstPredicateURI, isDatatypeProperty);
		}
		catch(Exception e) {
			fail("Failed to add predicate to registry ontology: " + e);
		}
	}
	
	/*
	public void testGetOntologyGraph() 
	{
		Collection<Triple> triples; 
		try {
			triples = registry.getOntologyGraph();
			TriplesHelper.writeTriplesAsRDF(System.out, triples, "N3");
		}
		catch(Exception e) {
			fail("Failed to generate ontology graph: " + e);
		}
	}
	*/
}
