package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;

public class ResourceFactoryTest
{
	@Test
	public void testCreateInstance()
	{
		String typeURI = "http://example.com/type";
		String id = "11235";
		Model model = ModelFactory.createDefaultModel();
		Resource type = model.createResource(typeURI);
		Resource r = ResourceFactory.createInstance(model, type, id);
		assertTrue("new resource is missing rdf:type", r.hasProperty(RDF.type, type));
		assertTrue("new resource is missing dc:identifier", r.hasProperty(DC.identifier, id));
	}
}
