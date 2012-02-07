package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.SADIException;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

public class MinimalModelDecomposerTest
{
	private static final Logger log = Logger.getLogger(MinimalModelDecomposerTest.class);
	
	private Model model;
	private OntModel ontModel;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
	}

	@Before
	public void setUp() throws Exception
	{
		model = ModelFactory.createDefaultModel();
		ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
	}

	@After
	public void tearDown() throws Exception
	{
		model.close();
		model = null;
		ontModel.close();
		ontModel = null;
	}

	@Test
	public void testDecompose1() throws SADIException
	{
		String namespace = "UniProt";
		String id = "Q9NZD0";
		String uri = LSRNUtils.getURI(namespace, id);
		OntClass uniprotRecord = OwlUtils.getOntClassWithLoad(ontModel, LSRNUtils.getClassURI(namespace));
		OntClass uniprotID = OwlUtils.getOntClassWithLoad(ontModel, LSRNUtils.getIdentifierClassURI(namespace));
		Model expectedModel = ModelFactory.createDefaultModel();
		SIOUtils.createAttribute(expectedModel.createResource(uri), uniprotID, ResourceFactory.createPlainLiteral(id));
		
		ontModel.read(uri);
		MinimalModelDecomposer mmd = new MinimalModelDecomposer(ontModel.getResource(uri), uniprotRecord);
		if (log.isTraceEnabled()) {
			mmd.getModel().register(new ModelChangedAdapter() {
				@Override
				public void addedStatement(Statement s) {
					log.trace(String.format("added statement %s", s));
				}
				@Override
				public void removedStatement(Statement s) {
					log.trace(String.format("removed statement %s", s));
				}
			});
		}
		mmd.decompose();
		
		if (log.isTraceEnabled()) {
			log.trace(String.format("minimal model:\n%s", RdfUtils.logModel(mmd.getModel())));
		}
		Resource Q9NZD0 = mmd.getModel().getResource(uri);
		assertEquals(id, LSRNUtils.getID(Q9NZD0));
	}
}
