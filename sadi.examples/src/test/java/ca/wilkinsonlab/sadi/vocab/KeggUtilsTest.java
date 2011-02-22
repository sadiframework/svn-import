package ca.wilkinsonlab.sadi.vocab;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class KeggUtilsTest 
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
		
		String id1 = ServiceUtils.getDatabaseId(resource1, LSRN.KEGG.GENE_IDENTIFIER, LSRN.KEGG.GENE_URI_PATTERNS);
		String id2 = ServiceUtils.getDatabaseId(resource2, LSRN.KEGG.GENE_IDENTIFIER, LSRN.KEGG.GENE_URI_PATTERNS);
		String id3 = ServiceUtils.getDatabaseId(resource3, LSRN.KEGG.GENE_IDENTIFIER, LSRN.KEGG.GENE_URI_PATTERNS);
		
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
		
		String id1 = ServiceUtils.getDatabaseId(resource1, LSRN.KEGG.GENE_IDENTIFIER, LSRN.KEGG.COMPOUND_URI_PATTERNS);
		String id2 = ServiceUtils.getDatabaseId(resource2, LSRN.KEGG.GENE_IDENTIFIER, LSRN.KEGG.COMPOUND_URI_PATTERNS);
		String id3 = ServiceUtils.getDatabaseId(resource3, LSRN.KEGG.GENE_IDENTIFIER, LSRN.KEGG.COMPOUND_URI_PATTERNS);
		
		assertTrue(id1.equals("cpd:1234"));
		assertTrue(id2.equals("cpd:1234"));
		assertTrue(id3.equals("cpd:1234"));
	}
	
}
