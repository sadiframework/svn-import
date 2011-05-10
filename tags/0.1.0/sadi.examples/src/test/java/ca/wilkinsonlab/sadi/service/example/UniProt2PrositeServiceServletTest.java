package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class UniProt2PrositeServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return UniProt2PrositeServiceServletTest.class.getResourceAsStream("/uniprot2prosite-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return UniProt2PrositeServiceServletTest.class.getResourceAsStream("/uniprot2prosite-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://purl.uniprot.org/uniprot/P86277";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/uniprot2prosite";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/uniprot2prosite";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new UniProt2PrositeServiceServlet();
	}
}
