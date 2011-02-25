package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class KeggGene2EntrezGeneServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return KeggGene2EntrezGeneServiceServletTest.class.getResourceAsStream("/keggGene2EntrezGene-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return KeggGene2EntrezGeneServiceServletTest.class.getResourceAsStream("/keggGene2EntrezGene-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://lsrn.org/KEGG:hsa:7157";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/keggGene2EntrezGene";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi-examples/keggGene2EntrezGene";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new KeggGene2EntrezGeneServiceServlet();
	}
}
