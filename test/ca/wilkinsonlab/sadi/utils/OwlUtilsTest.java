package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Property;

public class OwlUtilsTest
{
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
	public void testDecomposeString()
	{
		Set<OntProperty> properties = OwlUtils.listRestrictedProperties("http://elmonline.ca/sw/explore.owl#OtherClass");
		assertTrue("class did not provide expected predicate",
				propertyCollectionContains(properties, "http://elmonline.ca/sw/explore.owl#childProperty"));
		
		properties = OwlUtils.listRestrictedProperties("http://sadiframework.org/ontologies/service_objects.owl#getKEGGIDFromUniProt_Output");
		assertTrue("class did not provide expected predicate",
				propertyCollectionContains(properties, "http://es-01.chibi.ubc.ca/~benv/predicates.owl#isEncodedBy"));
	}
	
	private boolean propertyCollectionContains(Set<OntProperty> properties, String uri)
	{
		for (Property p: properties)
			if (p.getURI().equals(uri))
				return true;
		return false;
	}

//	@Test
//	public void testDecomposeOntClass()
//	{
//		fail("Not yet implemented");
//	}
}
