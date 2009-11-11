package ca.wilkinsonlab.sadi.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class ExceptionUtils
{
	private static final Log log = LogFactory.getLog(ExceptionUtils.class);
	
	public static final String NS = "http://sadiframework.org/ontologies/sadi.owl#";
	
	public static Model createExceptionModel(Throwable t)
	{
		Model model = ModelFactory.createDefaultModel();
		exceptionToRdf(model, t);
		return model;
	}
	
	public static Resource exceptionToRdf(Model model, Throwable t)
	{
		Resource exception = model.createResource();
		exception.addProperty(RDF.type, SADI.Exception);
		exception.addProperty(RDFS.label, StringUtils.defaultString(t.getMessage(), t.getClass().toString()));
		exception.addProperty(RDFS.comment, t.toString());
		RDFList stackTrace = model.createList();
		for (StackTraceElement e: t.getStackTrace()) {
			Resource element = model.createResource();
			element.addProperty(RDF.type, SADI.StackTraceElement);
			element.addProperty(RDFS.label, e.toString());
			stackTrace = stackTrace.with(element);
		}
		exception.addProperty(SADI.hasStackTrace, stackTrace);
		return exception;
	}
	
	public static String exceptionModelToString(Model model)
	{
		StringBuilder buf = new StringBuilder();
		for (ResIterator i = model.listResourcesWithProperty(RDF.type, SADI.Exception); i.hasNext(); ) {
			Resource exception = i.next();
			try {
				String next = exception.getProperty(RDFS.label).getLiteral().getLexicalForm();
				if (buf.length() > 0)
					buf.append(", ");
				buf.append(next);
			} catch (Exception e) {
				log.warn("error converting RDF exception to string", e);
			}
		}
		return buf.toString();
	}
}
