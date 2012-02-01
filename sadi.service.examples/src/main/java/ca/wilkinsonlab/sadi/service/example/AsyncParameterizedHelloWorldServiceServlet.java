package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.annotations.ParameterClass;
import ca.wilkinsonlab.sadi.service.annotations.ParameterDefaults;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.service.annotations.URI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;

/**
 * A slightly less simple "Hello, World" service that reads a name and 
 * attaches a greeting. The language of the greeting is read from a
 * secondary parameter.
 * 
 * This also demonstrates the use of annotations for service configuration.
 * 
 * @author Luke McCarthy
 */
@URI("http://sadiframework.org/examples/hello-param-async")
@Name("AsyncParamaterizedHelloWorld")
@Description("An asynchronous \"Hello, world!\" service where the output language is specified in a parameter")
@ContactEmail("info@sadiframework.org")
@InputClass("http://sadiframework.org/examples/hello.owl#NamedIndividual")
@OutputClass("http://sadiframework.org/examples/hello.owl#GreetedIndividual")
@ParameterClass("http://sadiframework.org/examples/hello.owl#SecondaryParameters")
@ParameterDefaults({"http://sadiframework.org/examples/hello.owl#lang, http://www.w3.org/2001/XMLSchema#string", "en"})
@TestCases({
		@TestCase(
				input = "/t/hello-param-input1.rdf", 
				output = "/t/hello-param-output1.rdf"
		)
		, @TestCase(
				input = "/t/hello-param-input2.rdf", 
				output = "/t/hello-param-output2.rdf"
		)
		, @TestCase(
				input = "/t/hello-param-input3.rdf", 
				output = "/t/hello-param-output3.rdf"
		)
})
public class AsyncParameterizedHelloWorldServiceServlet extends AsynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet#processInput(com.hp.hpl.jena.rdf.model.Resource, com.hp.hpl.jena.rdf.model.Resource, com.hp.hpl.jena.rdf.model.Resource)
	 */
	@Override
	public void processInput(Resource input, Resource output, Resource parameters)
	{
		String name = input.getProperty(FOAF.name).getString();
		String lang = parameters.getProperty(Vocab.lang).getString();
		String greeting = null;
		if (lang.equalsIgnoreCase("fr"))
			greeting = "Bonjour";
		else if (lang.equalsIgnoreCase("it"))
			greeting = "Ciao";
		else if (lang.equalsIgnoreCase("es"))
			greeting = "Hola";
		else if (lang.equalsIgnoreCase("en"))
			greeting = "Hello";
		output.addProperty(Vocab.greeting, String.format("%s, %s!", greeting, name));
	}
	
	private static class Vocab
	{
		private static String NS = "http://sadiframework.org/examples/hello.owl#";
		private static Model m_model = ModelFactory.createDefaultModel();
		public static Property greeting = m_model.createProperty(NS + "greeting");
		public static Property lang = m_model.createProperty(NS + "lang");
	}
}
