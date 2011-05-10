package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class UniProt2PdbServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return UniProt2PdbServiceServletTest.class.getResourceAsStream("/uniprot2pdb-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return UniProt2PdbServiceServletTest.class.getResourceAsStream("/uniprot2pdb-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://purl.uniprot.org/uniprot/P47989";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/uniprot2pdb";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/uniprot2pdb";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new UniProt2PdbServiceServlet();
	}
}
