package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class AsyncParameterizedHelloWorldServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return AsyncParameterizedHelloWorldServiceServletTest.class.getResourceAsStream("/hello-param-input1.rdf");
//		return ParameterizedHelloWorldServiceServletTest.class.getResourceAsStream("/hello-param-input2.rdf");
//		return ParameterizedHelloWorldServiceServletTest.class.getResourceAsStream("/hello-param-input3.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return AsyncParameterizedHelloWorldServiceServletTest.class.getResourceAsStream("/hello-param-output1.rdf");
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
		return "http://sadiframework.org/examples/hello-param-async";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/hello-param-async";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new AsyncParameterizedHelloWorldServiceServlet();
	}
}
