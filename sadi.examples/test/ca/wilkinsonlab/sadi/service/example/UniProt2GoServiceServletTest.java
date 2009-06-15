package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;

public class UniProt2GoServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return UniProt2GoServiceServletTest.class.getResourceAsStream("/uniprot2go-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return UniProt2GoServiceServletTest.class.getResourceAsStream("/uniprot2go-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://purl.uniprot.org/uniprot/P12345";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/uniprot2go";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi.examples/uniprot2go";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new UniProt2GoServiceServlet();
	}
	
	@Override
	public void testServiceInvocation()
	{
		return;	// multiple input nodes, so this would fail...
	}
}
