package ca.wilkinsonlab.sadi.client.virtual.sparql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInputPair;
import ca.wilkinsonlab.sadi.client.ServiceStatus;
import ca.wilkinsonlab.sadi.utils.QueryExecutorFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.util.NodeFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DARQRegistryTest 
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(DARQRegistryTest.class);
	private static SPARQLRegistry registry;
	
	private static final String SUBJECT_PREFIX1 = "subjectPrefix1:";
	private static final String SUBJECT_PREFIX2 = "subjectPrefix1:";
	private static final String OBJECT_PREFIX1 = "objectPrefix1:";
	
	private static final Resource SUBJECT1 = ResourceFactory.createResource(String.format("%ssubject", SUBJECT_PREFIX1)); 
	private static final Resource OBJECT1 = ResourceFactory.createResource(String.format("%sobject", OBJECT_PREFIX1));
	private static final Resource NONMATCHING_SUBJECT = ResourceFactory.createResource("nonmatching:subject");
	private static final Resource NONMATCHING_OBJECT = ResourceFactory.createResource("nonmatching:object");
	
	private static final String SPARQL_ENDPOINT1_URL = "http://endpoint1/sparql";
	private static final String SPARQL_ENDPOINT2_URL = "http://endpoint2/sparql";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		String registryFile = DARQRegistryTest.class.getResource("darq.registry.n3").getFile();
		registry = new DARQRegistry(QueryExecutorFactory.createFileModelQueryExecutor(registryFile));
	}

	@Test
	public void testGetSPARQLEndpoint() throws Exception
	{
		SPARQLEndpoint endpoint = registry.getSPARQLEndpoint(SPARQL_ENDPOINT1_URL);
		assertTrue(endpoint != null);
		assertTrue(endpoint.getResultsLimit() == 10000);
	}
	
	@Test
	public void testFindSPARQLEndpointsByTriplePattern() throws Exception
	{
		
		Collection<SPARQLEndpoint> matches;
		Set<String> matchURIs;
		Node s, p, o;
		Triple pattern;
		
		// Test 1: triple pattern matches endpoint #1 and endpoint #3.
		// Only endpoint #1 should be returned, because endpoint #3 
		// has status dead.
		
		s = SUBJECT1.asNode();
		p = RDFS.label.asNode();
		o = NodeFactory.parseNode("?o");
		
		pattern = new Triple(s, p, o);
		
		matches = registry.findSPARQLEndpointsByTriplePattern(pattern);
		
		assertTrue(matches.size() == 1);
		assertTrue(matches.iterator().next().getURI().equals(SPARQL_ENDPOINT1_URL));
		
		// Test 2: triple pattern predicate in endpoint #1, but not subject regex
		
		s = NONMATCHING_SUBJECT.asNode();
		p = RDFS.label.asNode();
		o = NodeFactory.parseNode("?o");
		
		pattern = new Triple(s, p, o);
		
		matches = registry.findSPARQLEndpointsByTriplePattern(pattern);
		
		assertTrue(matches.size() == 0);
		
		// Test 3: triple pattern matches all endpoints. Endpoint #3
		// should be ignored because it has status dead.
		
		s = NodeFactory.parseNode("?s");
		p = RDFS.label.asNode();
		o = NodeFactory.parseNode("?o");
		
		pattern = new Triple(s, p, o);
		
		matches = registry.findSPARQLEndpointsByTriplePattern(pattern);
		
		matchURIs = new HashSet<String>();
		for(SPARQLEndpoint endpoint : matches) {
			matchURIs.add(endpoint.getURI());
		}

		assertTrue(matches.size() == 2);
		
		assertTrue(matchURIs.contains(SPARQL_ENDPOINT1_URL));
		assertTrue(matchURIs.contains(SPARQL_ENDPOINT2_URL));
		
		// Test 4: triple pattern matches only endpoint #2, by object regex
		
		s = NodeFactory.parseNode("?s");
		p = NodeFactory.parseNode("?p");
		o = OBJECT1.asNode();
		
		pattern = new Triple(s, p, o);
		
		matches = registry.findSPARQLEndpointsByTriplePattern(pattern);
		
		assertTrue(matches.size() == 1);
		assertTrue(matches.iterator().next().getURI().equals(SPARQL_ENDPOINT2_URL));
		
		// Test 5: triple pattern is all variables, matches all endpoints.
		// Endpoint #3 should be omitted because it has status dead.
		
		s = NodeFactory.parseNode("?s");
		p = NodeFactory.parseNode("?p");
		o = NodeFactory.parseNode("?o");
		
		pattern = new Triple(s, p, o);
		
		matches = registry.findSPARQLEndpointsByTriplePattern(pattern);
		
		matchURIs = new HashSet<String>();
		for(SPARQLEndpoint endpoint : matches) {
			matchURIs.add(endpoint.getURI());
		}

		assertTrue(matches.size() == 2);
		
		assertTrue(matchURIs.contains(SPARQL_ENDPOINT1_URL));
		assertTrue(matchURIs.contains(SPARQL_ENDPOINT2_URL));
		
	}
	
	@Test
	public void getAllSPARQLEndpoints() throws Exception
	{
		Collection<SPARQLEndpoint> endpoints = registry.getAllSPARQLEndpoints();

		Set<String> endpointURIs = new HashSet<String>();
		for(SPARQLEndpoint endpoint : endpoints) {
			endpointURIs.add(endpoint.getURI());
		}

		assertTrue(endpoints.size() == 2);
		
		assertTrue(endpointURIs.contains(SPARQL_ENDPOINT1_URL));
		assertTrue(endpointURIs.contains(SPARQL_ENDPOINT2_URL));
	}
	
	@Test
	public void testSubjectMatchesRegEx() throws Exception
	{
		String subject;
		
		// Test 1: subject matches endpoint
		
		subject = String.format("%ssubject", SUBJECT_PREFIX2);
		assertTrue(registry.subjectMatchesRegEx(SPARQL_ENDPOINT2_URL, subject));
		
		// Test 2: subject doesn't match endpoint
		
		subject = "nonmatching:subject";
		assertFalse(registry.subjectMatchesRegEx(SPARQL_ENDPOINT2_URL, subject));
	}
	
	@Test
	public void testObjectMatchesRegEx() throws Exception
	{
		String object;
		
		// Test 1: object matches endpoint
		
		object = OBJECT1.getURI();
		assertTrue(registry.objectMatchesRegEx(SPARQL_ENDPOINT2_URL, object));
		
		// Test 2: subject doesn't match endpoint
		
		object = NONMATCHING_OBJECT.getURI();
		assertFalse(registry.objectMatchesRegEx(SPARQL_ENDPOINT2_URL, object));
	}
	
	@Test
	public void testDiscoverServicesByModel() throws Exception
	{
			
		// Test:
		// 
		// The input RDF file (input.model.n3) contains statements
		// about SUBJECT1 and OBJECT1.
		//
		// => SPARQL endpoint #1 matches SUBJECT1. 
		// => SPARQL endpoint #2 matches SUBJECT1 and OBJECT1.
		//

		Model model = ModelFactory.createDefaultModel();
		InputStream is = DARQRegistryTest.class.getResourceAsStream("input.model.n3"); 
		model.read(is, "", "N3");
		
		Collection<? extends ServiceInputPair> serviceInputPairs = registry.discoverServices(model);
		
		assertTrue(serviceInputPairs.size() == 3);
		
		for(ServiceInputPair serviceInputPair : serviceInputPairs) {
			
			Collection<Resource> input = serviceInputPair.getInput();
			SPARQLServiceWrapper sparqlService = (SPARQLServiceWrapper)serviceInputPair.getService();
			
			if (sparqlService.getURI().equals(SPARQL_ENDPOINT1_URL)) {

				assertTrue(input.size() == 1);
				assertTrue(input.contains(SUBJECT1));
			
			} else if (sparqlService.getURI().equals(SPARQL_ENDPOINT2_URL)) {
			
				assertTrue(input.size() == 1);
				assertTrue((!sparqlService.mapInputsToObjectPosition() && input.contains(SUBJECT1)) ||
						(sparqlService.mapInputsToObjectPosition && input.contains(OBJECT1)));
			
			} else {

				fail("unexpected service URI in ServiceInputPair");

			}
	
		}

	}
	
	@Test
	public void testDiscoverServicesBySubject() throws Exception
	{
		Collection<? extends Service> services = registry.discoverServices(OBJECT1);
		
		assertTrue(services.size() == 1);
		assertTrue(services.iterator().next().getURI().equals(SPARQL_ENDPOINT2_URL));
	}
	
	@SuppressWarnings("deprecation")
	@Test 
	public void testFindPredicatesBySubject() throws Exception
	{
		Collection<String> predicates = registry.findPredicatesBySubject(SUBJECT1);
		
		assertTrue(predicates.size() == 2);
		assertTrue(predicates.contains(RDFS.label.getURI()));
		assertTrue(predicates.contains(RDF.type.getURI()));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testFindServices() throws Exception
	{
		Collection<? extends Service> matches;
		
		matches = registry.findServices(SUBJECT1, RDFS.label.getURI());
		assertTrue(matches.size() == 1);
		assertTrue(matches.iterator().next().getURI().equals(SPARQL_ENDPOINT1_URL));
	
 		// test compatibility with temporary SHARE hack for inverse properties
		matches = registry.findServices(OBJECT1, String.format("%s-inverse", RDF.type.getURI()));
		assertTrue(matches.size() == 1);
		assertTrue(matches.iterator().next().getURI().equals(SPARQL_ENDPOINT2_URL));
		
	}

	/*
	@Test
	public void testFindServicesByInputInstance() throws Exception
	{
		Collection<? extends Service> matches = registry.findServicesByInputInstance(OBJECT1);
		assertTrue(matches.size() == 1);
		assertTrue(matches.iterator().next().getURI().equals(SPARQL_ENDPOINT2_URL));
	}
	*/
	
	@SuppressWarnings("deprecation")
	@Test
	public void testFindServicesByPredicate() throws Exception
	{
		Collection<? extends Service> matches = registry.findServicesByPredicate(RDF.type.getURI());
		assertTrue(matches.size() == 1);
		assertTrue(matches.iterator().next().getURI().equals(SPARQL_ENDPOINT2_URL));

		// test compatibility with temporary SHARE hack for inverse properties
		matches = registry.findServicesByPredicate(String.format("%s-inverse", RDF.type.getURI()));
		assertTrue(matches.size() == 1);
		assertTrue(matches.iterator().next().getURI().equals(SPARQL_ENDPOINT2_URL));
	}
	
	@Test
	public void testFindServicesByAttachedProperty() throws Exception
	{
		Collection<? extends Service> matches = registry.findServicesByAttachedProperty(RDF.type);
		assertTrue(matches.size() == 1);
		assertTrue(matches.iterator().next().getURI().equals(SPARQL_ENDPOINT2_URL));
	}
	
	@Test
	public void testFindServicesByInputClass() throws Exception
	{
		// TODO: It may be possible to support by including the rdf:types for
		// the subjects/objects of each predicate in the endpoint index.  
		Collection<? extends Service> matches = registry.findServicesByInputClass(OWL.Thing);
		assertTrue(matches.isEmpty());
	}

	@Test
	public void testFindServicesByConnectedClass() throws Exception
	{
		// TODO: It may be possible to support this by storing rdf:types for
		// the subjects/objects of each predicate in the endpoint index.  
		Collection<? extends Service> matches = registry.findServicesByConnectedClass(OWL.Thing);
		assertTrue(matches.isEmpty());
	}
	
	@Test
	public void testGetAllServices() throws Exception
	{
		Collection<? extends Service> services = registry.getAllServices();

		// There are only two endpoints in the mock registry that have
		// status alive, and one that is dead.
		// However, there are two services generated for every 
		// SPARQL endpoint:
		// 
		//  1) A service that retrieves triples where the input is the subject
		//  2) A service that retrieves triples where the input is the object
		
		assertTrue(services.size() == 4);

		Set<String> subjectServiceURIs = new HashSet<String>();
		Set<String> objectServiceURIs = new HashSet<String>();
		
		for(Service service : services) {
			
			SPARQLServiceWrapper wrapperService = (SPARQLServiceWrapper)service;
			
			if (wrapperService.mapInputsToObjectPosition()) {
				objectServiceURIs.add(wrapperService.getURI());
			} else {
				subjectServiceURIs.add(wrapperService.getURI());
			}
			
		}
		
		assertTrue(subjectServiceURIs.contains(SPARQL_ENDPOINT1_URL));
		assertTrue(subjectServiceURIs.contains(SPARQL_ENDPOINT2_URL));
		assertTrue(objectServiceURIs.contains(SPARQL_ENDPOINT1_URL));
		assertTrue(objectServiceURIs.contains(SPARQL_ENDPOINT2_URL));
	}
	
	@Test
	public void testGetServiceStatus() throws Exception
	{
		ServiceStatus status = registry.getServiceStatus(SPARQL_ENDPOINT1_URL);
		assertTrue(status.equals(ServiceStatus.OK));
	}
	
	@Test
	public void testGetService() throws Exception
	{
		Service service = registry.getService(SPARQL_ENDPOINT1_URL);
		assertTrue(service != null);
		assertTrue(service.getURI().equals(SPARQL_ENDPOINT1_URL));
	}

}
