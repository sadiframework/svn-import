package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class UniProt2PubmedServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return UniProt2PubmedServiceServletTest.class.getResourceAsStream("/uniprot2pubmed-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return UniProt2PubmedServiceServletTest.class.getResourceAsStream("/uniprot2pubmed-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://purl.uniprot.org/uniprot/Q8J016";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/uniprot2pubmed";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi.examples/uniprot2pubmed";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new UniProt2PubmedServiceServlet();
	}
}
