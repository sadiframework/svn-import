package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class BlastUniProtServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return BlastUniProtServiceServletTest.class.getResourceAsStream("/blastUniprot-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return BlastUniProtServiceServletTest.class.getResourceAsStream("/blastUniprot-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://purl.uniprot.org/uniprot/P12345";
	}
	
	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/blastUniprot";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/blastUniprot";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new BlastUniProtServiceServlet();
	}
}
