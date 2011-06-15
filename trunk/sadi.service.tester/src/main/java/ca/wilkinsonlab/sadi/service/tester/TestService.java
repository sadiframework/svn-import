package ca.wilkinsonlab.sadi.service.tester;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

import org.apache.axis.utils.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceImpl;
import ca.wilkinsonlab.sadi.client.testing.ServiceTester;
import ca.wilkinsonlab.sadi.client.testing.TestCase;
import ca.wilkinsonlab.sadi.utils.ModelDiff;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * A Maven plugin to test a SADI service.
 * @author Luke McCarthy
 * @goal test-service
 */
public class TestService extends AbstractMojo
{
	/**
	 * The URL of the service to be tested.
	 * This parameter is required.
	 * @parameter expression="${serviceURL}"
	 */
	private String serviceURL;
	private static final String SERVICE_URL_KEY = "serviceURL";
	
	/**
	 * The URL or local path of an input RDF document.
	 * This parameter is optional.
	 * @parameter expression="${input}" default-value=""
	 */
	private String inputPath;
	private static final String INPUT_KEY = "input";
	
	/**
	 * The URL or local path of the expected output RDF document corresponding
	 * to the input document specified above.
	 * This parameter is optional.
	 * @parameter expression="${expected}" default-value=""
	 */
	private String expectedPath;
	private static final String EXPECTED_OUTPUT_KEY = "expected";
	
	/**
	 * Expected properties:
	 *  serviceURL (required)
	 *  input (optional) input RDF URL or path to file
	 *  expected (optional) expected output RDF URL or path to file
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		Service service = initService();
		String serviceFileName = "";
		try {
			serviceFileName = URLEncoder.encode(service.getURI(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// this shouldn't happen...
			getLog().error(e.getMessage());
		}
		Collection<TestCase> testCases = ((ServiceImpl)service).getTestCases();
		if (!StringUtils.isEmpty(inputPath) && !StringUtils.isEmpty(expectedPath)) {
			testCases.add(new TestCase(inputPath, expectedPath));
		}
		if (testCases.isEmpty())
			throw new MojoFailureException("no test cases speciied in properties or service definition");
		int i=0;
		for (TestCase testCase: testCases) {
			++i;
			getLog().info(String.format("executing test case %d/%d", i, testCases.size()));
			writeModel(testCase.getInputModel(), String.format("target/%s.input.%d", serviceFileName, i));
			writeModel(testCase.getExpectedOutputModel(), String.format("target/%s.expected.%d", serviceFileName, i));
			Model outputModel;
			try {
				outputModel = ((ServiceImpl)service).invokeServiceUnparsed(testCase.getInputModel());
			} catch (IOException e) {
				throw new MojoFailureException(String.format("error contacting service %s: %s:", service, e.getMessage()));
			}
			writeModel(outputModel, String.format("target/%s.output.%d", serviceFileName, i));
			if (getLog().isDebugEnabled())
				getLog().debug(String.format("output from %s:\n%s", service, RdfUtils.logStatements("\t", outputModel)));
			compareOutput(outputModel, testCase.getExpectedOutputModel());
			try {
				sanityCheckOutputModel(service, outputModel);
			} catch (SADIException e) {
				getLog().warn(e.getMessage());
			}
		}
	}
	
	private void writeModel(Model model, String filename)
	{
		filename = filename.concat(".n3");
		try {
			model.write(new FileOutputStream(filename), "N3");
		} catch (FileNotFoundException e) {
			getLog().error(String.format("error writing to %s", filename), e);
		}
	}

//	private List<TestCase> getTestCases(Service service)
//	{
//		List<TestCase> tests = new ArrayList<TestCase>();
//		// TODO add to ServiceDescription interface and fix this...
//		Model serviceModel = ((ServiceImpl)service).getServiceModel();
//		MyGridServiceOntologyHelper helper = new MyGridServiceOntologyHelper();
//		for (RDFNode testCaseNode: helper.getTestCasePath().getValuesRootedAt(serviceModel.getResource(service.getURI()))) {
//			try {
//				if (!testCaseNode.isResource()) {
//					throw new Exception("test case node is literal");
//				}
//				Resource testCaseResource = testCaseNode.asResource();
//				Collection<RDFNode> inputs = helper.getTestInputPath().getValuesRootedAt(testCaseResource);
//				if (inputs.isEmpty()) {
//					throw new Exception("no input specified, but each test case needs one");
//				} else if (inputs.size() > 1) {
//					throw new Exception("multiple inputs specified, but each test case can only have one");
//				}
//				Collection<RDFNode> outputs = helper.getTestOutputPath().getValuesRootedAt(testCaseResource);
//				if (outputs.isEmpty()) {
//					throw new Exception("no output specified, but each test case needs one");
//				} else if (outputs.size() > 1) {
//					throw new Exception("multiple outputs specified, but each test case can only have one");
//				}
//				tests.add(new TestCase(inputs.iterator().next(), outputs.iterator().next()));
//			} catch (Exception e) {
//				getLog().warn(String.format("skipping test case %s: %s", testCaseNode, e.getMessage()));
//			}
//		}
//		return tests;
//	}

	private void sanityCheckOutputModel(Service service, Model outputModel) throws SADIException
	{
		ServiceTester.sanityCheckOutput(service, outputModel);
		getLog().info("actual output matches output class definition");
	}
	
	private boolean compareOutput(Model output, Model expected) throws MojoFailureException
	{
		if (output.isIsomorphicWith(expected)) {
			getLog().info("actual output matches expected output");
			return true;
		} else {
			ModelDiff diff = ModelDiff.diff(output, expected);
			if (!diff.inXnotY.isEmpty())
				getLog().error("service output had unexpected statements:\n" + RdfUtils.logStatements("\t", diff.inXnotY));
			if (!diff.inYnotX.isEmpty())
				getLog().error("service output had missing statements:\n" + RdfUtils.logStatements("\t", diff.inYnotX));
			throw new MojoFailureException("actual output did not match expected output; see above for details");
		}
	}
	
	private Service initService() throws MojoExecutionException
	{
		try {
			return new ServiceImpl(serviceURL);
		} catch (SADIException e) {
			throw new MojoExecutionException(String.format("error connecting to service %s: %s", serviceURL, e.toString()));
		}
	}
	
	public static void main(String args[]) throws MojoExecutionException, MojoFailureException
	{
		if (args.length > 0)
			System.setProperty(SERVICE_URL_KEY, args[0]);
		if (args.length > 1)
			System.setProperty(INPUT_KEY, args[1]);
		if (args.length > 2)
			System.setProperty(EXPECTED_OUTPUT_KEY, args[2]);
		new TestService().execute();
	}
}
