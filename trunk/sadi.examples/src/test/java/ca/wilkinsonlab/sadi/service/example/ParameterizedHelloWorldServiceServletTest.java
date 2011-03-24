package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class ParameterizedHelloWorldServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return ParameterizedHelloWorldServiceServletTest.class.getResourceAsStream("/hello-param-input1.rdf");
//		return ParameterizedHelloWorldServiceServletTest.class.getResourceAsStream("/hello-param-input2.rdf");
//		return ParameterizedHelloWorldServiceServletTest.class.getResourceAsStream("/hello-param-input3.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return ParameterizedHelloWorldServiceServletTest.class.getResourceAsStream("/hello-param-output1.rdf");
//		return ParameterizedHelloWorldServiceServletTest.class.getResourceAsStream("/hello-param-output2.rdf");
//		return ParameterizedHelloWorldServiceServletTest.class.getResourceAsStream("/hello-param-output3.rdf");
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
