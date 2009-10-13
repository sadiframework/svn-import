package ca.wilkinsonlab.sadi.rdf;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class RdfServiceTest
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(RdfServiceTest.class);

	private static final String DUMMY_SERVICE = "http://localhost/dummy";
	private static final String DUMMY_INPUT_CLASS = "http://elmonline.ca/sw/explore.owl#ParentClass";
	private static final String DUMMY_OUTPUT_CLASS = "http://elmonline.ca/sw/explore.owl#OtherClass";
	private static final String DUMMY_PREDICATE = "http://elmonline.ca/sw/explore.owl#childProperty";
	private static final String DIRECT_INSTANCE = "http://localhost/dummy/directInstance";
	private static final String INFERRED_INSTANCE = "http://localhost/dummy/inferredInstance";
	private static final String DIRECT_INSTANCE_NO_PROPERTY = "http://localhost/dummy/directInstanceNoProperty";
	
	static RdfService service;
	static Model inputModel;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		service = createDummyService();
		inputModel = createInputModel();
	}
	
	private static RdfService createDummyService() throws MalformedURLException
	{
		RdfService service = new RdfService(DUMMY_SERVICE, DUMMY_INPUT_CLASS, DUMMY_OUTPUT_CLASS);
		
		return service;
	}
	
	private static Model createInputModel() throws Exception
	{
		Model model = ModelFactory.createDefaultModel();
		Resource inputClass = model.createResource(DUMMY_INPUT_CLASS);
		Property predicate = model.createProperty(DUMMY_PREDICATE);
		Resource directInstance = model.createResource(DIRECT_INSTANCE, inputClass);
		directInstance.addProperty(predicate, directInstance);
		Resource inferredInstance = model.createResource(INFERRED_INSTANCE);
		inferredInstance.addProperty(predicate, directInstance);
		model.createResource(DIRECT_INSTANCE_NO_PROPERTY, inputClass);
		
		return model;
	}
	
	@Test
	public void testGetPredicates() throws Exception
	{
		String[] expectedPredicates = new String[]{ DUMMY_PREDICATE };
		Collection<String> actualPredicates = service.getPredicates();
		for (String expected: expectedPredicates) {
			assertTrue("service does not provide expected predicate " + expected, actualPredicates.contains(expected));
		}
	}
	
	@Test
	public void testIsInputInstance() throws Exception
	{
		assertTrue("unrecognized direct input instance", service.isInputInstance(inputModel.getResource(DIRECT_INSTANCE)));
		assertTrue("unrecognized inferred input instance", service.isInputInstance(inputModel.getResource(INFERRED_INSTANCE)));
//		assertFalse("recognized input instance with no properties", service.isInputInstance(inputModel.getResource(DIRECT_INSTANCE_NO_PROPERTY)));
	}
	
	@Test
	public void testDiscoverInputInstances() throws Exception
	{
		Collection<Resource> instances = service.discoverInputInstances(inputModel);
		assertTrue("direct input instance not discovered", instances.contains(inputModel.getResource(DIRECT_INSTANCE)));
		assertTrue("inferred input instance not discovered", instances.contains(inputModel.getResource(INFERRED_INSTANCE)));
//		assertFalse("input instance with no properties discovered", instances.contains(inputModel.getResource(DIRECT_INSTANCE_NO_PROPERTY)));
	}
}
