package ca.wilkinsonlab.sadi.service.example;

import java.util.ArrayList;
import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Resource;

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
		// this method shouldn't be called because we're overriding getInputNodes...
		throw new UnsupportedOperationException();
//		return "http://sadiframework.org/examples/input/regression1";
	}
	
	@Override
	protected Collection<Resource> getInputNodes()
	{
		Collection<Resource> inputNodes = new ArrayList<Resource>();
		for (int i=1; i<=2; ++i) {
			inputNodes.add(getInputModel().getResource(String.format("http://sadiframework.org/examples/input/regression%d", i)));
		}
		return inputNodes;
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/linear-async";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/linear-async";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new AsyncLinearRegressionServiceServlet();
	}
}
