package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;

/**
 * A simple "Hello, World" service that reads a name and attaches a greeting.
 * 
 * @author Luke McCarthy
 */
@SuppressWarnings("serial")
public class HelloWorldServiceServlet extends SimpleSynchronousServiceServlet
{
	private static String NS = "http://sadiframework.org/examples/hello.owl#";
	private static Property greeting = ResourceFactory.createProperty(NS + "greeting");
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet#processInput(com.hp.hpl.jena.rdf.model.Resource, com.hp.hpl.jena.rdf.model.Resource)
	 */
	@Override
	protected void processInput(Resource input, Resource output)
	{
		String name = input.getProperty(FOAF.name).getString();
		output.addProperty(greeting, String.format("Hello, %s!", name));
	}
}
