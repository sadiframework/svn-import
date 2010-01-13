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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RdfRegistryTest
{
	private static final Logger log = Logger.getLogger(RdfRegistryTest.class);
	
	private static final String SERVICE_URI = "http://sadiframework.org/examples/linear";
	private static final String SERVICE_PREDICATE = "http://sadiframework.org/examples/regression.owl#hasRegressionModel";
	private static final String DIRECT_INPUT_INSTANCE = "http://sadiframework.org/examples/input/regressionDirect";
//	something wrong with example input...
//	private static final String INFERRED_INPUT_INSTANCE = "http://sadiframework.org/examples/input/regressionInferred";
	
	static RdfRegistry registry;
	static Model inputModel;
	
	StopWatch timer;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		registry = new RdfRegistry("http://biordf.net/sparql", "http://sadiframework.org/registry");
		
		inputModel = ModelFactory.createMemModelMaker().createFreshModel();
		inputModel.read( RdfRegistryTest.class.getResourceAsStream("resources/regression-input.rdf"), "");
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
		assertTrue(String.format("failed to find service %s matching predicate %s", SERVICE_URI, SERVICE_PREDICATE),
				serviceCollectionContains(registry.findServicesByInputClass(registry.getService(SERVICE_URI).getInputClass()), SERVICE_URI));
	}
	
	@Test
	public void testFindServices() throws Exception
	{
		assertTrue(String.format("failed to find service %s from input %s", SERVICE_URI, DIRECT_INPUT_INSTANCE),
				serviceCollectionContains(registry.findServices(inputModel.getResource(DIRECT_INPUT_INSTANCE)), SERVICE_URI));
//		something wrong with example input...
//		assertTrue(String.format("failed to find service %s from input %s", SERVICE_URI, INFERRED_INPUT_INSTANCE),
//				serviceCollectionContains(registry.findServices(inputModel.getResource(INFERRED_INPUT_INSTANCE)), SERVICE_URI));
	}

	@Test
	public void testDiscoverServices() throws Exception
	{
		Collection<ServiceInputPair> pairs = registry.discoverServices(inputModel);
		assertTrue("failed to find direct service input pair",
				serviceInputPairCollectionContains(pairs, SERVICE_URI, DIRECT_INPUT_INSTANCE));
//		something wrong with example input...
//		assertTrue("failed to find inferred service input pair",
//				serviceInputPairCollectionContains(pairs, SERVICE_URI, INFERRED_INPUT_INSTANCE));
	}

	@Test
	public void testFindPredicatesBySubject() throws Exception
	{
		Collection<String> predicates;
		predicates = registry.findPredicatesBySubject(inputModel.getResource(DIRECT_INPUT_INSTANCE));
		assertTrue("failed to find predicate for direct input", predicates.contains(SERVICE_PREDICATE));
//		something wrong with example input...
//		predicates = registry.findPredicatesBySubject(inputModel.getResource(INFERRED_INPUT_INSTANCE));
//		assertTrue("failed to find predicate for inferred input", predicates.contains(SERVICE_PREDICATE));
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
