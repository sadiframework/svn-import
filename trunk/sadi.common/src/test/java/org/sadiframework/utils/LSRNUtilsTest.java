package org.sadiframework.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sadiframework.SADIException;
import org.sadiframework.utils.LSRNUtils;


import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author Luke McCarthy
 */
public class LSRNUtilsTest
{
	private static Model testModel;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		testModel = ModelFactory.createMemModelMaker().createFreshModel();
		testModel.read(LSRNUtilsTest.class.getResourceAsStream("LSRNUtilsTest.rdf"), null);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{
	}

	/**
	 * Test method for {@link org.sadiframework.utils.LSRNUtils#getInstance(com.hp.hpl.jena.ontology.OntClass, java.lang.String)}.
	 */
	@Test
	public void testCreateInstance() throws SADIException
	{
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		Resource c = model.createResource("http://purl.oclc.org/SADI/LSRN/UniProt_Record");
		Resource instance = LSRNUtils.createInstance(c, "P12345");
		assertTrue(String.format("new instance has incorrect URI %s", instance.getURI()),
				instance.getURI().equals("http://lsrn.org/UniProt:P12345"));
		assertTrue("new instance does not have the correct type",
				instance.hasProperty(RDF.type, c));
	}

	@Test
	public void testIsLSRNURI()
	{
		assertTrue(LSRNUtils.isLSRNURI("http://lsrn.org/KEGG:hsa:1234"));
		assertFalse(LSRNUtils.isLSRNURI("http://sadiframework.org"));
	}

	@Test
	public void testGetNamespaceFromLSRNURI()
	{
		assertTrue(LSRNUtils.getNamespaceFromLSRNURI("http://lsrn.org/KEGG:hsa:1234").equals("KEGG"));
		assertTrue(LSRNUtils.getNamespaceFromLSRNURI("http://lsrn.org/UniProt:P12345").equals("UniProt"));
		assertTrue(LSRNUtils.getNamespaceFromLSRNURI("http://sadiframework.org") == null);
	}

	@Test
	public void testGetIDFromLSRNURI()
	{
		assertTrue(LSRNUtils.getIDFromLSRNURI("http://lsrn.org/KEGG:hsa:1234").equals("hsa:1234"));
		assertTrue(LSRNUtils.getIDFromLSRNURI("http://lsrn.org/UniProt:P12345").equals("P12345"));
		assertTrue(LSRNUtils.getIDFromLSRNURI("http://sadiframework.org") == null);
	}

	@Test
	public void testGetID()
	{
		Resource uniprotIdTypeURI = LSRNUtils.getIdentifierClass("UniProt");
		Resource keggGeneIdTypeURI = LSRNUtils.getIdentifierClass("KEGG");
		Resource testNode;
		String id;

		// CASE: node with non-LSRN URI and an attached identifier attribute
		testNode = testModel.getResource("http://example.com/not-a-uniprot-id-1");
		id = LSRNUtils.getID(testNode, uniprotIdTypeURI);
		assertTrue(id.equals("Q7Z591"));

		// CASE: node with standard LSRN URI and no attached identifier attribute
		testNode = testModel.getResource("http://lsrn.org/UniProt:P12345");
		id = LSRNUtils.getID(testNode, uniprotIdTypeURI);
		assertTrue(id.equals("P12345"));

		// CASE: node with non-LSRN URI and no attached identifier attribute,
		// but with URI that matches pattern in test/resources/lsrn.properties
		testNode = testModel.getResource("http://test.uri.pattern/P12345/stuff/at/the/end");
		id = LSRNUtils.getID(testNode, uniprotIdTypeURI);
		assertTrue(id.equals("P12345"));

		// CASE: node with non-standard URI and no attached identifier attribute;
		// failsafe URI pattern takes effect which takes part after last '#', ':', or '/' as id.
		testNode = testModel.getResource("http://example.com/not-a-uniprot-id-2");
		id = LSRNUtils.getID(testNode, uniprotIdTypeURI);
		assertTrue(id.equals("not-a-uniprot-id-2"));

		// CASE: custom failsafe URI pattern configured in test/resources/lsrn.properties;
		// KEGG IDs are an unusual case for parsing URIs because the database IDs contain a ":"
		testNode = testModel.getResource("http://unrecognized.uri.pattern:hsa:1234");
		id = LSRNUtils.getID(testNode, keggGeneIdTypeURI);
		assertTrue(id.equals("hsa:1234"));

	}
}
