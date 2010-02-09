package ca.wilkinsonlab.sadi.jowl;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class InstanceSerializerTest
{
	public static final String NS = "http://sadiframework.org/ontologies/test/JOWL.owl#";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
	}

	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	@SuppressWarnings("unused")
	public void testAsResource() throws SerializationException
	{
		UnannotatedClass unnanotatedInstance = new UnannotatedClass();
		Resource unannotatedResource = InstanceSerializer.asResource(unnanotatedInstance, NS);
		assertHasProperty(unannotatedResource,
				RDF.type, ResourceFactory.createResource(NS + "UnannotatedClass"));
		
		AnnotatedClass annotatedInstance = new AnnotatedClass();
		annotatedInstance.unannotatedField = "unannotatedFieldValue";
		annotatedInstance.annotatedField = "annotatedFieldValue";
		Resource annotatedResource = InstanceSerializer.asResource(annotatedInstance, NS);
		assertHasProperty(annotatedResource,
				RDF.type, ResourceFactory.createResource(NS + "ManualClassURI"));
		assertHasProperty(annotatedResource,
				ResourceFactory.createProperty(NS + "unannotatedField"),
				ResourceFactory.createPlainLiteral(annotatedInstance.unannotatedField));
		assertHasProperty(annotatedResource,
				ResourceFactory.createProperty(NS + "manualPropertyURI"),
				ResourceFactory.createPlainLiteral(annotatedInstance.annotatedField));
		
		ChildClass childInstance = new ChildClass();
		childInstance.parentProperty = new ValuesFromClass1();
		childInstance.childProperty = new ValuesFromClass2() {
			public String getURL() {
				return NS + "valuesFrom2Instance";
			}
		};
		Resource childResource = InstanceSerializer.asResource(childInstance, NS);
		assertHasProperty(childResource,
				RDF.type, ResourceFactory.createResource(NS + "ChildClass"));
		assertHasProperty(childResource,
				ResourceFactory.createProperty(NS + "childProperty"),
				ResourceFactory.createResource(NS + "valuesFrom2Instance"));
		Property parentProperty = ResourceFactory.createProperty(NS + "parentProperty");
		assertHasProperty(childResource.getRequiredProperty(parentProperty).getResource(),
				RDF.type, ResourceFactory.createResource(NS + "ValuesFromClass1"));
	}
	
	private void assertHasProperty(Resource s, Property p, RDFNode o)
	{
		assertTrue(String.format("missing statement %s %s %s", s, p, o),
				s.hasProperty(p, o));
	}
	
	static class ParentClass
	{
		ValuesFromClass1 parentProperty;
	}
	
	static class ChildClass extends ParentClass
	{
		ValuesFromClass2 childProperty;
	}
	
	static class ValuesFromClass1
	{
	}
	
	static class ValuesFromClass2
	{
	}
	
	static class UnannotatedClass
	{	
	}
	
	@OWLClass(NS + "ManualClassURI")
	static class AnnotatedClass
	{
		@OWLProperty(NS + "manualPropertyURI")
		String annotatedField;
		
		String unannotatedField;
	}
}
