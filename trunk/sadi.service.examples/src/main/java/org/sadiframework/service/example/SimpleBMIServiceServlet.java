package org.sadiframework.service.example;

import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.Description;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.service.annotations.URI;
import org.sadiframework.service.simple.SimpleSynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

@URI("http://sadiframework.org/examples/simpleBMI")
@Name("simpleBMI")
@Description("Calculates body mass index using curried datatype properties for height/weight")
@ContactEmail("info@sadiframework.org")
@InputClass("http://sadiframework.org/examples/bmi.owl#SimpleInputClass")
@OutputClass("http://sadiframework.org/examples/bmi.owl#OutputClass")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/simpleBMI.input.1.rdf",
				output = "http://sadiframework.org/examples/t/simpleBMI.output.1.rdf"
		)
)
public class SimpleBMIServiceServlet extends SimpleSynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
//	private static final Log log = LogFactory.getLog(BMIServiceServlet.class);

	protected Model prepareOutputModel(Model inputModel)
	{
		Model model = super.prepareOutputModel(inputModel);
		model.setNsPrefix("bmi", Vocab.LOCAL_NS);
		return model;
	}
	
	public void processInput(Resource input, Resource output)
	{
		output.addLiteral(Vocab.BMI, getWeightInKg(input)/Math.pow(getHeightInM(input), 2));
	}

	/**
	 * Return the height in centimeters, assuming the input resource conforms
	 * to the MGED ontology for measurements.
	 * @param input
	 */
	public double getWeightInKg(Resource input)
	{
		String value = input.getRequiredProperty(Vocab.weight_kg).getString();
		return Double.parseDouble(value);
	}
	
	/**
	 * Return the height in meters, assuming the input resource conforms to the
	 * MGED ontology for measurements.
	 * @param input
	 */
	public double getHeightInM(Resource input)
	{
		String value = input.getRequiredProperty(Vocab.height_m).getString();
		return Double.parseDouble(value);
	}

	private static class Vocab
	{
		public static String LOCAL_NS = "http://sadiframework.org/examples/bmi.owl#";
		public static Property height_m = ResourceFactory.createProperty(LOCAL_NS, "height_m");
		public static Property weight_kg = ResourceFactory.createProperty(LOCAL_NS, "weight_kg");
		public static Property BMI = ResourceFactory.createProperty(LOCAL_NS + "BMI");
	}
}
