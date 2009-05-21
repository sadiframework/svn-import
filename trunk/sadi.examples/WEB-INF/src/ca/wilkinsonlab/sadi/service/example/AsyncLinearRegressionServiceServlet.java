package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.regression.SimpleRegression;

import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class AsyncLinearRegressionServiceServlet extends AsynchronousServiceServlet
{
	private static final Log log = LogFactory.getLog(AsyncLinearRegressionServiceServlet.class);

	private final Property ELEMENT;
	private final Property X;
	private final Property Y;
	private final Resource LINEAR_REGRESSION_MODEL;
	private final Property HAS_REGRESSION_MODEL;
	private final Property SLOPE;
	private final Property INTERCEPT;
	
	public AsyncLinearRegressionServiceServlet()
	{
		super();

		ELEMENT = ontologyModel.getProperty("http://sadiframework.org/examples/regression.owl#element");
		X = ontologyModel.getProperty("http://sadiframework.org/examples/regression.owl#x");
		Y = ontologyModel.getProperty("http://sadiframework.org/examples/regression.owl#y");
		LINEAR_REGRESSION_MODEL = ontologyModel.getResource("http://sadiframework.org/examples/regression.owl#LinearRegressionModel");
		HAS_REGRESSION_MODEL = ontologyModel.getProperty("http://sadiframework.org/examples/regression.owl#hasRegressionModel");
		SLOPE = ontologyModel.getProperty("http://sadiframework.org/examples/regression.owl#slope");
		INTERCEPT = ontologyModel.getProperty("http://sadiframework.org/examples/regression.owl#intercept");
	}
	
	public void processInput(Resource input, Resource output)
	{
		log.trace("processing input " + input);
		SimpleRegression regressionModel = new SimpleRegression();
		for (StmtIterator statements = input.listProperties(ELEMENT); statements.hasNext(); ) {
			Statement statement = statements.nextStatement();
			Resource pair = statement.getResource();
			log.trace("found list item " + pair);
			regressionModel.addData(pair.getProperty(X).getDouble(), pair.getProperty(Y).getDouble());
		}
		
		double slope = regressionModel.getSlope();
		double intercept = regressionModel.getIntercept();
		Resource linearRegressionModel = output.getModel().createResource(LINEAR_REGRESSION_MODEL);
		linearRegressionModel.addLiteral(SLOPE, slope);
		linearRegressionModel.addLiteral(INTERCEPT, intercept);
		output.addProperty(HAS_REGRESSION_MODEL, linearRegressionModel);
		
		log.trace("waiting 5 seconds to facilitate debugging");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			log.warn(e);
		}
	}
}
