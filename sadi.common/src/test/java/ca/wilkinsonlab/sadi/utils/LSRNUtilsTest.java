package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.SADIException;

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
	 * Test method for {@link ca.wilkinsonlab.sadi.utils.LSRNUtils#getInstance(com.hp.hpl.jena.ontology.OntClass, java.lang.String)}.
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
}
