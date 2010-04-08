package ca.wilkinsonlab.sadi.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.utils.OwlUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class ServiceImplTest
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(ServiceImplTest.class);

	private static final String DUMMY_SERVICE = "http://localhost/dummy";
	private static final String DUMMY_NS = "http://sadiframework.org/test/dummy.owl#";
	private static final String DUMMY_INPUT_CLASS = DUMMY_NS + "InputClass";
	private static final String DUMMY_OUTPUT_CLASS = DUMMY_NS + "OutputClass";
	private static final String DUMMY_INPUT_PREDICATE = DUMMY_NS + "inputProperty";
	private static final String DUMMY_OUTPUT_PREDICATE = DUMMY_NS + "outputProperty";
	private static final String DIRECT_INSTANCE = "http://localhost/dummy/directInstance";
	private static final String INFERRED_INSTANCE = "http://localhost/dummy/inferredInstance";
	private static final String DIRECT_INSTANCE_NO_PROPERTY = "http://localhost/dummy/directInstanceNoProperty";
	
	static ServiceImpl service;
	static Model inputModel;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		service = createDummyService();
		inputModel = createInputModel();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		service = null;
		inputModel = null;
	}
	
	@SuppressWarnings("serial")
	private static ServiceImpl createDummyService() throws SADIException
	{
		ServiceImpl service = new ServiceImpl( new HashMap<String, String>() {{
			put("serviceURI", DUMMY_SERVICE);
			put("inputClassURI", DUMMY_INPUT_CLASS);
			put("outputClassURI", DUMMY_OUTPUT_CLASS);
		}});
		
		return service;
	}
	
	private static Model createInputModel() throws Exception
	{
		Model model = ModelFactory.createDefaultModel();
		Resource inputClass = model.createResource(DUMMY_INPUT_CLASS);
		Property inputProperty = model.createProperty(DUMMY_INPUT_PREDICATE);
		Resource object = model.createResource();
		
		Resource directInstance = model.createResource(DIRECT_INSTANCE, inputClass);
		directInstance.addProperty(inputProperty, object);
		
		Resource inferredInstance = model.createResource(INFERRED_INSTANCE);
		inferredInstance.addProperty(inputProperty, object);
		
		model.createResource(DIRECT_INSTANCE_NO_PROPERTY, inputClass);
		
		return model;
	}
	
	@Test
	public void testOntology() throws Exception
	{
		OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
//		model.read(DUMMY_NS);
//		OntClass inputClass = model.getOntClass(DUMMY_INPUT_CLASS);
		OntClass inputClass = OwlUtils.getOntClassWithLoad(model, DUMMY_INPUT_CLASS);
		assertNotNull(String.format("input class %s is undefined", DUMMY_INPUT_CLASS), inputClass);
//		OntClass outputClass = model.getOntClass(DUMMY_OUTPUT_CLASS);
		OntClass outputClass = OwlUtils.getOntClassWithLoad(model, DUMMY_OUTPUT_CLASS);
		assertNotNull(String.format("output class %s is undefined", DUMMY_OUTPUT_CLASS), outputClass);
		model.add(inputModel);
		assertTrue(String.format("direct instance %s does not have type %s", DIRECT_INSTANCE, DUMMY_INPUT_CLASS),
				model.getIndividual(DIRECT_INSTANCE).hasOntClass(inputClass));
		assertTrue(String.format("indirect instance %s does not have type %s", INFERRED_INSTANCE, DUMMY_INPUT_CLASS),
				model.getIndividual(INFERRED_INSTANCE).hasOntClass(inputClass));
	}
	
	@Test
	public void testGetPredicates() throws Exception
	{
		String[] expectedPredicates = new String[]{ DUMMY_OUTPUT_PREDICATE };
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
