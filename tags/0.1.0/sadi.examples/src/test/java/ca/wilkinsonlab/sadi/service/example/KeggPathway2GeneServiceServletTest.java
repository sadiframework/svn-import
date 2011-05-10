package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class KeggPathway2GeneServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return KeggPathway2GeneServiceServletTest.class.getResourceAsStream("/keggPathway2Gene-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return KeggPathway2GeneServiceServletTest.class.getResourceAsStream("/keggPathway2Gene-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://lsrn.org/KEGG_PATHWAY:hsa00232";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/keggPathway2Gene";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/keggPathway2Gene";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new KeggPathway2GeneServiceServlet();
	}
}
