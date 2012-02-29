package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class ExceptionUtilsTest
{
	private static final Logger log = Logger.getLogger(ExceptionUtils.class);
	
	@Test
	public void testCreateExceptionModel() throws Exception
	{
		String message = "this is the exception message";
		Exception exception = new Exception(message);
		Model model = ExceptionUtils.createExceptionModel(exception);
		log.debug(String.format("exception model:\n%s", RdfUtils.logModel(model)));
	}

	@Test
	public void testExceptionToRdf() throws Exception
	{
		String message = "this is the exception message";
		Exception exception = new Exception(message);
		List<String> stackTraceFromJava = new ArrayList<String>();
		for (StackTraceElement element: exception.getStackTrace()) {
			stackTraceFromJava.add(element.toString());
		}
		
		Model model = ModelFactory.createDefaultModel();
		Resource exceptionNode = ExceptionUtils.exceptionToRdf(model, exception);
		List<String> stackTraceFromRDF = new ArrayList<String>();
		for (RDFNode element: exceptionNode.getProperty(SADI.hasStackTrace).getObject().as(RDFList.class).asJavaList()) {
			stackTraceFromRDF.add(element.as(Resource.class).getProperty(RDFS.label).getLiteral().getLexicalForm());
		}
		
		for (String s: stackTraceFromJava)
			assertTrue(String.format("missing stack trace element %s", s), stackTraceFromRDF.contains(s));
		for (String s: stackTraceFromRDF)
			assertTrue(String.format("superfluous stack trace element %s", s), stackTraceFromJava.contains(s));
	}

	@Test
	public void testExceptionModelToString()
	{
	}
}
