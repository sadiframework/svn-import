package org.sadiframework.service.example;

import org.sadiframework.service.AsynchronousServiceServlet;
import org.sadiframework.service.annotations.Authoritative;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.Description;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.service.annotations.ParameterClass;
import org.sadiframework.service.annotations.ParameterDefaults;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.service.annotations.URI;

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
@Authoritative(true)
@TestCases({
		@TestCase(
				input = "/t/hello-param.input.1.rdf",
				output = "/t/hello-param.output.1.rdf"
		)
		, @TestCase(
				input = "/t/hello-param.input.2.rdf",
				output = "/t/hello-param.output.2.rdf"
		)
		, @TestCase(
				input = "/t/hello-param.input.3.rdf",
				output = "/t/hello-param.output.3.rdf"
		)
})
public class AsyncParameterizedHelloWorldServiceServlet extends AsynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see org.sadiframework.service.ServiceServlet#prepareOutputModel(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	protected Model prepareOutputModel(Model inputModel)
	{
		Model model = super.prepareOutputModel(inputModel);
		model.setNsPrefix("hello", Vocab.NS);
		return model;
	}

	/* (non-Javadoc)
	 * @see org.sadiframework.service.AsynchronousServiceServlet#processInput(com.hp.hpl.jena.rdf.model.Resource, com.hp.hpl.jena.rdf.model.Resource, com.hp.hpl.jena.rdf.model.Resource)
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
