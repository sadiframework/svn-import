package ca.wilkinsonlab.sadi.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.Config;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.MapFilter;
import com.hp.hpl.jena.util.iterator.MapFilterIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class RdfUtils
{
	private static final Logger log = Logger.getLogger(RdfUtils.class);
	
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
	 * Convert a Jena Model to a collection of triples.
	 * @param model
	 * @return a collection of triples representing the model
	 */
	public static Collection<Triple> modelToTriples(Model model) {
		
		Collection<Triple> triples = new ArrayList<Triple>();
		StmtIterator i = model.listStatements();
		while(i.hasNext()) {
			Statement stmt = i.next();
			Triple triple = new Triple(
					stmt.getSubject().asNode(),
					stmt.getPredicate().asNode(),
					stmt.getObject().asNode());
			triples.add(triple);
		}
		return triples;
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
	
	public static RDFNode getRDFNode(Model model, Node node)
	{
		if (node.isLiteral())
			return getLiteral(model, node);
		else
			return getResource(model, node);
	}
	
	public static Resource getResource(Model model, Node node)
	{
		if (node.isBlank())
			return model.createResource(node.getBlankNodeId());
		else if (node.isURI())
			return model.createResource(node.getURI());
		else
			throw new IllegalArgumentException(String.format("node %s is not a resource", node));
	}
	
	public static Literal getLiteral(Model model, Node node)
	{
		if (!node.isLiteral())
			throw new IllegalArgumentException(String.format("node %s is not a literal", node));
			
		return ResourceFactory.createTypedLiteral(node.getLiteralLexicalForm(), node.getLiteralDatatype());
	}
	
	public static Property getProperty(Model model, Node node)
	{
		if (node.isURI())
			return model.createProperty(node.getURI());
		else
			throw new IllegalArgumentException(String.format("node %s is not a resource", node));
	}
	
	public static String logStatements(Model model)
	{
		return logStatements("", model);
	}
	
	public static String logStatements(String prefix, Model model)
	{
		StringBuilder buf = new StringBuilder();
		
		StmtIterator iter = model.listStatements();
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();
		    Resource  subject   = stmt.getSubject();
		    Property  predicate = stmt.getPredicate();
		    RDFNode   object    = stmt.getObject();
		    
		    buf.append(prefix);
		    buf.append(subject.toString());
		    buf.append(" ");
		    buf.append(predicate.toString());
		    buf.append(" ");
		    if (object.isLiteral())
		    	buf.append("\"");
		    buf.append(object.toString());
		    if (object.isLiteral())
		    	buf.append("\"");
		    if (iter.hasNext())
		    	buf.append(" .\n");
		}
		
	    return buf.toString();
	}
	
	public static String logModel(Model model)
	{
		StringWriter writer = new StringWriter();
		model.write(writer, "N3");
		return writer.toString();
	}

	/**
	 * @author Ben Vandervalk
	 * @param uriString the URI to test, as a string
	 * @return true if the given string is an absolute URI
	 */
	public static boolean isURI(String uriString)
	{
		if (StringUtils.isEmpty(uriString))
			return false;
	
		try {
			URI uri = new URI(uriString);
			if (uri.isAbsolute())
				return true;
			else
				return false;
		} catch (URISyntaxException e) {
			return false;
		}
	}
	
	public static boolean isURL(String url)
	{
		try {
			new URL(url);
			return true;
		} catch (MalformedURLException e) {
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
		return resource.hasProperty(RDF.type);
	}
	
	/**
	 * Returns an iterator over the rdf:types of the specified resource.
	 * @param resource the resource
	 * @return an iterator over the rdf:types of the specified resource
	 */
	public static ExtendedIterator<Resource> getTypes(Resource resource)
	{
		return getPropertyValues(resource, RDF.type, null);
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
	 * (including numbers) and appends the associated xsd:datatype.  However,
	 * Jena does not include angle brackets around the absolute datatype URI,
	 * so the default string form will not parse within a standard SPARQL query.  
	 * Also, the Jena string representation of a variable doesn't include the 
	 * preceding "?".
	 * 
	 * @param node
	 * @return
	 */
	public static String getPlainString(Node node)
	{
		String str;
		if(node.isURI()) 
			str = node.toString();
		else if(node.isVariable()) 
			str = "?" + node.getName();
		else if(node.isBlank()) 
			str = node.getBlankNodeLabel().toString(); 
		else 
//			str = node.getLiteral().getLexicalForm().toString();
			str = node.getLiteralLexicalForm();
		return str;
	}
	
	public static String getPlainString(RDFNode node)
	{
		return getPlainString(node.asNode());
	}
	
	/**
	 * Attempt to parse the specified literal as a boolean.
	 * In addition to the actual typed literal true, the strings
	 * "true", "t", "on", "1", etc., will be parsed as true.
	 * @param literal
	 * @return
	 */
	public static Boolean getBoolean(Literal literal)
	{
		try {
			return literal.getBoolean();
		} catch (Exception e) { } 
		try {
			int i = literal.getInt();
			if (i == 1)
				return true;
			else if (i == 0)
				return false;
		} catch (Exception e) { }
		String s = literal.getLexicalForm();
		if (s.equalsIgnoreCase("true") ||
				s.equalsIgnoreCase("t") ||
				s.equalsIgnoreCase("yes") ||
				s.equalsIgnoreCase("y") ||
				s.equalsIgnoreCase("1")) {
			return true;
		} else if (s.equalsIgnoreCase("false") ||
				s.equalsIgnoreCase("f") ||
				s.equalsIgnoreCase("no") ||
				s.equalsIgnoreCase("n") ||
				s.equals("0")) {
			return false;
		}
		return null;
	}
	
	public static Literal createTypedLiteral(String jenaToString)
	{
		int splitPoint = jenaToString.lastIndexOf("^^");
		if (splitPoint < 0)
			return ResourceFactory.createPlainLiteral(jenaToString);
		
		String value = jenaToString.substring(0, splitPoint);
		String datatypeURI = jenaToString.substring(splitPoint+2);
		RDFDatatype datatype = TypeMapper.getInstance().getTypeByName(datatypeURI);
		return ResourceFactory.createTypedLiteral(value, datatype);
	}
	
	public static Collection<Resource> extractResources(Collection<RDFNode> nodes) 
	{
		Collection<Resource> resources = new ArrayList<Resource>(nodes.size());
		for (RDFNode node :nodes) {
			if (node.isResource()) {
				resources.add(node.as(Resource.class));
			}
		}
		return resources;
	}
	
	public static Collection<Literal> extractLiterals(Collection<RDFNode> nodes) 
	{	
		Collection<Literal> literals = new ArrayList<Literal>(nodes.size());
		for (RDFNode node :nodes) {
			if (node.isLiteral()) {
				literals.add(node.as(Literal.class));
			}
		}
		return literals;
	}
	
	/**
	 * Create a new memory model and read the contents of the argument,
	 * which can be either a local path or a remote URL.
	 * @param pathOrURL a local path or a remote URL
	 * @return the new model
	 * @throws IOException if the argument is an invalid URL and can't be read locally
	 * @deprecated use loadModelFromString (and create/destroy your own model)
	 */
	public static Model createModelFromPathOrURL(String pathOrURL) throws IOException
	{
		Model model = ModelFactory.createDefaultModel();
		return loadModelFromPathOrURL(model, pathOrURL);
	}
	
	/**
	 * Read the contents of a local path or a remote URL into the specified model.
	 * @param model the model 
	 * @param pathOrURL a local path or a remote URL
	 * @return the model
	 * @throws IOException if the argument is an invalid URL and can't be read locally
	 * @deprecated use loadModelFromString
	 */
	public static Model loadModelFromPathOrURL(Model model, String pathOrURL) throws IOException
	{
		try {
			URL url = new URL(pathOrURL);
			log.debug(String.format("identified %s as a URL", pathOrURL));
			model.read(url.toString());
			return model;
		} catch (MalformedURLException e) {
			log.debug(String.format("%s is not a URL: %s", pathOrURL, e.getMessage()));
		}
		log.debug(String.format("identified %s as a path", pathOrURL));
		try {
			File f = new File(pathOrURL);
			model.read(new FileInputStream(f), "");
			return model;
		} catch (FileNotFoundException e) {
			log.error(String.format("error reading RDF from %s: %s", pathOrURL, e.toString()));
			throw new IOException(String.format("%s did not parse as a URL and could not be read as a file: %s", pathOrURL, e.getMessage()));
		}
	}
	
	/**
	 * Load the specified model according to the argument,
	 * which can be a remote URL, a local path, or inline RDF.
	 * @param s a remote URL, a local path, or inline RDF
	 * @return the new model
	 * @throws IOException if the argument is invalid
	 */
	public static Model loadModelFromString(Model model, String s) throws IOException
	{
		return loadModelFromString(model, s, null);
	}
	
	/**
	 * Load the specified model according to the argument,
	 * which can be a remote URL, a local path, or inline RDF.
	 * @param s a remote URL, a local path, or inline RDF
	 * @param c a class relative to which to search for a resource, or null
	 * @return the new model
	 * @throws IOException if the argument is invalid
	 */
	public static Model loadModelFromString(Model model, String s, Class<?> c) throws IOException
	{
		// try as URL first...
		try {
			URL url = new URL(s);
			if (log.isDebugEnabled())
				log.debug(String.format("identified %s as a URL", s));
			model.read(url.toString());
			return model;
		} catch (MalformedURLException e) {
			if (log.isDebugEnabled())
				log.debug(String.format("'%s' is not a URL: %s", s, e.getMessage()));
		}
		
		// try as classpath resource next...
		if (c != null) {
			InputStream stream = c.getResourceAsStream(s);
			if (stream != null) {
				if (log.isDebugEnabled())
					log.debug(String.format("identified %s as a classpath resource", s));
				model.read(stream, "");
				return model;
			} else {
				if (log.isDebugEnabled())
					log.debug(String.format("'%s' is not a classpath resource", s));
			}
		}
		
		// try as local path next...
		try {
			File f = new File(s);
			if (log.isDebugEnabled())
				log.debug(String.format("identified %s as a path", s));
			model.read(new FileInputStream(f), "");
			return model;
		} catch (FileNotFoundException e) {
			if (log.isDebugEnabled())
				log.debug(String.format("error reading RDF from '%s': %s", s, e.toString()));
		}
		
		// try as inline RDF last...
		try {
			return loadModelFromInlineRDF(model, s);
		} catch (IOException e) {
		}
		
		// not a remote URL, local path or inline RDF...
		throw new IOException(String.format("could not identify '%s' as a remote URL, local path or inline RDF", s));
	}
	
	/**
	 * Load the specified model with the RDF serialized in the specified string.
	 * @param model the model
	 * @param s the inline RDF
	 * @return the model
	 * @throws IOException if the inline RDF is serialized in an unrecognized format
	 */
	public static Model loadModelFromInlineRDF(Model model, String s) throws IOException
	{
		ByteArrayInputStream stream = new ByteArrayInputStream(s.getBytes());
		for (ContentType type: ContentType.getUniqueContentTypes()) {
			stream.reset();
			try {
				type.readModel(model, stream, "");
				if (log.isDebugEnabled())
					log.debug(String.format("identified '%s' as %s", s, type));
				return model;
			} catch (Exception e) {
				if (log.isDebugEnabled())
					log.debug(String.format("error parsing '%s' as %s: %s", s, type, e.toString()));
			}
		}
		throw new IOException(String.format("could not identify '%s' as inline RDF", s));
	}
	
	/**
	 * Load the specified model with RDF serialized in the specified format
	 * from the specified string.
	 * @param model the model
	 * @param s the inline RDF
	 * @param type the ContentType
	 * @return the model
	 * @throws IOException if the inline RDF isn't valid for the specified format
	 */
	public static Model loadModelFromInlineRDF(Model model, String s, ContentType type) throws IOException
	{
		ByteArrayInputStream stream = new ByteArrayInputStream(s.getBytes());
		type.readModel(model, stream, "");
		return model;
	}
	
	/**
	 * Adds common namespace prefixes to the specified model.
	 * @param model the model
	 */
	public static void addNamespacePrefixes(Model model)
	{
		Configuration nsConfig = Config.getConfiguration().subset("sadi.ns");
		for (Iterator<?> keys = nsConfig.getKeys(); keys.hasNext();) {
			String key = (String)keys.next();
			model.setNsPrefix(key, nsConfig.getString(key));
		}
	}
	
	/**
	 * Create a Resource with the specified URI that has all of the
	 * properties of the specified BNode.
	 * This effectively gives the BNode a URI.
	 * @param bnode the source BNode
	 * @param uri the URI of the new Resource
	 * @return
	 * @deprecated use com.hp.hpl.jena.util.ResourceUtils.renameResource instead...
	 */
	public static Resource createNamedClone(Resource bnode, String uri)
	{
		Resource target = bnode.getModel().createResource(uri);
		StmtIterator statements = bnode.listProperties();
		while (statements.hasNext()) {
			Statement statement = statements.next();
			target.addProperty(statement.getPredicate(), statement.getObject());
		}
		return target;
	}
	
	/**
	 * Returns a UUID-based URI (as per RFC 4122).
	 * @return a UUID-based URI
	 */
	public static String createUniqueURI()
	{
		return String.format("urn:uuid:%s", UUID.randomUUID());
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
	
	/**
	 * Returns the first value of a property that is of a specified type.
	 * @param s the subject
	 * @param p the property
	 * @param type the type
	 * @return the first value of a property that is of a specified type
	 */
	public static final Resource getPropertyValue(Resource s, Property p, Resource type)
	{
		ExtendedIterator<Resource> i = getPropertyValues(s, p, type);
		try {
			return i.hasNext() ? i.next() : null;
		} finally {
			i.close();
		}
	}
	
	/**
	 * Returns values of a property that are of a specified type.
	 * @param s the subject
	 * @param p the property
	 * @param type the type
	 * @return values of a property that are of a specified type
	 */
	public static final ExtendedIterator<Resource> getPropertyValues(Resource s, Property p, Resource type)
	{
		return new MapFilterIterator<Statement, Resource>(new StatementToResourceFilter(type), s.listProperties(p));
	}
	
	/**
	 * 
	 * @author Luke McCarthy
	 */
	public static final class StatementToResourceFilter implements MapFilter<Statement, Resource>
	{
		private Resource type;
		
		public StatementToResourceFilter()
		{
			this(null);
		}
		
		public StatementToResourceFilter(Resource type)
		{
			this.type = type;
		}
		
		@Override
		public Resource accept(Statement s)
		{
			RDFNode o = s.getObject();
			if (o.isResource()) {
				Resource object = o.asResource();
				if (type == null || object.hasProperty(RDF.type, type))
					return object;
			}
			return null;
		}
	}

	/**
	 * Remove anonymous nodes from a collection of RDF nodes.
	 * @param subProperties
	 * @return true if any nodes were removed from the collection; false otherwise
	 */
	public static boolean removeAnonymousNodes(Collection<? extends RDFNode> nodes)
	{
		boolean removedAny = false;
		for (Iterator<? extends RDFNode> i = nodes.iterator(); i.hasNext(); ) {
			if (i.next().isAnon()) {
				i.remove();
				removedAny = true;
			}
		}
		return removedAny;
	}
}
