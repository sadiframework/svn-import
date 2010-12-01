package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;

/**
 * A simple "Hello, World" service that reads a name and attaches a greeting.
 * 
 * @author Luke McCarthy
 */
@SuppressWarnings("serial")
public class HelloWorldServiceServlet extends SimpleSynchronousServiceServlet
{
	public void processInput(Resource input, Resource output)
	{
		String name = input.getProperty(FOAF.name).getString();
		output.addProperty(Vocab.greeting, String.format("Hello, %s!", name));
	}
	
	private static class Vocab
	{
		private static String NS = "http://sadiframework.org/examples/hello.owl#";
		private static Model m_model = ModelFactory.createDefaultModel();
		public static Property greeting = m_model.createProperty(NS + "greeting");
	}
}
