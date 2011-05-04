package ca.wilkinsonlab.sadi.service.example;

import java.util.Collection;

import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class BMIServiceServletTest extends ServiceServletTestBase
{
	@Override
	protected Object getInput()
	{
		return BMIServiceServletTest.class.getResourceAsStream("/bmi-input.rdf");
	}

	@Override
	protected Object getExpectedOutput()
	{
		return BMIServiceServletTest.class.getResourceAsStream("/bmi-output.rdf");
	}

	@Override
	protected String getInputURI()
	{
		return null;
	}

	@Override
	protected Collection<Resource> getInputNodes()
	{
		return getInputModel().listResourcesWithProperty(RDF.type, ResourceFactory.createResource("http://sadiframework.org/examples/bmi.owl#InputClass")).toList();
	}

	@Override
	protected String getServiceURI()
	{
		return "http://sadiframework.org/examples/calculateBMI";
	}

	@Override
	protected String getLocalServiceURL()
	{
		return "http://localhost:8180/sadi-examples/calculateBMI";
	}

	@Override
	protected ServiceServlet getServiceServletInstance()
	{
		return new ErmineJServiceServlet();
	}
}
