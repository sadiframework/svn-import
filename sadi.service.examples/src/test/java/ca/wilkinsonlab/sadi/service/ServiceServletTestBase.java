package ca.wilkinsonlab.sadi.service;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.ServiceFactory;
import ca.wilkinsonlab.sadi.client.ServiceImpl;
import ca.wilkinsonlab.sadi.client.ServiceInvocationException;
import ca.wilkinsonlab.sadi.client.testing.ServiceTester;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.utils.ModelDiff;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.util.ResourceUtils;

public abstract class ServiceServletTestBase extends TestCase
{
	private static final Log log = LogFactory.getLog(ServiceServletTestBase.class);
	
	public static final String PRODUCTION_URI_PREFIX = "http://sadiframework.org/examples/";
	public static final String LOCAL_URI_PREFIX = "http://localhost:8180/sadi-examples/";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		System.setProperty("sadi.service.ignoreForcedURL", "true");
		LocationMapper.get().addAltPrefix(PRODUCTION_URI_PREFIX, LOCAL_URI_PREFIX);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		System.setProperty("sadi.service.ignoreForcedURL", null);
		LocationMapper.get().removeAltPrefix(PRODUCTION_URI_PREFIX);
	}
	
	private Iterable<ca.wilkinsonlab.sadi.client.testing.TestCase> getTestCases(ServiceImpl service)
	{
		MyGridServiceOntologyHelper helper = new MyGridServiceOntologyHelper();
		for (RDFNode testCaseNode: helper.getTestCasePath().getValuesRootedAt(service.getServiceModel().getResource(service.getURI()))) {
			Resource testCaseResource = testCaseNode.asResource();
			RDFNode input = helper.getTestInputPath().getValuesRootedAt(testCaseResource).iterator().next();
			if (input.isURIResource() && input.asResource().getURI().startsWith(PRODUCTION_URI_PREFIX)) {
				String newURI = input.asResource().getURI().replaceAll(PRODUCTION_URI_PREFIX, LOCAL_URI_PREFIX);
				input = ResourceUtils.renameResource(input.asResource(), newURI);
			}
			RDFNode output = helper.getTestOutputPath().getValuesRootedAt(testCaseResource).iterator().next();
			if (output.isURIResource() && output.asResource().getURI().startsWith(PRODUCTION_URI_PREFIX)) {
				String newURI = output.asResource().getURI().replaceAll(PRODUCTION_URI_PREFIX, LOCAL_URI_PREFIX);
				output = ResourceUtils.renameResource(output.asResource(), newURI);
			}
		}
		return service.getTestCases();
	}
	
	@Test
	public void testLocalService() throws Exception
	{
		ServiceImpl service = getLocalServiceInstance();
		String serviceFileName = getServiceFileNameBase(service);
		int i=0;
		int fail=0;
		for (ca.wilkinsonlab.sadi.client.testing.TestCase testCase: getTestCases(service)) {
			++i;
			log.info(String.format("testing case %d", i));
			writeModel(testCase.getInputModel(), String.format("target/%s.input.%d", serviceFileName, i));
			writeModel(testCase.getExpectedOutputModel(), String.format("target/%s.expected.%d", serviceFileName, i));
			Model outputModel;
			try {
				log.debug("calling service");
				outputModel = service.invokeServiceUnparsed(testCase.getInputModel());
//			} catch (IOException e) {
			} catch (ServiceInvocationException e) {
				log.error(String.format("error invoking service %s: %s", service, e.getMessage()), e);
				++fail;
				continue;
			}
			writeModel(outputModel, String.format("target/%s.output.%d", serviceFileName, i));
			log.debug("comparing expected output to actual output");
			if (!compareOutput(outputModel, testCase.getExpectedOutputModel())) {
				++fail;
				continue;
			}
//			try {
//				sanityCheckOutput(service, outputModel);
//			} catch (SADIException e) {
//				log.warn(e.getMessage());
//			}
		}
		if (i == 0)
			fail("no test cases");
		if (fail > 0)
			fail(String.format("failed %d/%d test cases", fail, i));
	}
	
	private String getServiceFileNameBase(ServiceImpl service)
	{
		String[] elements = service.getURI().split("/");
		return elements[elements.length-1];
	}
	
	private void writeModel(Model model, String filename)
	{
		filename = filename.concat(".rdf");
		try {
			model.write(new FileOutputStream(filename), "RDF/XML-ABBREV");
		} catch (FileNotFoundException e) {
			log.error(String.format("error writing to %s", filename), e);
		}
	}
	
	protected boolean compareOutput(Model output, Model expected)
	{
		if (output.isIsomorphicWith(expected)) {
			return true;
		} else {
			ModelDiff diff = ModelDiff.diff(output, expected);
			if (!diff.inXnotY.isEmpty())
				log.error("service output had unexpected statements:\n" + RdfUtils.logStatements("\t", diff.inXnotY));
			if (!diff.inYnotX.isEmpty())
				log.error("service output had missing statements:\n" + RdfUtils.logStatements("\t", diff.inYnotX));
			return false;
		}
	}
	
	protected void sanityCheckOutput(ServiceImpl service, Model output) throws SADIException
	{
		log.debug("sanity checking output");
		ServiceTester.sanityCheckOutput(service, output);
	}
	
	protected String getLocalServiceURL()
	{
		URI annotation = getClass().getAnnotation(URI.class);
		return annotation == null ? null : annotation.value();
	}
	
	protected ServiceImpl getLocalServiceInstance() throws SADIException
	{
//		return new ServiceImpl(getLocalServiceURL());
		return (ServiceImpl)ServiceFactory.createService(getLocalServiceURL());
	}
}
