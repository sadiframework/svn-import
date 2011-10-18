package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;

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
@TestCases({
	@TestCase(
			input = "http://sadiframework.org/examples/t/hello-input.rdf", 
			output = "http://sadiframework.org/examples/t/hello-output.rdf"
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
