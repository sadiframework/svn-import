package ca.wilkinsonlab.sadi.service.example;

import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class EntrezGene2UniProtServiceServletTest extends ServiceServletTestBase 
{
	@Override
	protected Object getInput()
	{
		return EntrezGene2UniProtServiceServletTest.class.getResourceAsStream("/entrezGene2Uniprot-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return EntrezGene2UniProtServiceServletTest.class.getResourceAsStream("/entrezGene2Uniprot-output.rdf");
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
		return "http://sadiframework.org/examples/entrezGene2Uniprot";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/entrezGene2Uniprot";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new EntrezGene2UniProtServiceServlet();
	}
}
