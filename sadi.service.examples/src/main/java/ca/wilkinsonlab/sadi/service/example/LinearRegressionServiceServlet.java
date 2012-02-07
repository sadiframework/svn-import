package ca.wilkinsonlab.sadi.service.example;

import java.util.Iterator;

import org.apache.commons.math.stat.regression.SimpleRegression;

import ca.wilkinsonlab.sadi.service.annotations.Authoritative;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.vocab.Regression;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

@URI("http://sadiframework.org/examples/linear")
@Name("Linear regression")
@Description("Fits a least-squares regression line and returns the slope and intercept.")
@ContactEmail("info@sadiframework.org")
@InputClass("http://sadiframework.org/examples/regression.owl#InputClass")
@OutputClass("http://sadiframework.org/examples/regression.owl#OutputClass")
@Authoritative(true)
@TestCases({
		@TestCase(
				input = "http://sadiframework.org/examples/t/linear.input.1.rdf", 
				output = "http://sadiframework.org/examples/t/linear.output.1.rdf"
		),
		@TestCase(
				input = "http://sadiframework.org/examples/t/linear.input.2.rdf", 
				output = "http://sadiframework.org/examples/t/linear.output.2.rdf"
		)}
)
public class LinearRegressionServiceServlet extends SimpleSynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
//	private static final Log log = LogFactory.getLog(LinearRegressionServiceServlet.class);

	@Override
	protected Model createOutputModel()
	{
		Model model = super.createOutputModel();
		model.setNsPrefix("regress", "http://sadiframework.org/examples/regression.owl#");
		return model;
	}
	
	public void processInput(Resource input, Resource output)
	{
		SimpleRegression regressionModel = new SimpleRegression();
		for (Iterator<Resource> pairs = RdfUtils.getPropertyValues(input, SIO.has_member, Regression.PairedValue); pairs.hasNext(); ) {
			Resource pair = pairs.next();
			Resource x = RdfUtils.getPropertyValue(pair, SIO.has_attribute, Regression.X);
			Resource y = RdfUtils.getPropertyValue(pair, SIO.has_attribute, Regression.Y);
			regressionModel.addData(x.getRequiredProperty(SIO.has_value).getDouble(), 
					y.getRequiredProperty(SIO.has_value).getDouble());
		}
		double m = regressionModel.getSlope();
		double b = regressionModel.getIntercept();
		Resource linearRegressionModel = output.getModel().createResource(Regression.LinearRegressionModel);
		linearRegressionModel.addLiteral(Regression.slope, m);
		linearRegressionModel.addLiteral(Regression.intercept, b);
		output.addProperty(Regression.hasRegressionModel, linearRegressionModel);
	}
}
