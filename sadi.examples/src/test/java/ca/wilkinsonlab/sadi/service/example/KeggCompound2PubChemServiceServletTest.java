package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class KeggCompound2PubChemServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return KeggCompound2PubChemServiceServletTest.class.getResourceAsStream("/keggCompound2PubChem-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return KeggCompound2PubChemServiceServletTest.class.getResourceAsStream("/keggCompound2PubChem-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return "http://lsrn.org/KEGG_COMPOUND:cpd:C00791";
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/keggCompound2PubChem";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi-examples/keggCompound2PubChem";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new KeggCompound2PubChemServiceServlet();
	}
}
