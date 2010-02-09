package ca.wilkinsonlab.sadi.jowl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.utils.OwlUtils;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.vocabulary.OWL;

public class OntModelBuilderTest
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
	public void testBuildOntModel()
	{
		OntModel model = OntModelBuilder.buildOntModel(NS, ParentClass.class, ChildClass.class, AnnotatedClass.class, UnannotatedClass.class);
		
		OntClass UnannotatedClass = getClass(model, NS + "UnannotatedClass");
		OntProperty unannotatedProperty = getProperty(model, NS + "unannotatedField");
		OntClass AnnotatedClass = getClass(model, NS + "ManualClassURI");
		OntProperty annotatedProperty = getProperty(model, NS + "manualPropertyURI");
		
		OntClass ParentClass = getClass(model, NS + "ParentClass");
		OntProperty parentProperty = getProperty(model, NS + "parentProperty");
		OntClass ValuesFromClass1 = getClass(model, NS + "ValuesFromClass1");
		assertRestriction(ParentClass, parentProperty, ValuesFromClass1);
		
		OntClass ChildClass = getClass(model, NS + "ChildClass");
		OntProperty childProperty = getProperty(model, NS + "childProperty");
		OntClass ValuesFromClass2 = getClass(model, NS + "ValuesFromClass2");
		assertRestriction(ChildClass, parentProperty, ValuesFromClass1);
		assertRestriction(ChildClass, childProperty, ValuesFromClass2);
		
		Individual valuesFrom1Instance = model.createIndividual(NS + "valuesFrom1Instance", ValuesFromClass1);
		Individual parentInstance = model.createIndividual(NS + "parentInstance", OWL.Thing);
		parentInstance.addProperty(parentProperty, valuesFrom1Instance);
		assertHasClass(parentInstance, ParentClass);
		
		Individual valuesFrom2Instance = model.createIndividual(NS + "valuesFrom2Instance", ValuesFromClass1);
		Individual childInstance = model.createIndividual(NS + "childInstance", OWL.Thing);
		childInstance.addProperty(parentProperty, valuesFrom1Instance);
		childInstance.addProperty(childProperty, valuesFrom2Instance);
		assertHasClass(childInstance, ParentClass);
		assertHasClass(childInstance, ChildClass);
	}
	
	private OntClass getClass(OntModel model, String classURI)
	{
		OntClass c = model.getOntClass(classURI);
		assertNotNull("generated ontology does not contain class " + classURI, c);
		return c;
	}
	
	private OntProperty getProperty(OntModel model, String propertyURI)
	{
		OntProperty p = model.getOntProperty(propertyURI);
		assertNotNull("generated ontology does not contain property " + propertyURI, p);
		return p;
	}
	
	private void assertRestriction(OntClass c, OntProperty p, OntClass valuesFrom)
	{
		for (Restriction restriction: OwlUtils.listRestrictions(c)) {
			if (restriction.getOnProperty().equals(p) &&
				restriction.isSomeValuesFromRestriction() &&
				restriction.asSomeValuesFromRestriction().getSomeValuesFrom().equals(valuesFrom))
				return;
		}
	}
	
	private void assertHasClass(Individual i, OntClass c)
	{
		assertTrue(String.format("failed to identify %s as an instance of %s", i, c),
				i.hasOntClass(c));
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
