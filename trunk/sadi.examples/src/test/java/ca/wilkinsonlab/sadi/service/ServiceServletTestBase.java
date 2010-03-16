package ca.wilkinsonlab.sadi.service;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.client.ServiceImpl;
import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.utils.ModelDiff;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.vocabulary.RDF;

public abstract class ServiceServletTestBase extends TestCase
{
	private static final Log log = LogFactory.getLog(ServiceServletTestBase.class);
	static String uriPrefix = "http://sadiframework.org/examples/";
	static String altPrefix = "http://localhost:8080/sadi-examples/";
	
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
	
	protected ServiceImpl getLocalServiceInstance() throws SADIException
	{
		return new ServiceImpl(getLocalServiceURL());
	}
	
	protected Collection<Resource> getInputNodes()
	{
		return Collections.singleton(getInputModel().createResource(getInputURI()));
	}
	
	protected abstract Object getInput();
	
	protected abstract Object getExpectedOutput();

	protected abstract String getInputURI();
	
	protected abstract ServiceServlet getServiceServletInstance();
	
	protected abstract String getServiceURI();
	
	protected abstract String getLocalServiceURL();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		System.setProperty("sadi.service.ignoreForcedURL", "true");
		LocationMapper.get().addAltPrefix(uriPrefix, altPrefix);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		System.setProperty("sadi.service.ignoreForcedURL", null);
		LocationMapper.get().removeAltPrefix(uriPrefix);
	}
	
	@Test
	public void testInputInstance() throws Exception
	{
		/* inputs to services must be explicitly typed, so there's not much
		 * to do here now...
		 */
		OntClass inputClass = getServiceServletInstance().inputClass;
		for (Resource inputNode: getInputNodes()) {
			assertTrue(String.format("individual %s is not an instance of class %s", inputNode, inputClass),
					inputNode.hasProperty(RDF.type, inputClass));
		}
	}
	
//	@Test
//	public void testProcessInputModel() throws Exception
//	{
//		ServiceServlet serviceServletInstance = getServiceServletInstance();
//		Model inputModel = getInputModel();
//		Model expectedOutputModel = getExpectedOutputModel();
//		Model actualOutputModel = serviceServletInstance.processInput(inputModel);
//		RdfService.resolveAsynchronousData(actualOutputModel);
//
//		if (log.isTraceEnabled()) {
//			log.trace(RdfUtils.logStatements("Input", inputModel));
//			log.trace(RdfUtils.logStatements("Expected output", expectedOutputModel));
//			log.trace(RdfUtils.logStatements("Actual output", actualOutputModel));
//		}
//		
//		assertTrue(String.format("%s.processInput does not produce expected output", serviceServletInstance.getClass().getSimpleName()),
//				actualOutputModel.isIsomorphicWith(expectedOutputModel));
//	}
	
	@Test
	public void testServiceInvocation() throws Exception
	{
		ServiceImpl regressionService = getLocalServiceInstance();
		Model expectedOutputModel = getExpectedOutputModel();
		Model actualOutputModel = RdfUtils.triplesToModel( regressionService.invokeService(getInputNodes()) );
		
		if (log.isTraceEnabled()) {
			Model inputModel = ModelFactory.createDefaultModel();
			for (Resource inputNode: getInputNodes()) {
				inputModel.add(OwlUtils.getMinimalModel(inputNode, regressionService.getInputClass()));
			}
			try {
				inputModel.write( new FileOutputStream( String.format( "target/%s.input.rdf", getClass().getSimpleName() ) ) );
				expectedOutputModel.write( new FileOutputStream( String.format( "target/%s.expected.rdf", getClass().getSimpleName() ) ) );
				actualOutputModel.write( new FileOutputStream( String.format( "target/%s.output.rdf", getClass().getSimpleName() ) ) );
			} catch (Exception e) {
				log.error("error writing models", e);
			}
		}
		
		if (!actualOutputModel.isIsomorphicWith(expectedOutputModel)) {
			ModelDiff diff = ModelDiff.diff(expectedOutputModel, actualOutputModel);
			StringBuffer buf = new StringBuffer(String.format("service call to %s does not produce expected output\n", getLocalServiceURL()));
			buf.append(RdfUtils.logStatements("", diff.inXnotY));
			buf.append(RdfUtils.logStatements("    ", diff.inBoth));
			buf.append(RdfUtils.logStatements("        ", diff.inYnotX));
			fail(buf.toString());
		}
	}
}
