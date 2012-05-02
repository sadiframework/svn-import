package org.sadiframework.utils;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.sadiframework.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class RdfUtilsIT
{
	@Test
	public void testLoadModelFromRemoteURL() throws IOException
	{
		Model model = ModelFactory.createDefaultModel();
		String ontology = "http://sadiframework.org/ontologies/sadi.owl";
		RdfUtils.loadModelFromString(model, ontology);
		assertTrue(model.contains(ResourceFactory.createResource(ontology), RDF.type, OWL.Ontology));
		model.close();
	}
}
