package ca.wilkinsonlab.sadi.service.example;

import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

public class UniProt2GoServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return UniProt2GoServiceServletTest.class.getResourceAsStream("/uniprot2go-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return UniProt2GoServiceServletTest.class.getResourceAsStream("/uniprot2go-output.rdf");
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
		return "http://sadiframework.org/examples/uniprot2go";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/uniprot2go";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new UniProt2GoServiceServlet();
	}
}
