package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertTrue;

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
import com.hp.hpl.jena.vocabulary.OWL;

public class OwlUtilsTest
{
	static final String NS = "http://sadiframework.org/ontologies/OwlUtilsTest.owl#";
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
	public void testListRestrictedPropertiesString()
	{
		String NS = "http://sadiframework.org/ontologies/test.owl#";
		Set<OntProperty> properties = OwlUtils.listRestrictedProperties(NS + "ClassWithRestriction");
		assertTrue("class did not provide expected predicate",
				propertyCollectionContains(properties, NS + "restrictedProperty"));
	}
	
	private boolean propertyCollectionContains(Set<OntProperty> properties, String uri)
	{
		for (Property p: properties)
			if (p.getURI().equals(uri))
				return true;
		return false;
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
}
