package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


public class ServiceUtilsTest 
{
	private Pattern[] URI_PATTERNS = {
			Pattern.compile("http://lsrn.org/UniProt:(\\S+)")
	};
	
	private static String UNIPROT_PREFIX = "http://lsrn.org/UniProt:";
	private static Resource UNIPROT_IDENTIFIER = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/UniProt_Identifier");
	private static Model testModel;
	
	@BeforeClass
	public static void beforeClass()
	{
		testModel = ModelFactory.createMemModelMaker().createFreshModel();
		testModel.read(ServiceUtilsTest.class.getResourceAsStream("/ServiceUtilsTest.rdf"), null);
	}
	
	@Test
	public void testGetDatabaseID_ExplicitID()
	{
		Resource nodeWithExplicitID = testModel.getResource("http://example.com/not-a-uniprot-id-1");
		String uniprotID = ServiceUtils.getDatabaseId(nodeWithExplicitID, UNIPROT_IDENTIFIER, URI_PATTERNS);
		assertTrue(uniprotID.equals("Q7Z591"));
	}
	
	@Test
	public void testGetDatabaseID_URI()
	{
		Resource nodeWithExplicitID = testModel.getResource(UNIPROT_PREFIX + "P12345");
		String uniprotID = ServiceUtils.getDatabaseId(nodeWithExplicitID, UNIPROT_IDENTIFIER, URI_PATTERNS);
		assertTrue(uniprotID.equals("P12345"));
	}

	@Test
	public void testGetDatabaseID_NoID()
	{
		Resource nodeWithExplicitID = testModel.getResource("http://example.com/not-a-uniprot-id-2");
		String uniprotID = ServiceUtils.getDatabaseId(nodeWithExplicitID, UNIPROT_IDENTIFIER, URI_PATTERNS);
		assertTrue(uniprotID == null);
	}
}
