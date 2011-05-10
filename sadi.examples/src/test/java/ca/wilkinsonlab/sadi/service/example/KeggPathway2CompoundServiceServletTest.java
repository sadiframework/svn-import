package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class KeggPathway2CompoundServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return KeggPathway2CompoundServiceServletTest.class.getResourceAsStream("/keggPathway2Compound-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return KeggPathway2CompoundServiceServletTest.class.getResourceAsStream("/keggPathway2Compound-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://lsrn.org/KEGG_PATHWAY:hsa00232";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/keggPathway2Compound";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/keggPathway2Compound";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new KeggPathway2CompoundServiceServlet();
	}
}
