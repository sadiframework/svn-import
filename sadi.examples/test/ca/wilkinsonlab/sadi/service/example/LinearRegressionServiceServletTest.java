package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;

public class LinearRegressionServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return LinearRegressionServiceServletTest.class.getResourceAsStream("/regression-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return LinearRegressionServiceServletTest.class.getResourceAsStream("/regression-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://sadiframework.org/examples/input/regression1";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/linear";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi.examples/linear";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new LinearRegressionServiceServlet();
	}
}
