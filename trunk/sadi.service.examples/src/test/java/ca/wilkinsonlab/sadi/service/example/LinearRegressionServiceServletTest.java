package ca.wilkinsonlab.sadi.service.example;

import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.utils.OwlUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;

public class LinearRegressionServiceServletTest
{
	private static final Logger log = Logger.getLogger(LinearRegressionServiceServletTest.class);
	
	private static final String NS = "http://sadiframework.org/examples/regression.owl#";
	
	private OntModel model;
	
	@Before
	public void before() throws SADIException
	{
		model = OwlUtils.createDefaultReasoningModel();
		log.debug("reading patients.rdf...");
		model.read(LinearRegressionServiceServletTest.class.getResourceAsStream("/patients.rdf"), "");
		log.debug("reading patients.owl...");
		model.read("file:../sadiframework.org/ontologies/patients.owl");
		log.debug("reading regression.owl...");
		model.read("file:src/main/webapp/regression.owl");
		log.debug("preparing...");
		model.prepare();
		log.debug("done");
	}
	
	@After
	public void after()
	{
		model.close();
		model = null;
	}
	
	@Test
	public void testOWL() throws Exception
	{
		findInstances(model.getOntClass(NS + "X"));
		findInstances(model.getOntClass(NS + "Y"));
		findInstances(model.getOntClass(NS + "PairedValue"));
		findInstances(model.getOntClass(NS + "InputClass"));
	}
	private void findInstances(OntClass c)
	{
		assertTrue(String.format("failed to identify dynamic instances of %s", c), c.listInstances().hasNext());
	}
}
