package ca.wilkinsonlab.sadi.utils.blast;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.BadURIException;

public class BLASTUtilsTest
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

	@Test
	public void testParseBLAST() throws Exception
	{
		Model model = ModelFactory.createDefaultModel();
		BLASTUtils.parseBLAST(BLASTUtilsTest.class.getResourceAsStream("/blast-report.xml"), model);
		assertFalse("empty result model", model.isEmpty());
		try {
			model.write(System.out, "RDF/XML");
		} catch (BadURIException e) {
			fail("result model contains invalid URIs");
		}
	}
}
