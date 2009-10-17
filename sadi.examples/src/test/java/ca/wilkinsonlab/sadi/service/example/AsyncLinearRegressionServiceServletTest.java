package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class AsyncLinearRegressionServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return AsyncLinearRegressionServiceServletTest.class.getResourceAsStream("/regression-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return AsyncLinearRegressionServiceServletTest.class.getResourceAsStream("/regression-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://sadiframework.org/examples/input/regression1";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/linear-async";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi.examples/linear-async";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new AsyncLinearRegressionServiceServlet();
	}
}
