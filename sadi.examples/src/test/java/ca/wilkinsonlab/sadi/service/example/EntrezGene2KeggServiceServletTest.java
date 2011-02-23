package ca.wilkinsonlab.sadi.service.example;

import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class EntrezGene2KeggServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return EntrezGene2KeggServiceServletTest.class.getResourceAsStream("/entrezGene2Kegg-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return EntrezGene2KeggServiceServletTest.class.getResourceAsStream("/entrezGene2Kegg-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return null;
	}

	@Override
	protected Collection<Resource> getInputNodes()
	{
		return getInputModel().listSubjects().toList();
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/entrezGene2Kegg";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi-examples/entrezGene2Kegg";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new EntrezGene2KeggServiceServlet();
	}
}
