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
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author Luke McCarthy
 */
public class LSRNUtilsTest
{
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
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

}
