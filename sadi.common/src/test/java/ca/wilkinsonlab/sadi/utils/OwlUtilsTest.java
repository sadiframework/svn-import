package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.rdfpath.RDFPath;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OwlUtilsTest
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(OwlUtilsTest.class);
	
	static final String NS = "http://sadiframework.org/ontologies/OwlUtilsTest.owl#";
	
	static OntModel model;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		model = OwlUtils.createDefaultReasoningModel();
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
	
	@Test
	public void testGetOWLModel()
	{
		OntModel owlModel = OwlUtils.getOWLModel();
		assertNotNull("getOntClass(OWL.Thing) returned null", owlModel.getOntClass(OWL.Thing.getURI()));
	}
	
	@Test
	@SuppressWarnings("deprecation")
	public void testGetLabels()
	{
		OntClass c = model.getOntClass(NS + "RangeClass");
		OntProperty p = model.getOntProperty(NS + "rangedObjectProperty");
		assertEquals(String.format("[%s, %s]", OwlUtils.getLabel(c), OwlUtils.getLabel(p)),
				OwlUtils.getLabels(Arrays.asList(new OntResource[]{c, p}).iterator()));
	}
	
	@Test
	@SuppressWarnings("deprecation")
	public void testGetRestrictionString()
	{
		OntClass c = model.getOntClass(NS + "restrictionOnUndefinedProperty");
		assertEquals(String.format("undefinedProperty min 1"),
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

	@SuppressWarnings("deprecation")
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
	
//	@Test
//	public void testLoadOntologyForURI() throws Exception
//	{
//		OntModel model = OwlUtils.createDefaultReasoningModel();
//		model.getDocumentManager().getFileManager().getLocationMapper().addAltEntry(
//				"http://sadiframework.org/ontologies/OwlUtilsTest.owl", 
//				OwlUtilsTest.class.getResource("OwlUtilsTest.owl").toExternalForm());
//		model.load("");
//	}
	
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
	
	public void testExtractMinimalOntology()
	{
//		OwlUtils.extractMinimalOntology(Model target, Model source, String uri);
	}
	
	@Test
	public void testCreateRestrictions()
	{
		RDFPath path1 = new RDFPath(
			"http://semanticscience.org/resource/SIO_000300",
				XSDDatatype.XSDstring.getURI());
		Resource root1 = model.createResource("root1");
		path1.createLiteralRootedAt(root1, "value1");
		OntClass r1 = OwlUtils.createRestrictions(model, path1, false);
		assertTrue(String.format("%s is not an instance of %s (created by %s)",
				root1, r1, path1), root1.hasProperty(RDF.type, r1));

		RDFPath path2 = new RDFPath(
			"http://semanticscience.org/resource/SIO_000008",
				"http://semanticscience.org/resource/SIO_000116",
			"http://semanticscience.org/resource/SIO_000300",
				XSDDatatype.XSDstring.getURI());
		Resource root2 = model.createResource("root2");
		path2.createLiteralRootedAt(root2, "value2");

		OntClass r2 = OwlUtils.createRestrictions(model, path2);
		assertTrue(String.format("%s is not an instance of %s (created by %s)",
				root2, r2, path2), root2.hasProperty(RDF.type, r2));
	}
	
	@Test
	public void testGetReasonerSpec() throws Exception
	{
	    assertEquals("failed for com.hp.hpl.jena.ontology.OntModelSpec.OWL_MEM_MICRO_RULE_INF",
	            com.hp.hpl.jena.ontology.OntModelSpec.OWL_MEM_MICRO_RULE_INF,
	            OwlUtils.getReasonerSpec("com.hp.hpl.jena.ontology.OntModelSpec.OWL_MEM_MICRO_RULE_INF"));
	}
	
	private boolean propertyCollectionContains(Set<OntProperty> properties, String uri)
	{
		for (Property p: properties)
			if (p.getURI().equals(uri))
				return true;
		return false;
	}
}
