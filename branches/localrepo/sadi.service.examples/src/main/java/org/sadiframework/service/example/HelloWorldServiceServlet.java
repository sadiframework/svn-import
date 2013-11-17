package org.sadiframework.service.example;

import org.sadiframework.service.annotations.Authoritative;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.service.simple.SimpleSynchronousServiceServlet;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
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
@ContactEmail("info@sadiframework.org")
@Authoritative(true)
@TestCases({
	@TestCase(
			input = "http://sadiframework.org/examples/t/hello.input.1.rdf",
			output = "http://sadiframework.org/examples/t/hello.output.1.rdf"
	)
})
public class HelloWorldServiceServlet extends SimpleSynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;

	protected Model prepareOutputModel(Model inputModel)
	{
		Model model = super.prepareOutputModel(inputModel);
		model.setNsPrefix("hello", Vocab.NS);
		return model;
	}

	public void processInput(Resource input, Resource output)
	{
		String name = input.getProperty(FOAF.name).getString();
		output.addProperty(Vocab.greeting, String.format("Hello, %s!", name), XSDDatatype.XSDstring);
	}

	private static class Vocab
	{
		private static String NS = "http://sadiframework.org/examples/hello.owl#";
		private static Model m_model = ModelFactory.createDefaultModel();
		public static Property greeting = m_model.createProperty(NS + "greeting");
	}
}
