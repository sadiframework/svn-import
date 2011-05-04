package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class KeggGene2PathwayServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return KeggGene2PathwayServiceServletTest.class.getResourceAsStream("/keggGene2Pathway-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return KeggGene2PathwayServiceServletTest.class.getResourceAsStream("/keggGene2Pathway-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://lsrn.org/KEGG:hsa:50616";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/keggGene2Pathway";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/keggGene2Pathway";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new KeggGene2PathwayServiceServlet();
	}
	
}
