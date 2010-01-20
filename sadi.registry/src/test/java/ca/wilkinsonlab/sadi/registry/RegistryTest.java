package ca.wilkinsonlab.sadi.registry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class RegistryTest
{
	static Registry registry;
	static final String SERVICE_URI = "http://sadiframework.org/examples/linear-async";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		Model model = ModelFactory.createDefaultModel();
		registry = new Registry(model);
	}


	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
	}
	
	@Before
	public void setUp() throws Exception
	{
		registry.registerService(SERVICE_URI);
	}
	
	@After
	public void tearDown() throws Exception
	{
		registry.getModel().removeAll();
	}

	/**
	 * Test method for {@link ca.wilkinsonlab.sadi.registry.Registry#registerService(java.lang.String)}.
	 */
	@Test
	public void testRegisterService() throws Exception
	{
		assertTrue("registry did not contain service after registration",
				registry.getRegisteredServiceNodes().toSet().contains(ResourceFactory.createResource(SERVICE_URI)));
	}

	/**
	 * Test method for {@link ca.wilkinsonlab.sadi.registry.Registry#unregisterService(java.lang.String)}.
	 */
	@Test
	public void testUnregisterService() throws Exception
	{
		registry.unregisterService(SERVICE_URI);
		assertFalse("registry still contained service after unregistration",
				registry.getRegisteredServiceNodes().toSet().contains(ResourceFactory.createResource(SERVICE_URI)));
		assertTrue("registry is not empty after unregistration",
				registry.getModel().isEmpty());
	}

	/**
	 * Test method for {@link ca.wilkinsonlab.sadi.registry.Registry#getRegisteredServiceNodes()}.
	 */
	@Test
	public void testGetRegisteredServiceNodes() throws Exception
	{
		assertTrue("registered service nodes did not contain service",
				registry.getRegisteredServiceNodes().toSet().contains(ResourceFactory.createResource(SERVICE_URI)));
	}

	/**
	 * Test method for {@link ca.wilkinsonlab.sadi.registry.Registry#getRegisteredServices()}.
	 */
	@Test
	public void testGetRegisteredServices() throws Exception
	{
		for (ServiceBean service: registry.getRegisteredServices()) {
			if (service.getServiceURI().equals(SERVICE_URI))
				return;
		}
		fail("registered services did not contain service");
	}

}
