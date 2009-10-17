package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.regression.SimpleRegression;

import ca.wilkinsonlab.sadi.service.simple.SimpleAsynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

@SuppressWarnings("serial")
public class AsyncLinearRegressionServiceServlet extends SimpleAsynchronousServiceServlet
{
	private static final Log log = LogFactory.getLog(AsyncLinearRegressionServiceServlet.class);

	private final Property element = ResourceFactory.createProperty("http://sadiframework.org/examples/regression.owl#element");
	private final Property x = ResourceFactory.createProperty("http://sadiframework.org/examples/regression.owl#x");
	private final Property y = ResourceFactory.createProperty("http://sadiframework.org/examples/regression.owl#y");
	private final Resource LinearRegressionModel = ResourceFactory.createResource("http://sadiframework.org/examples/regression.owl#LinearRegressionModel");
	private final Property hasRegressionModel = ResourceFactory.createProperty("http://sadiframework.org/examples/regression.owl#hasRegressionModel");
	private final Property slope = ResourceFactory.createProperty("http://sadiframework.org/examples/regression.owl#slope");
	private final Property intercept = ResourceFactory.createProperty("http://sadiframework.org/examples/regression.owl#intercept");
	
	public AsyncLinearRegressionServiceServlet()
	{
		super();
	}
	
	public void processInput(Resource input, Resource output)
	{
		log.trace("processing input " + input);
		SimpleRegression regressionModel = new SimpleRegression();
		for (StmtIterator statements = input.listProperties(element); statements.hasNext(); ) {
			Statement statement = statements.nextStatement();
			Resource pair = statement.getResource();
			log.trace("found list item " + pair);
			regressionModel.addData(pair.getProperty(x).getDouble(), pair.getProperty(y).getDouble());
		}
		
		double m = regressionModel.getSlope();
		double x = regressionModel.getIntercept();
		Resource linearRegressionModel = output.getModel().createResource(LinearRegressionModel);
		linearRegressionModel.addLiteral(slope, m);
		linearRegressionModel.addLiteral(intercept, x);
		output.addProperty(hasRegressionModel, linearRegressionModel);
		
		log.info("waiting 5 seconds to facilitate debugging");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			log.warn(e);
		}
	}
}
