package ca.wilkinsonlab.sadi.rdf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.client.ServiceInputPair;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RdfRegistryTest
{
	private static final String TEST_REGISTRY_ENDPOINT = "http://biordf.net/sparql";
	private static final String TEST_REGISTRY_GRAPH = "http://sadiframework.org/test/registry/";

	private static final Logger log = Logger.getLogger(RdfRegistryTest.class);
	
	
	private static final String SERVICE_URI = "http://sadiframework.org/examples/linear";
	private static final String SERVICE_PREDICATE = "http://sadiframework.org/examples/regression.owl#hasRegressionModel";
	private static final String DIRECT_INPUT_INSTANCE = "http://sadiframework.org/examples/input/regressionDirect";
	private static final String INFERRED_INPUT_INSTANCE = "http://sadiframework.org/examples/input/regressionInferred";
	private static final String CONNECTED_CLASS = "http://sadiframework.org/examples/regression.owl#RegressionModel";
	static RdfRegistry registry;
	static Model inputModel;
	
	StopWatch timer;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		registry = new RdfRegistry(TEST_REGISTRY_ENDPOINT, TEST_REGISTRY_GRAPH);
		
		inputModel = ModelFactory.createMemModelMaker().createFreshModel();
		inputModel.read( RdfRegistryTest.class.getResourceAsStream("regression-input.rdf"), "");
	}
	
	@Before
	public void setUp() throws Exception
	{
		timer = new StopWatch();
		timer.start();
	}

	@After
	public void tearDown() throws Exception
	{
		timer.stop();
		log.info(String.format("test finished in %dms.", timer.getTime()));
	}
	
	@Test
	public void testGetService() throws Exception
	{
		assertNotNull(String.format("failed to find service %s", SERVICE_URI), registry.getService(SERVICE_URI));
	}
	
	@Test
	public void testGetAllServices() throws Exception
	{
		assertFalse("no services found in registry", registry.getAllServices().isEmpty());
	}

	@Test
	public void testFindServicesByPredicate() throws Exception
	{
		assertTrue(String.format("failed to find service %s matching predicate %s", SERVICE_URI, SERVICE_PREDICATE),
				serviceCollectionContains(registry.findServicesByPredicate(SERVICE_PREDICATE), SERVICE_URI));
	}
	
	@Test
	public void testFindServicesByInputClass() throws Exception
	{
		OntClass inputClass = registry.getService(SERVICE_URI).getInputClass();
		assertTrue(String.format("failed to find service %s matching input class %s", SERVICE_URI, inputClass),
				serviceCollectionContains(registry.findServicesByInputClass(inputClass), SERVICE_URI));
	}
	
//	@Test
//	public void testFindServicesByOutputClass() throws Exception
//	{
//		OntClass outputClass = registry.getService(SERVICE_URI).getOutputClass();
//		assertTrue(String.format("failed to find service %s matching output class %s", SERVICE_URI, outputClass),
//				serviceCollectionContains(registry.findServicesByOutputClass(outputClass), SERVICE_URI));
//	}
	
	@Test
	public void testFindServicesByConnectedClass() throws Exception
	{
		OntClass connectedClass = registry.getService(SERVICE_URI).getOutputClass().getOntModel().getOntClass(CONNECTED_CLASS);
		assertTrue(String.format("failed to find service %s matching connected class %s", SERVICE_URI, connectedClass),
				serviceCollectionContains(registry.findServicesByConnectedClass(connectedClass), SERVICE_URI));
	}
	
	@Test
	public void testFindServicesByInputInstance() throws Exception
	{
		assertTrue(String.format("failed to find service %s from input %s", SERVICE_URI, DIRECT_INPUT_INSTANCE),
				serviceCollectionContains(registry.findServicesByInputInstance(inputModel.getResource(DIRECT_INPUT_INSTANCE)), SERVICE_URI));
		assertTrue(String.format("failed to find service %s from input %s", SERVICE_URI, INFERRED_INPUT_INSTANCE),
				serviceCollectionContains(registry.findServicesByInputInstance(inputModel.getResource(INFERRED_INPUT_INSTANCE)), SERVICE_URI));
	}

	@Test
	public void testDiscoverServices() throws Exception
	{
		Collection<ServiceInputPair> pairs = registry.discoverServices(inputModel);
		assertTrue("failed to find direct service input pair",
				serviceInputPairCollectionContains(pairs, SERVICE_URI, DIRECT_INPUT_INSTANCE));
		assertTrue("failed to find inferred service input pair",
				serviceInputPairCollectionContains(pairs, SERVICE_URI, INFERRED_INPUT_INSTANCE));
	}

	@Test
	public void testFindPredicatesBySubject() throws Exception
	{
		Collection<String> predicates;
		predicates = registry.findPredicatesBySubject(inputModel.getResource(DIRECT_INPUT_INSTANCE));
		assertTrue("failed to find predicate for direct input", predicates.contains(SERVICE_PREDICATE));
		predicates = registry.findPredicatesBySubject(inputModel.getResource(INFERRED_INPUT_INSTANCE));
		assertTrue("failed to find predicate for inferred input", predicates.contains(SERVICE_PREDICATE));
	}
	
	private boolean serviceCollectionContains(Collection<RdfService> services, String serviceUri)
	{
		for (RdfService service: services)
			if (service.getServiceURI().equals(serviceUri))
				return true;
		
		return false;
	}
	
	private boolean serviceInputPairCollectionContains(Collection<ServiceInputPair> pairs, String serviceUri, String inputUri)
	{
		for (ServiceInputPair pair: pairs)
			if (pair.getService().getServiceURI().equals(serviceUri) && pair.getInput().getURI().equals(inputUri))
				return true;
		
		return false;
	}
}
