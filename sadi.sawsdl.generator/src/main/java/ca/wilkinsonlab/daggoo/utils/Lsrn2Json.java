package ca.wilkinsonlab.daggoo.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class Lsrn2Json {

    public static String GenerateLSRN2Json() {
	StringBuilder sb = new StringBuilder("[");
	Model m = ModelFactory.createDefaultModel();
	m.read("http://sadiframework.org/RESOURCES/Ontologies/LSRN/", null);
	ResIterator iterator = m.listResourcesWithProperty(RDF.type,OWL.Class);
	int count = 0;
	while (iterator.hasNext()) {
	    Resource node = iterator.next();
	    sb.append(count++ == 0 ? "" : ",");
	    sb.append(String.format("{\"uri\":\"%s\", \"label\":\"%s\"}", node.getURI(), node.getProperty(RDFS.label).getObject()));
	}
	String xsdUri = "http://w3c.org/2001/XMLSchema#";
	String[] primitives = {"string", "int", "boolean", "decimal","float","double"};
	for (String s : primitives) {
	    sb.append(String.format(",{\"uri\":\"%s\", \"label\":\"%s\"}", xsdUri+s, s));
	}
	
	sb.append("]");
	return sb.toString();
    }
    
    public static void main(String[] args) throws Exception {
	BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File("src/main/webapp/resources/LSRN.json")));
	stream.write(GenerateLSRN2Json().getBytes());
	stream.flush();
	stream.close();
    }
}
