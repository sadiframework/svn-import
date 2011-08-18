package ca.wilkinsonlab.sadi.service;

import static org.junit.Assert.assertTrue;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.annotations.ParameterClass;
import ca.wilkinsonlab.sadi.service.annotations.ParameterDefaults;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.LocationMapper;

public class ServiceServletTest
{
	private static final Logger log = Logger.getLogger(ServiceServletTest.class);
	private static final String URI_PREFIX = "http://sadiframework.org/examples/";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		LocationMapper.get().addAltPrefix(URI_PREFIX, "file:src/test/resources/ca/wilkinsonlab/sadi/service/");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		LocationMapper.get().removeAltPrefix(URI_PREFIX);
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
	public void testParameterInstanceURI() throws ServletException
	{
		ParameterizedServiceServlet servlet = new ParameterizedServiceServlet();
		servlet.init();
		if (log.isDebugEnabled()) {
			StringBuffer buf = new StringBuffer("default parameters:\n");
			StmtIterator statements = servlet.getDefaultParameters().listProperties();
			while (statements.hasNext()) {
				buf.append("\t");
				buf.append(statements.next());
			}
			statements.close();
			log.debug(buf.toString());
		}
		assertTrue("default parameter instance does not have lang=\"en\"",
				   servlet.getDefaultParameters().hasLiteral(ResourceFactory.createProperty(URI_PREFIX + "hello.owl#lang"), "en"));
	}

	@InputClass(URI_PREFIX + "hello.owl#NamedIndividual")
	@OutputClass(URI_PREFIX + "hello.owl#GreetedIndividual")
	@ParameterClass(URI_PREFIX + "hello.owl#SecondaryParameters")
//	@ParameterDefaults({URI_PREFIX + "hello.owl#defaultParameters"})
	@ParameterDefaults({"http://sadiframework.org/examples/hello.owl#lang, http://www.w3.org/2001/XMLSchema#string", "en"})
	private static class ParameterizedServiceServlet extends ServiceServlet
	{
		private static final long serialVersionUID = 1L;
		
	}
}
