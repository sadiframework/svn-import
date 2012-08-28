package org.sadiframework.service.blast;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sadiframework.service.blast.NCBIBLASTServiceServlet;
import org.sadiframework.service.blast.MasterServlet.Taxon;
import org.sadiframework.utils.RdfUtils;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.BadURIException;

public class NCBIBLASTServiceServletTest
{
	private static Logger log = Logger.getLogger(NCBIBLASTServiceServletTest.class);
	
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
	}

	@After
	public void tearDown() throws Exception
	{
	}
	
	@Test
	public void testParseBLAST() throws Exception
	{
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("ncbi-blast", "http://sadiframework.org/services/blast/ncbi-blast.owl#");
		model.setNsPrefix("blast", "http://sadiframework.org/ontologies/blast.owl#");
		model.setNsPrefix("sio", "http://semanticscience.org/resource/");
		Taxon human = new Taxon();
		human.name = "human";
		human.id = "9606";
		new NCBIBLASTServiceServlet.BLASTParser(human).parseBLAST(model, NCBIBLASTServiceServletTest.class.getResourceAsStream("/blast-report.xml"));
		assertFalse("empty result model", model.isEmpty());
		if (log.isDebugEnabled())
			log.debug(String.format("BLAST RDF:\n%s", RdfUtils.logModel(model)));
		try {
			model.write(new FileOutputStream("/tmp/blast-report.n3"), "N3");
		} catch (IOException e) {
			log.error(String.format("error writing to /tmp/blast-report.n3: %s", e));
		}
		try {
			model.write(new NullOutputStream(), "RDF/XML");
		} catch (BadURIException e) {
			fail("result model contains invalid URIs");
		}
	}
}
