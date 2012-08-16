package org.sadiframework.vocab;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sadiframework.utils.ServiceUtils;
import org.sadiframework.vocab.LSRN;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class LSRNTest 
{
	@Test
	public void testFailsafeKeggGeneRegex()
	{
		// KEGG gene IDs are an unusual case for parsing input URIs because they
		// contain a colon (e.g. "hsa:1234")
		
		Model model = ModelFactory.createDefaultModel();
		Resource resource1 = model.createResource("http://unrecognized.uri.pattern/hsa:1234");
		Resource resource2 = model.createResource("http://unrecognized.uri.pattern:hsa:1234");
		Resource resource3 = model.createResource("http://unrecognized.uri.pattern#hsa:1234");
		
		String id1 = ServiceUtils.getDatabaseId(resource1, LSRN.KEGG.Gene);
		String id2 = ServiceUtils.getDatabaseId(resource2, LSRN.KEGG.Gene);
		String id3 = ServiceUtils.getDatabaseId(resource3, LSRN.KEGG.Gene);
		
		assertTrue(id1.equals("hsa:1234"));
		assertTrue(id2.equals("hsa:1234"));
		assertTrue(id3.equals("hsa:1234"));
	}

	@Test
	public void testFailsafeKeggCompoundRegex()
	{
		// KEGG gene IDs are an unusual case for parsing input URIs because they
		// contain a colon (e.g. "hsa:1234")
		
		Model model = ModelFactory.createDefaultModel();
		Resource resource1 = model.createResource("http://unrecognized.uri.pattern/cpd:1234");
		Resource resource2 = model.createResource("http://unrecognized.uri.pattern:cpd:1234");
		Resource resource3 = model.createResource("http://unrecognized.uri.pattern#cpd:1234");
		
		String id1 = ServiceUtils.getDatabaseId(resource1, LSRN.KEGG.Gene);
		String id2 = ServiceUtils.getDatabaseId(resource2, LSRN.KEGG.Gene);
		String id3 = ServiceUtils.getDatabaseId(resource3, LSRN.KEGG.Gene);
		
		assertTrue(id1.equals("cpd:1234"));
		assertTrue(id2.equals("cpd:1234"));
		assertTrue(id3.equals("cpd:1234"));
	}
	
}
