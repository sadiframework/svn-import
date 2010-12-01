package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Resource;

@SuppressWarnings("serial")
public class LinearRegressionServiceServlet extends SimpleSynchronousServiceServlet
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(LinearRegressionServiceServlet.class);
	
	public void processInput(Resource input, Resource output)
	{
		RegressionUtils.processInput(input, output);
	}
}
