package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class UniProtUtilsTest
{	
	@Test
	public void testGetUniprotId()
	{
		String message = "incorrect UniProt ID from URI";
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://purl.uniprot.org/uniprot/P12345")));
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://www.uniprot.org/uniprot/P12345")));
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://www.uniprot.org/uniprot/P12345.rdf")));
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://biordf.net/moby/UniProt/P12345")));
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://lsrn.org/UniProt:P12345")));
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://lsrn.org/unknown_namespace:P12345")));
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://example.com/foo.rdf#P12345")));
	}
}
