package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

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
		return "http://purl.uniprot.org/uniprot/P12345";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/uniprotInfo";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi-examples/uniprotInfo";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new UniProtInfoServiceServlet();
	}
}
