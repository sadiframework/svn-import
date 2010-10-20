package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class BlastUniProtByIdServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return BlastUniProtByIdServiceServletTest.class.getResourceAsStream("/blastUniprotById-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return BlastUniProtByIdServiceServletTest.class.getResourceAsStream("/blastUniprotById-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://purl.uniprot.org/uniprot/P12345";
	}
	
	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/blastUniprotById";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi-examples/blastUniprotById";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new BlastUniProtByIdServiceServlet();
	}
}
