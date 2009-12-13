package ca.wilkinsonlab.sadi.sparql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.Before;

import static org.junit.Assert.fail;

import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint.EndpointType;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.admin.Config;

public class VirtuosoSPARQLRegistryAdminTest {

	public final static Log LOGGER = LogFactory.getLog(VirtuosoSPARQLRegistryAdminTest.class);
	public final static String ENDPOINT_URI = Config.getConfiguration().getString("sadi.registry.sparql.endpoint"); 
	public final static String TEST_INDEX_GRAPH = "http://test/sparqlreg/endpoints/";
	public final static String TEST_ONTOLOGY_GRAPH = "http://test/sparqlreg/ontology/"; 

	/*
	 * TODO: A number of unit tests in this class depend on third-party SPARQL endpoints,
	 * which is bad.  To fix this, set up a test registry containing only test (dummy) SPARQL 
	 * endpoints, and use these endpoints for the unit tests instead.
	 * 
	 * For the time being, the affected unit tests are commented out.
	 */
	
	/*
	public final static String EXAMPLE_ENDPOINT1_URI = "http://omim.bio2rdf.org/sparql";
	public final static EndpointType EXAMPLE_ENDPOINT1_TYPE = EndpointType.VIRTUOSO;
	public final static String EXAMPLE_ENDPOINT2_URI = "http://kegg.bio2rdf.org/sparql";
	public final static EndpointType EXAMPLE_ENDPOINT2_TYPE = EndpointType.VIRTUOSO;
	public final static String EXAMPLE_ENDPOINT3_URI = "http://www4.wiwiss.fu-berlin.de/drugbank/sparql";
	public final static EndpointType EXAMPLE_ENDPOINT3_TYPE = EndpointType.D2R;
	*/
	
	VirtuosoSPARQLRegistryAdmin registry;

	@Before
	public void setUp() {
		try {
			registry = new VirtuosoSPARQLRegistryAdmin(ENDPOINT_URI, TEST_INDEX_GRAPH, TEST_ONTOLOGY_GRAPH);
		}
		catch(Exception e) {
			fail("Failed to initialize registry: " + e);
		}
	}
	
	/* Helper methods for unit tests. */
	
	private boolean graphIsEmpty(String graphURI)
	{
		String query = "SELECT * FROM %u% WHERE { ?s ?p ?o } LIMIT 1";
		query = strFromTemplate(query, graphURI);
		return !selectQueryHasResults(query);
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

	private void testIndexEndpoint(String URI, EndpointType type) 
	{
		try {
			registry.indexEndpoint(URI, type);
		}
		catch(Exception e) {
			fail("Failed to update endpoint data in registry: " + e);
		}
	}
	
	private SPARQLEndpoint createSPARQLEndpoint(String endpointURI, EndpointType type)
	{
		SPARQLEndpoint endpoint = null;
		try {
			endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		}
		catch(Exception e) {
			fail("Failed to create SPARQLEndpoint object: " + e);
		}
		return endpoint;
	}
	
	/* TODO: These tests are to remain disabled until their dependence on third-party
	 * endpoints has been removed (see TODO at top of file).
	 */

	/*
	@Test
	public void testAddEndpoint()
	{
		try {
			registry.addEndpoint(EXAMPLE_ENDPOINT1_URI, EXAMPLE_ENDPOINT1_TYPE);
		}
		catch(Exception e) {
			fail("Failed to add endpoint to registry: " + e);
		}
	}

	@Test
	public void testIndexEndpoint() 
	{
		testIndexEndpoint(EXAMPLE_ENDPOINT1_URI, EXAMPLE_ENDPOINT1_TYPE);
	}

	@Test
	public void testRemoveEndpoint() 
	{
		// Add an endpoint, then remove it, then make sure no trace of it remains.
		testIndexEndpoint(EXAMPLE_ENDPOINT1_URI, EXAMPLE_ENDPOINT1_TYPE);
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

	@Test
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
	*/
}
