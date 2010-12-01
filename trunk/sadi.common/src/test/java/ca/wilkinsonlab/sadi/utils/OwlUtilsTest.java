package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OwlUtilsTest
{
	static final String NS = "http://sadiframework.org/ontologies/OwlUtilsTest.owl#";
	static final String MINIMAL_ONTOLOGY_NS = "http://sadiframework.org/ontologies/test/MinimalOntologyTest.owl#";
	
	static OntModel model;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		model.read( OwlUtilsTest.class.getResourceAsStream("OwlUtilsTest.owl"), NS );
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		model.close();
	}

	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}

//	@Test
//	public void testLoadOWLFilesForPredicates()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testLoadOntologyForUri()
//	{
//		fail("Not yet implemented");
//	}
	
	@Test
	public void testGetOWLModel()
	{
		OntModel owlModel = OwlUtils.getOWLModel();
		assertNotNull("getOntClass(OWL.Thing) returned null", owlModel.getOntClass(OWL.Thing.getURI()));
	}
	
	@Test
	public void testGetLabels()
	{
		OntClass c = model.getOntClass(NS + "RangeClass");
		OntProperty p = model.getOntProperty(NS + "rangedObjectProperty");
		assertEquals(String.format("[%s, %s]", OwlUtils.getLabel(c), OwlUtils.getLabel(p)),
				OwlUtils.getLabels(Arrays.asList(new OntResource[]{c, p}).iterator()));
	}
	
	@Test
	public void testGetRestrictionString()
	{
		OntClass c = model.getOntClass(NS + "restrictionOnUndefinedProperty");
		assertEquals(String.format("{undefinedProperty minCardinality 1}"),
				OwlUtils.getRestrictionString(c.asRestriction()));
	}
	
	@Test
	public void testGetDefaultRange()
	{
		OntModel localModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		OntProperty p = localModel.createOntProperty(NS + "p");
		assertEquals(String.format("default range of property isn't RDFS.Resource"),
				localModel.createClass(RDFS.Resource.getURI()), OwlUtils.getDefaultRange(p));
		OntProperty dataP = localModel.createDatatypeProperty(NS + "dataP");
		assertEquals(String.format("default range of datatype property isn't RDFS.Literal"),
				localModel.createClass(RDFS.Literal.getURI()), OwlUtils.getDefaultRange(dataP));
		OntProperty objectP = localModel.createObjectProperty(NS + "objectP");
		assertEquals(String.format("default range of object property isn't OWL.Thing"),
				localModel.createClass(OWL.Thing.getURI()), OwlUtils.getDefaultRange(objectP));
	}

	@Test
	public void testListRestrictedPropertiesString() throws Exception
	{
		String NS = "http://sadiframework.org/ontologies/test.owl#";
		Set<OntProperty> properties = OwlUtils.listRestrictedProperties(NS + "ClassWithRestriction");
		assertTrue("class did not provide expected predicate",
				propertyCollectionContains(properties, NS + "restrictedProperty"));
	}

	@Test
	public void testListRestrictedPropertiesOntClass()
	{
		OntClass c = model.getOntClass(NS + "ClassWithRestriction");
		Set<OntProperty> properties = OwlUtils.listRestrictedProperties(c);
		assertTrue("class did not provide expected predicate",
				propertyCollectionContains(properties, NS + "restrictedProperty"));
	}
	
//	@Test
//	public void testGetUsefulRangeExpectedValues()
//	{
//		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
//		OntProperty p = OwlUtils.getOntPropertyWithLoad(model, "http://sadiframework.org/ontologies/predicates.owl#has3DStructure");
//		OntClass range = OwlUtils.getUsefulRange(p);
//		String expected = "http://purl.oclc.org/SADI/LSRN/PDB_Record";
//		assertTrue(String.format("getUsefulRange of %s did not return %s", p, expected),
//				range.getURI().equals(expected));
//	}
	
	@Test
	public void testGetUsefulRangeOnRangedObjectProperty()
	{
		OntProperty p = model.getOntProperty(NS + "rangedObjectProperty");
		OntClass c = OwlUtils.getUsefulRange(p);
		assertTrue("getUsefulRange did not return expected class",
				c.getURI().equals(NS + "RangeClass"));
	}
	
	@Test
	public void testGetUsefulRangeOnUnrangedObjectProperty()
	{
		OntProperty p = model.getOntProperty(NS + "unrangedObjectProperty");
		OntClass c = OwlUtils.getUsefulRange(p);
		assertTrue("getUsefulRange did not return expected class",
				c.equals(OWL.Thing));
	}
	
	@Test
	public void testGetUsefulRangeOnRangedDatatypeProperty()
	{
		OntProperty p = model.getOntProperty(NS + "rangedDatatypeProperty");
		OntClass c = OwlUtils.getUsefulRange(p);
		assertTrue("getUsefulRange did not return expected class",
				c.getURI().equals("http://www.w3.org/2001/XMLSchema#string"));
	}
	
	@Test
	public void testGetUsefulRangeOnUnrangedDatatypeProperty()
	{
		OntProperty p = model.getOntProperty(NS + "unrangedDatatypeProperty");
		OntClass c = OwlUtils.getUsefulRange(p);
		assertTrue("getUsefulRange did not return expected class",
				c.getURI().equals("http://www.w3.org/2000/01/rdf-schema#Literal"));
	}
	
	@Test
	public void testGetUsefulRangeOnRDFProperty()
	{
		model.read(RDF.getURI());
		OntProperty p = model.getOntProperty(RDF.value.getURI());
		OntClass c = OwlUtils.getUsefulRange(p);
		assertTrue("getUsefulRange did not return expected class",
				c.getURI().equals("http://www.w3.org/2000/01/rdf-schema#Resource"));
	}
	
	@Test
	public void testGetValuesFrom()
	{
		OntClass c = model.getOntClass(NS + "ClassWithRestriction");
		Set<String> restrictionValuesFrom = new HashSet<String>();
		for (Restriction r: OwlUtils.listRestrictions(c)) {
			OntResource valuesFrom = OwlUtils.getValuesFrom(r);
			if (valuesFrom.isURIResource())
				restrictionValuesFrom.add(valuesFrom.getURI());
		}
		assertTrue("getValuesFrom did not return expected class",
				restrictionValuesFrom.contains(NS + "ValuesFromClass"));
	}
	
	@Test
	public void testLoadMinimalOntologyForUri() throws Exception
	{
		OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		String pURI = "http://sadiframework.org/ontologies/test/InversePropertyTest.owl#p";
		String qURI = "http://sadiframework.org/ontologies/test/InversePropertyTest.owl#q";
		OwlUtils.loadMinimalOntologyForUri(model, pURI);
		OntProperty p = model.getOntProperty(pURI);
		assertNotNull(String.format("minimal model missing property %s", p), p);
		OntProperty q = model.getOntProperty(qURI);
		assertNotNull(String.format("minimal model missing property %s", q), q);
		assertTrue(String.format("%s is not an inverse of %s", p, q), p.isInverseOf(q));
		assertTrue(String.format("%s is not an inverse of %s", q, p), q.isInverseOf(p));
	}
	
	@Test
	public void testLoadMinimalOntologyFromUri()
	{
		String rootUri = MINIMAL_ONTOLOGY_NS + "ClassD";

		Resource classD = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "ClassD");
		Resource classA = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "ClassA");
		Resource classB = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "ClassB");
		Resource classC = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "ClassC");
		Resource propA = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "PropertyA");
		Resource propB = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "PropertyB");
		Resource propC = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "PropertyC");
		Resource propD = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "PropertyD");
		Resource propE = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "PropertyE");
		
		try {
			OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
			OwlUtils.loadMinimalOntologyForUri(ontModel, rootUri);
			
			// stuff that the minimal model should have
			assertTrue(String.format("minimal ontology is missing %s", classA), ontModel.containsResource(classA));
			assertTrue(String.format("minimal ontology is missing %s", classB), ontModel.containsResource(classB));
			assertTrue(String.format("minimal ontology is missing %s", classD), ontModel.containsResource(classD));
			assertTrue(String.format("minimal ontology is missing %s", propA), ontModel.containsResource(propA));
			assertTrue(String.format("minimal ontology is missing %s", propB), ontModel.containsResource(propB));
			assertTrue(String.format("minimal ontology is missing %s", propC), ontModel.containsResource(propC));
			assertTrue(String.format("minimal ontology is missing %s", propD), ontModel.containsResource(propD));

			// stuff that the minimal model shouldn't have
			assertFalse(String.format("minimal ontology should not contain %s", classC), ontModel.containsResource(classC));
			assertFalse(String.format("minimal ontology should not contain %s", propE), ontModel.containsResource(propE));
			
		} catch(Exception e) {
			fail(String.format("failed to load minimal ontology for %s:\n%s", rootUri, ExceptionUtils.getStackTrace(e)));
		}
	}
	
	@Test
	public void testGetEquivalentProperties()
	{
		OntProperty p = model.getOntProperty( NS + "p" );
		OntProperty equivalentToP = model.getOntProperty( NS + "equivalentToP" );
		OntProperty subP = model.getOntProperty( NS + "subP" );
		OntProperty subEquivalentToP = model.getOntProperty( NS + "subEquivalentToP" );
		Set<OntProperty> equivs;

		{
			equivs = OwlUtils.getEquivalentProperties(p);
			assertTrue(String.format("getEquivalentProperties(p) fails to return %s", p), equivs.contains(p));
			assertTrue(String.format("getEquivalentProperties(p) fails to return %s", equivalentToP), equivs.contains(equivalentToP));
			assertFalse(String.format("getEquivalentProperties(p) incorrectly returns %s", subP), equivs.contains(subP));
			assertFalse(String.format("getEquivalentProperties(p) incorrectly returns %s", subEquivalentToP), equivs.contains(subEquivalentToP));
		}
		{
			equivs = OwlUtils.getEquivalentProperties(p, true);
			assertTrue(String.format("getEquivalentProperties(p, true) fails to return %s", p), equivs.contains(p));
			assertTrue(String.format("getEquivalentProperties(p, true) fails to return %s", equivalentToP), equivs.contains(equivalentToP));
			assertTrue(String.format("getEquivalentProperties(p, true) fails to return %s", subP), equivs.contains(subP));
			assertTrue(String.format("getEquivalentProperties(p, true) fails to return %s", subEquivalentToP), equivs.contains(subEquivalentToP));
		}
		{
			equivs = OwlUtils.getEquivalentProperties(p, false);
			assertTrue(String.format("getEquivalentProperties(p, false) fails to return %s", p), equivs.contains(p));
			assertTrue(String.format("getEquivalentProperties(p, false) fails to return %s", equivalentToP), equivs.contains(equivalentToP));
			assertFalse(String.format("getEquivalentProperties(p, false) incorrectly returns %s", subP), equivs.contains(subP));
			assertFalse(String.format("getEquivalentProperties(p, false) incorrectly returns %s", subEquivalentToP), equivs.contains(subEquivalentToP));
		}
	}
	
	public void testGetMinimalModel()
	{
		
	}
	
// This isn't working right now; figure out why...
//	@Test
//	public void testCreateDummyInstance() throws Exception
//	{
//		OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
//		OntClass clazz = OwlUtils.getOntClassWithLoad(model, "http://semanticscience.org/sadi/ontology/lipinskiserviceontology.owl#hbdasmilesmolecule");
//		Resource instance = OwlUtils.createDummyInstance(clazz);
//		System.out.println(RdfUtils.logStatements(instance.getModel()));
//		model.addSubModel(instance.getModel());
//		assertTrue(String.format("dummy is not an instance of %s", clazz), clazz.listInstances().toSet().contains(instance));
//	}
	
	private boolean propertyCollectionContains(Set<OntProperty> properties, String uri)
	{
		for (Property p: properties)
			if (p.getURI().equals(uri))
				return true;
		return false;
	}
}
