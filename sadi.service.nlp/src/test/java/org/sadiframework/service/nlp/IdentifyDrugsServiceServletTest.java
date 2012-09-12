package org.sadiframework.service.nlp;

import gate.Document;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.sadiframework.service.nlp.vocab.NLP2RDF;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDFS;

public class IdentifyDrugsServiceServletTest
{
	@Test
	@Ignore
	public void testAnnotateDocument() throws Exception
	{
		String text = IOUtils.toString(getClass().getResourceAsStream("/test.txt"));
		Document doc = GateHelper.getGateHelper().annotateText(text);
		IdentifyDrugsServiceServlet service = new IdentifyDrugsServiceServlet();
//		Model model = service.prepareOutputModel(ModelFactory.createDefaultModel());
		Model model = ModelFactory.createDefaultModel();
//		model.setNsPrefix("nlp2rdf", "http://nlp2rdf.lod2.eu/schema/");
		model.setNsPrefix("str", NLP2RDF.str);
		model.setNsPrefix("scms", NLP2RDF.scms); // for scms:means
		model.setNsPrefix("drugbank", "http://www.drugbank.ca/drugs/"); // for output URIs
		model.setNsPrefix("rdfs", RDFS.getURI()); // for rdfs:label
		model.setNsPrefix("dc", DC.getURI()); // for dc:identifer
		Resource output = model.createResource("http://example.com/test");
		service.annotateDocument(doc, output);
		model.write(System.out, "N3");
	}
}
