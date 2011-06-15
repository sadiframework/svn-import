package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Resource;

@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/regression-input.rdf", 
				output = "http://sadiframework.org/examples/t/regression-output.rdf"
		)
)
public class LinearRegressionServiceServlet extends SimpleSynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
//	private static final Log log = LogFactory.getLog(LinearRegressionServiceServlet.class);
	
	public void processInput(Resource input, Resource output)
	{
		RegressionUtils.processInput(input, output);
	}
}
