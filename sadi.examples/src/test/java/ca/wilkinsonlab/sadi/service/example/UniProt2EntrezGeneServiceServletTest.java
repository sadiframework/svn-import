package ca.wilkinsonlab.sadi.service.example;

import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class UniProt2EntrezGeneServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return UniProt2EntrezGeneServiceServletTest.class.getResourceAsStream("/uniprot2EntrezGene-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return UniProt2EntrezGeneServiceServletTest.class.getResourceAsStream("/uniprot2EntrezGene-output.rdf");
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
		return "http://sadiframework.org/examples/uniprot2EntrezGene";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8080/sadi-examples/uniprot2EntrezGene";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new UniProt2EntrezGeneServiceServlet();
	}
}
