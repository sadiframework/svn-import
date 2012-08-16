package org.sadiframework.utils;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sadiframework.utils.ServiceUtils;
import org.sadiframework.vocab.LSRN;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class ServiceUtilsTest 
{
	private static String UNIPROT_PREFIX = "http://lsrn.org/UniProt:";
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
		String uniprotID = ServiceUtils.getDatabaseId(nodeWithExplicitID, LSRN.UniProt);
		assertTrue(uniprotID.equals("Q7Z591"));
	}
	
	@Test
	public void testGetDatabaseID_URI()
	{
		Resource nodeWithExplicitID = testModel.getResource(UNIPROT_PREFIX + "P12345");
		String uniprotID = ServiceUtils.getDatabaseId(nodeWithExplicitID, LSRN.UniProt);
		assertTrue(uniprotID.equals("P12345"));
	}

	@Test
	public void testGetDatabaseID_NoID()
	{
		Resource nodeWithoutExplicitID = testModel.getResource("http://example.com/not-a-uniprot-id-2");
		String uniprotID = ServiceUtils.getDatabaseId(nodeWithoutExplicitID, LSRN.UniProt);
		// fallback is to take part after the last '/','#', or ':'.
		assertTrue(uniprotID.equals("not-a-uniprot-id-2"));
	}
}
