package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class ResourceTyperTest
{
	@Test
	public void testAttachType()
	{
		Model model = ModelFactory.createDefaultModel();
		Resource uniprotResource = model.createResource("http://purl.uniprot.org/uniprot/P12345");
		ResourceTyper.getResourceTyper().attachType(uniprotResource);
		assertTrue(iteratorContains(uniprotResource.listProperties(RDF.type), "http://purl.org/SADI/LSRN/UniProt_Record"));
		

		Resource biordfResource = model.createResource("http://biordf.net/moby/UniProt/P12345");
		ResourceTyper.getResourceTyper().attachType(biordfResource);
		assertTrue(iteratorContains(biordfResource.listProperties(RDF.type), "http://purl.org/SADI/LSRN/UniProt_Record"));
	}
	
	static boolean iteratorContains(StmtIterator i, String o)
	{
		while (i.hasNext()) {
			Statement statement = i.nextStatement();
			if (statement.getResource().getURI().equals(o))
				return true;
		}
		return false;
	}
}
