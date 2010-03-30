package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class HelloWorldServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return HelloWorldServiceServletTest.class.getResourceAsStream("/hello-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return HelloWorldServiceServletTest.class.getResourceAsStream("/hello-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://sadiframework.org/examples/hello-input.rdf#1";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/hello";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi-examples/hello";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new LinearRegressionServiceServlet();
	}
}
