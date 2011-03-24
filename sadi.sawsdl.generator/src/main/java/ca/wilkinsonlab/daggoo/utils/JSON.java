package ca.wilkinsonlab.daggoo.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC_11;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class JSON {

    public static String GenerateOWLClassJSON() {
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
    
    public static String GenerateOWLPropertyJSON() {
	StringBuilder sb = new StringBuilder("[");
	Model m = ModelFactory.createDefaultModel();
	m.read("http://semanticscience.org/ontology/sio-core.owl", null);
	m.read("http://sadiframework.org/ontologies/predicates.owl", null);
	ResIterator iterator = m.listResourcesWithProperty(RDF.type,OWL.ObjectProperty);
	int count = 0;
	while (iterator.hasNext()) {
	    Resource node = iterator.next();
	    sb.append(count++ == 0 ? "" : ",");
	    String uri = node.getURI();
	    String label = node.hasProperty(RDFS.label) ? node.getProperty(RDFS.label).getObject().toString() : node.getLocalName();
	    String description = node.hasProperty(DC_11.description) ? node.getProperty(RDFS.label).getObject().toString() : "";
	    if (label.trim().matches(".*@\\w+")) {
		label = label.trim().substring(0, label.length()-3);
	    }
	    if (description.trim().matches(".*@\\w+")) {
		description = description.trim().substring(0, description.length()-3);
	    }
	    sb.append(String.format("{\"uri\":\"%s\", \"label\":\"%s\", \"description\":\"%s\"}", uri, label, description));
	}
	iterator = m.listResourcesWithProperty(RDF.type,OWL.DatatypeProperty);
	while (iterator.hasNext()) {
	    Resource node = iterator.next();
	    sb.append(count++ == 0 ? "" : ",");
	    String uri = node.getURI();
	    String description = node.hasProperty(DC_11.description) ? node.getProperty(RDFS.label).getObject().toString() : "";
	    String label = node.hasProperty(RDFS.label) ? node.getProperty(RDFS.label).getObject().toString() : node.getLocalName();
	    if (label.trim().matches(".*@\\w+")) {
		label = label.trim().substring(0, label.length()-3);
	    }
	    if (description.trim().matches(".*@\\w+")) {
		description = description.trim().substring(0, description.length()-3);
	    }
	    sb.append(String.format("{\"uri\":\"%s\", \"label\":\"%s\", \"description\":\"%s\"}", uri, label, description));
	}	
	sb.append("]");
	return sb.toString();
    }
    
    
    public static void main(String[] args) throws Exception {
	
	BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File("src/main/webapp/resources/LSRN.json")));
	stream.write(GenerateOWLClassJSON().getBytes());
	stream.flush();
	stream.close();
	stream = new BufferedOutputStream(new FileOutputStream(new File("src/main/webapp/resources/props.json")));
	stream.write(GenerateOWLPropertyJSON().getBytes());
	stream.flush();
	stream.close();
    }
}
