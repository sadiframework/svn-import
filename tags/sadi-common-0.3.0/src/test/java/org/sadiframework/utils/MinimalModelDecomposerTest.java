package org.sadiframework.utils;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sadiframework.SADIException;


import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class MinimalModelDecomposerTest
{
	@SuppressWarnings("unused")
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
	}
}
