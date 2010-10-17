package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class UniProt2KeggServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return UniProt2KeggServiceServletTest.class.getResourceAsStream("/uniprot2kegg-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return UniProt2KeggServiceServletTest.class.getResourceAsStream("/uniprot2kegg-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://purl.uniprot.org/uniprot/P04637";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/uniprot2kegg";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi-examples/uniprot2kegg";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new UniProt2KeggServiceServlet();
	}
}
