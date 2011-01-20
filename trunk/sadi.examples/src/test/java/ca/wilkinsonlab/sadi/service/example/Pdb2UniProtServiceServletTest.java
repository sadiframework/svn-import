package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class Pdb2UniProtServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return Pdb2UniProtServiceServletTest.class.getResourceAsStream("/pdb2uniprot-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return Pdb2UniProtServiceServletTest.class.getResourceAsStream("/pdb2uniprot-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://lsrn.org/PDB:3IWT";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/pdb2uniprot";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi-examples/pdb2uniprot";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new Pdb2UniProtServiceServlet();
	}
}
