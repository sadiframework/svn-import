package ca.wilkinsonlab.sadi.service.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.rdf.RdfService;
import ca.wilkinsonlab.sadi.utils.ModelDiff;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * A Maven plugin to test a SADI service.
 * @author Luke McCarthy
 * @goal test-service
 */
public class ServiceTester extends AbstractMojo
{
	private static final String SERVICE_URL_KEY = "serviceURL";
	private static final String INPUT_KEY = "input";
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
		String serviceURL = System.getProperty(SERVICE_URL_KEY);
		Service service;
		try {
			service = new RdfService(serviceURL);
		} catch (SADIException e) {
			throw new MojoFailureException(String.format("error connecting to service %s: %s", serviceURL, e.getMessage()));
		}
		
		Model inputModel = null;
		String inputPath = System.getProperty(INPUT_KEY);
		if (inputPath != null)
			inputModel = createModel(inputPath);
		
		if (inputModel == null)
			throw new MojoFailureException("error reading input RDF; see above for details");

		Model outputModel;
		try {
			OntClass inputClass = service.getInputClass();
			Collection<Resource> inputNodes = inputModel.listResourcesWithProperty(RDF.type, inputClass).toList();
			Collection<Triple> triples = service.invokeService(inputNodes);
			outputModel = RdfUtils.triplesToModel(triples);
		} catch (SADIException e) {
			throw new MojoFailureException(String.format("error invoking service at %s: %s", serviceURL, e.toString()));
		}

		RDFWriter writer = outputModel.getWriter();
		writer.write(outputModel, System.out, "");
		
		Model expectedModel = null;
		String expectedPath = System.getProperty(EXPECTED_OUTPUT_KEY);
		if (expectedPath != null)
			expectedModel = createModel(expectedPath);
		if (expectedModel != null) {
			if (outputModel.isIsomorphicWith(expectedModel)) {
				getLog().info("actual output matched expected output");	
			} else {
				ModelDiff diff = ModelDiff.diff(outputModel, expectedModel);
				if (!diff.inXnotY.isEmpty())
					getLog().error("service output had unexpected statements:\n" + RdfUtils.logStatements("\t", diff.inXnotY));
				if (!diff.inYnotX.isEmpty())
					getLog().error("service output had unexpected statements:\n" + RdfUtils.logStatements("\t", diff.inYnotX));
			}
		}
	}
	
	private Model createModel(String pathOrURL)
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
			getLog().error(String.format("error reading RDF from %s: %s", pathOrURL, e.toString()));
		}
		return null;
	}
	
	public static void main(String args[]) throws MojoExecutionException, MojoFailureException
	{
		if (args.length > 0)
			System.setProperty(SERVICE_URL_KEY, args[0]);
		if (args.length > 1)
			System.setProperty(INPUT_KEY, args[1]);
		if (args.length > 2)
			System.setProperty(EXPECTED_OUTPUT_KEY, args[2]);
		new ServiceTester().execute();
	}
}
