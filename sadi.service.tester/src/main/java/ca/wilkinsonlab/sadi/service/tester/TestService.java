package ca.wilkinsonlab.sadi.service.tester;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceImpl;
import ca.wilkinsonlab.sadi.utils.ModelDiff;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

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
	 * This parameter is required.
	 * @parameter expression="${input}"
	 */
	private String inputPath;
	private static final String INPUT_KEY = "input";
	
	/**
	 * The URL or local path of the expected output RDF document corresponding
	 * to the input document specified above.
	 * This parameter is required.
	 * @parameter expression="${expected}"
	 */
	private String expectedPath;
	private static final String EXPECTED_OUTPUT_KEY = "expected";
	
	/**
	 * Expected properties:
	 *  serviceURL (required)
	 *  input (required) input RDF URL or path to file
	 *  expected (optional) expected output RDF URL or path to file
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		Service service = initService();
		Model inputModel = createInputModel();
		Model expectedModel = createExpectedOutputModel();
		Model outputModel = createOutputModel(service, inputModel);
		sanityCheckOutputModel(service, outputModel);
		if (getLog().isDebugEnabled())
			getLog().debug(String.format("output from %s:\n%s", service, RdfUtils.logStatements("\t", outputModel)));
		if (expectedModel != null)
			if (!compareOutput(outputModel, expectedModel))
				throw new MojoFailureException("actual output did not match expected output; see above for details");
	}
	
	private void sanityCheckOutputModel(Service service, Model outputModel) throws MojoFailureException
	{
		/* TODO put this in the Service interface...
		 */
		try {
			OntModel ontModel = ((ServiceImpl)service).getOutputClass().getOntModel();
			ontModel.addSubModel(outputModel);
			ontModel.rebind();
			Collection<Individual> outputs = ontModel.listIndividuals(service.getOutputClass()).toList();
			if (outputs.isEmpty())
				throw new SADIException(String.format("output model doesn't contain any instances of output class %s", service.getOutputClassURI()));
			StringBuffer buf = new StringBuffer();
//			for (Restriction restriction: ((ServiceImpl)service).getRestrictions()) {
//				/* confirm that the expected model attaches the predicates the
//				 * registry thinks that it does...
//				 */
//				for (Individual output: outputs) {
//					if (!output.hasOntClass(restriction))
//						buf.append(String.format("\noutput node %s doesn't match restriction %s", output, OwlUtils.getRestrictionString(restriction)));
//				}
//			}
			if (buf.length() > 0) {
				buf.insert(0, "output doesn't appear to match the restrictions specified on your output class:");
				throw new SADIException(buf.toString());
			}
		} catch (SADIException e) {
			throw new MojoFailureException(e.getMessage());
		}
	}
	
	private boolean compareOutput(Model output, Model expected)
	{
		if (output.isIsomorphicWith(expected)) {
			getLog().info("actual output matched expected output");
			return true;
		} else {
			ModelDiff diff = ModelDiff.diff(output, expected);
			if (!diff.inXnotY.isEmpty())
				getLog().error("service output had unexpected statements:\n" + RdfUtils.logStatements("\t", diff.inXnotY));
			if (!diff.inYnotX.isEmpty())
				getLog().error("service output had missing statements:\n" + RdfUtils.logStatements("\t", diff.inYnotX));
			return false;
		}
	}
	
	private Service initService() throws MojoExecutionException
	{
//		serviceURL = getRequiredProperty(SERVICE_URL_KEY);
		try {
			return new ServiceImpl(serviceURL);
		} catch (SADIException e) {
			throw new MojoExecutionException(String.format("error connecting to service %s: %s", serviceURL, e.toString()));
		}
	}
	
	private Model createInputModel() throws MojoFailureException, MojoExecutionException
	{
//		inputPath = getRequiredProperty(INPUT_KEY);
		try {
			return createModel(inputPath);
		} catch (MojoFailureException e) {
			throw new MojoFailureException(String.format("error reading input RDF: %s", e.getMessage()));
		}
	}
	
	private Model createExpectedOutputModel() throws MojoFailureException
	{
//		expectedPath = System.getProperty(EXPECTED_OUTPUT_KEY);
		if (expectedPath != null)
			return createModel(expectedPath);
		else
			return null;
	}
	
	private static Model createOutputModel(Service service, Model input) throws MojoFailureException, MojoExecutionException
	{
		try {
			OntClass inputClass = service.getInputClass();
			Collection<Resource> inputNodes = input.listResourcesWithProperty(RDF.type, inputClass).toList();
			Collection<Triple> triples = service.invokeService(inputNodes);
			return RdfUtils.triplesToModel(triples);
		} catch (SADIException e) {
			throw new MojoExecutionException(String.format("error invoking service %s: %s", service, e.toString()));
		}
	}
	
	private static Model createModel(String pathOrURL) throws MojoFailureException
	{
		Model model = ModelFactory.createDefaultModel();
		try {
			URL url = new URL(pathOrURL);
			model.read(url.toString());
			return model;
		} catch (MalformedURLException e) {
		}
		try {
			File f = new File(pathOrURL);
			model.read(new FileInputStream(f), "");
			return model;
		} catch (FileNotFoundException e) {
			throw new MojoFailureException(String.format("can't read RDF from %s: %s", pathOrURL, e.getMessage()));
		}
	}
	
//	private static String getRequiredProperty(String key) throws MojoExecutionException
//	{
//		String value = System.getProperty(key);
//		if (value == null)
//			throw new MojoExecutionException( String.format("required property %s is undefined", key) );
//		else
//			return value;
//	}
	
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
