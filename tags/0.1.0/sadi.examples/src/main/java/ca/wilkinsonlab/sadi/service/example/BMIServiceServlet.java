package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

@SuppressWarnings("serial")
public class BMIServiceServlet extends SimpleSynchronousServiceServlet
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(BMIServiceServlet.class);
	
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
		Resource measurement = input.getRequiredProperty(Vocab.weight).getResource();
		Resource units = measurement.getRequiredProperty(Vocab.units).getResource();
		String value = measurement.getRequiredProperty(Vocab.value).getString();
		if (units.equals(Vocab.kg))
			return Double.parseDouble(value);
		else
			throw new IllegalArgumentException("height measurement in unknown units");
	}
	/**
	 * Return the height in meters, assuming the input resource conforms to the
	 * MGED ontology for measurements.
	 * @param input
	 */
	public double getHeightInM(Resource input)
	{
		Resource measurement = input.getRequiredProperty(Vocab.height).getResource();
		Resource units = measurement.getRequiredProperty(Vocab.units).getResource();
		String value = measurement.getRequiredProperty(Vocab.value).getString();
		if (units.equals(Vocab.cm))
			return Double.parseDouble(value)/100;
		else if (units.equals(Vocab.m))
			return Double.parseDouble(value);
		else
			throw new IllegalArgumentException("height measurement in unknown units");
	}
	
	@SuppressWarnings("unused")
	private static class Vocab
	{
		public static String LOCAL_NS = "http://sadiframework.org/examples/bmi.owl#";
		public static String GALEN_NS = "http://www.co-ode.org/galen/full-galen.owl#";
		public static String SCI_UNITS_NS = "http://sweet.jpl.nasa.gov/2.0/sciUnits.owl#";
		public static String MUO_NS = "http://id.fundacionctic.org/muo/muo-vocab.owl#";
		public static String UCUM_NS = "http://id.fundacionctic.org/muo/ucum-instances.owl#";
		public static String MGED_NS = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#";
		public static Property height = ResourceFactory.createProperty(MGED_NS + "has_height");
		public static Property weight = ResourceFactory.createProperty(MGED_NS + "has_mass");
		public static Property units = ResourceFactory.createProperty(MGED_NS + "has_units");
		public static Property value = ResourceFactory.createProperty(MGED_NS + "has_value");
		public static Property BMI = ResourceFactory.createProperty(LOCAL_NS + "BMI");
		public static Resource kg = ResourceFactory.createResource(MGED_NS + "kg");
		public static Resource cm = ResourceFactory.createResource(MGED_NS + "cm");
		public static Resource m = ResourceFactory.createResource(MGED_NS + "m");
	}
}
