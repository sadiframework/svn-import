package ca.wilkinsonlab.sadi.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class RdfUtils
{
	/**
	 * Convert a collection of Jena Triples to a Jena Model.
	 * @param triples the collection of triples
	 * @return a new model containing the triples
	 */
	public static Model triplesToModel(Collection<Triple> triples)
	{
		Model model = ModelFactory.createMemModelMaker().createFreshModel();
		for (Triple triple: triples)
			addTripleToModel(model, triple);
		return model;
	}
	
	/**
	 * Add the specified Triple to the specified Model.
	 * @param model the model
	 * @param triple the triple
	 */
	public static void addTripleToModel(Model model, Triple triple)
	{
		Resource s = getResource(model, triple.getSubject());
		Property p = getProperty(model, triple.getPredicate());
		RDFNode o = getRDFNode(model, triple.getObject());
		model.add(s, p, o);
	}
	
	private static RDFNode getRDFNode(Model model, Node node)
	{
		if (node.isLiteral())
			return getLiteral(model, node);
		else
			return getResource(model, node);
	}
	
	private static Resource getResource(Model model, Node node)
	{
		if (node.isBlank())
			return model.createResource(node.getBlankNodeId());
		else if (node.isURI())
			return model.createResource(node.getURI());
		else
			throw new IllegalArgumentException(String.format("node %s is not a resource", node));
	}
	
	private static Literal getLiteral(Model model, Node node)
	{
		if (node.isLiteral())
			return model.createLiteral(node.getLiteralLexicalForm(), node.getLiteralLanguage());
		else
			throw new IllegalArgumentException(String.format("node %s is not a literal", node));
	}
	
	private static Property getProperty(Model model, Node node)
	{
		if (node.isURI())
			return model.createProperty(node.getURI());
		else
			throw new IllegalArgumentException(String.format("node %s is not a resource", node));
	}
	
	public static String logStatements(Model model)
	{
		StmtIterator iter = model.listStatements();
		StringBuilder buf = new StringBuilder();
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();
		    Resource  subject   = stmt.getSubject();
		    Property  predicate = stmt.getPredicate();
		    RDFNode   object    = stmt.getObject();

		    if (buf.length() > 0)
		    	buf.append(" .\n");
		    buf.append(subject.toString());
		    buf.append(" ");
		    buf.append(predicate.toString());
		    buf.append(" ");
		    if (object.isLiteral())
		    	buf.append("\"");
		    buf.append(object.toString());
		    if (object.isLiteral())
		    	buf.append("\"");
		} 
	    return buf.toString();
	}

	public static boolean isURI(String objectValue)
	{
		if (StringUtils.isEmpty(objectValue))
			return false;
	
		try {
			// TODO is "UTF-8" really required here?
			URI uri = new URI(objectValue, false, "UTF-8");
			if (uri.isAbsoluteURI())
				return true;
			else
				return false;
		} catch (URIException e) {
			return false;
		}
	}
	
	/**
	 * Returns true if the specified resource has at least one rdf:type,
	 * false otherwise.
	 * @param resource the resource
	 * @return true if the specified resource has at least one rdf:type,
	 *         false otherwise.
	 */
	public static boolean isTyped(Resource resource)
	{
		return resource.getProperty(RDF.type) != null;
	}

	/** 
	 * Extract a collection of triples from an RDF input stream.  The encoding
	 * of the incoming RDF is specified by 'lang' (e.g. "RDF/XML").  The possible
	 * values for 'lang' are the same as for Jena's 'Model.read' method:
	 * "RDF/XML", "N-TRIPLES", and "N3".
	 * 
	 * The main caveat of this method is that it must load the entire set
	 * of triples into memory. 
	 * 
	 * @param input The RDF to be converted to triples.  
	 * @param lang The encoding of the RDF. 
	 * @return a collection of triples.
	 */
	public static Collection<Triple> getTriples(InputStream input, String lang)
	{
		// Use a Jena model to convert RDF/XML => triples.
		Model model = ModelFactory.createMemModelMaker().createFreshModel();
		model.read(input, "", lang);
		
		Collection<Triple> triples = new ArrayList<Triple>();
		for (StmtIterator i = model.listStatements(); i.hasNext(); )
			triples.add(i.nextStatement().asTriple());
		return triples;
	}

	/**
	 * Return the string representation of a Jena Node, without surrounding
	 * brackets or quotes, and without any xsd:datatype suffix.
	 * 
	 * The default string representation of a node in Jena quotes literals 
	 * (including numbers) and appends the associated xsd:datatype.  Also, the
	 * Jena string representation of a variable doesn't include the preceding
	 * "?".
	 * 
	 * @param node
	 * @return
	 */
	public static String getPlainString(Node node)
	{
		String str;
		if(node.isURI()) {
			str = node.toString();
		}
		else if(node.isVariable()) {
			str = "?" + node.getName();
		}
		else {
			str = node.getLiteralValue().toString();
		}
		return str;
	}
	
//	/**
//	 * Write a collection of triples to a file, as RDF.  
//	 * @param filename The name of the file to write to
//	 * @param triples The triples to write
//	 * @param rdfFormat This value is passed onto Jena's model.write() method.  Possible
//	 * values include "RDF/XML" and "N3".
//	 * @throws IOException if there is a problem opening or writing to the file
//	 */
//	public static void writeTriplesAsRDF(String filename, Collection<Triple> triples, String rdfFormat) throws IOException 
//	{
//		FileOutputStream fos = new FileOutputStream(filename);
//		writeTriplesAsRDF(fos, triples, rdfFormat);
//		fos.close();
//	}
//	
//	/**
//	 * Write a collection of triples to an output stream, as RDF.  
//	 * @param os The output stream
//	 * @param triples The triples to write
//	 * @param rdfFormat This value is passed onto Jena's model.write() method.  Possible
//	 * values include "RDF/XML" and "N3".
//	 * @throws IOException if there is a problem writing to the stream
//	 */
//	public static void writeTriplesAsRDF(OutputStream os, Collection<Triple> triples, String rdfFormat) throws IOException
//	{
//		Model model = modelMaker.createFreshModel();
//		for (Triple triple : triples) {
//			Resource s = model.createResource(triple.getSubject().toString());
//			Property p = model.createProperty(triple.getPredicate().toString());
//			String obj = triple.getObject().toString();
//			RDFNode o;
//			if (TriplesHelper.isURI(obj))
//				o = new ResourceImpl(StringUtil.escapeURI(obj));
//			else
//				o = model.createLiteral(obj);
//			model.add(s,p,o);
//		}
//		try {
//			model.write(os, rdfFormat);
//		}
//		finally {
//			model.close();
//		}
//	}
}
