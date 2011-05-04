package ca.wilkinsonlab.sadi.service.example;

import java.util.Collection;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

import com.hp.hpl.jena.rdf.model.Resource;

public class UniProtInfoServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return UniProtInfoServiceServletTest.class.getResourceAsStream("/uniprotInfo-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return UniProtInfoServiceServletTest.class.getResourceAsStream("/uniprotInfo-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return null;
	}

	@Override
	protected Collection<Resource> getInputNodes()
	{
		return getInputModel().listSubjects().toList();
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/uniprotInfo";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/uniprotInfo";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new UniProtInfoServiceServlet();
	}
}
