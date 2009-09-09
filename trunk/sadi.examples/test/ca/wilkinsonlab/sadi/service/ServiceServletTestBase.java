package ca.wilkinsonlab.sadi.service;

import java.io.InputStream;
import java.net.MalformedURLException;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ca.wilkinsonlab.sadi.rdf.RdfService;
import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.service.example.LocalServiceWrapper;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public abstract class ServiceServletTestBase extends TestCase
{
	private static final Log log = LogFactory.getLog(ServiceServletTestBase.class);
	
	protected Model getInputModel()
	{
		Model model = ModelFactory.createDefaultModel();
		Object input = getInput();
		if (input instanceof InputStream)
			model.read((InputStream)input, "");
		else if (input instanceof String)
			model.read((String)input);
		else
			throw new IllegalArgumentException("getInput() must return an InputStream or a String");
		return model;
	}
	
	protected Model getExpectedOutputModel()
	{
		Model model = ModelFactory.createDefaultModel();
		Object output = getExpectedOutput();
		if (output instanceof InputStream)
			model.read((InputStream)output, "");
		else if (output instanceof String)
			model.read((String)output);
		else
			throw new IllegalArgumentException("getOutput() must return an InputStream or a String");
		return model;
	}
	
	protected RdfService getLocalServiceInstance() throws MalformedURLException
	{
		return new LocalServiceWrapper(getServiceURI(), getLocalServiceURL());
	}
	
	protected Resource getInputNode()
	{
		return getInputModel().createResource(getInputURI());
	}
	
	protected abstract Object getInput();
	
	protected abstract Object getExpectedOutput();

	protected abstract String getInputURI();
	
	protected abstract ServiceServlet getServiceServletInstance();
	
	protected abstract String getServiceURI();
	
	protected abstract String getLocalServiceURL();
	
	@Test
	public void testInputInstance() throws Exception
	{
		ServiceServlet serviceServletInstance = getServiceServletInstance();
		Model inputModel = getInputModel();
		OntModel infModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, inputModel);
		assertTrue(String.format("individual %s is not an instance of class %s", getInputNode(), serviceServletInstance.inputClass),
				infModel.getIndividual(getInputNode().getURI()).hasOntClass(serviceServletInstance.inputClass));
	}
	
	@Test
	public void testProcessInputModel() throws Exception
	{
		ServiceServlet serviceServletInstance = getServiceServletInstance();
		Model inputModel = getInputModel();
		Model expectedOutputModel = getExpectedOutputModel();
		Model actualOutputModel = serviceServletInstance.processInput(inputModel);

		if (log.isTraceEnabled()) {
			log.trace(RdfUtils.logStatements("Input", inputModel));
			log.trace(RdfUtils.logStatements("Expected output", expectedOutputModel));
			log.trace(RdfUtils.logStatements("Actual output", actualOutputModel));
		}
		
		assertTrue(String.format("%s.processInput does not produce expected output", serviceServletInstance.getClass().getSimpleName()),
				actualOutputModel.isIsomorphicWith(expectedOutputModel));
	}
	
	@Test
	public void testServiceInvocation() throws Exception
	{
		RdfService regressionService = getLocalServiceInstance();
		Resource inputNode = getInputNode();
		Model expectedOutputModel = getExpectedOutputModel();
		Model actualOutputModel = RdfUtils.triplesToModel( regressionService.invokeService(inputNode) );
		
		if (log.isTraceEnabled()) {
			log.trace(RdfUtils.logStatements("Minimal input", OwlUtils.getMinimalModel(inputNode, regressionService.getInputClass())));
			log.trace(RdfUtils.logStatements("Expected output", expectedOutputModel));
			log.trace(RdfUtils.logStatements("Actual output", actualOutputModel));
		}
		
		assertTrue(String.format("service call to %s does not produce expected output", getLocalServiceURL()),
				actualOutputModel.isIsomorphicWith(expectedOutputModel));
	}
}
