package ca.wilkinsonlab.sadi.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Utility class encapsulating methods useful for converting simple LSRN
 * database records to and from RDF/OWL.
 * @author Luke McCarthy
 */
public class LSRNUtils
{
	public static final Pattern NS_PATTERN = Pattern.compile("http://purl.oclc.org/SADI/LSRN/(.+?)_Record");
	public static final Pattern URI_PATTEN = Pattern.compile("http://lsrn.org/([^:]+):(.+)");
	public static final String OUTPUT_URI_PATTERN = "http://lsrn.org/$NS:$ID";
	public static final Pattern ID_TYPE_PATTERN = Pattern.compile("http://purl.oclc.org/SADI/LSRN/(.+?)_Identifier");
	public static final String OUTPUT_ID_TYPE_PATTERN = "http://purl.oclc.org/SADI/LSRN/$NS_Identifier";
	public static final Pattern ID_PATTERN = Pattern.compile(".*[/:#]([^\\s\\.]+)");
	
	public static boolean isLSRNType(Resource type)
	{
		return type.isURIResource() && isLSRNType(type.getURI());
	}

	public static boolean isLSRNType(String uri)
	{
		return NS_PATTERN.matcher(uri).matches(); 
	}
	
	public static String getNamespaceFromLSRNTypeURI(String uri)
	{
		Matcher typeMatcher = NS_PATTERN.matcher(uri);
		if (typeMatcher.matches())
			return typeMatcher.group(1);
		else
			return null;
	}
	
	public static boolean isLSRNIdentifierType(Resource type)
	{
		return type.isURIResource() && isLSRNIdentifierType(type.getURI());
	}

	public static boolean isLSRNIdentifierType(String uri)
	{
		return ID_TYPE_PATTERN.matcher(uri).matches(); 
	}
	
	public static String getNamespaceFromLSRNIdentifierTypeURI(String uri)
	{
		Matcher typeMatcher = ID_TYPE_PATTERN.matcher(uri);
		if (typeMatcher.matches())
			return typeMatcher.group(1);
		else
			return null;
	}
	
	/**
	 * Returns a Resource that is an instance of the specified type with the 
	 * specified ID.  The Resource will be created with an appropriate URI
	 * (@link {@link #OUTPUT_URI_PATTERN}).
	 * @param type the type (class) of the new instance
	 * @param id the id of the new instance
	 * @return a Resource view of the new instance
	 */
	public static Resource createInstance(Resource type, String id)
	{
		return createInstance(ModelFactory.createDefaultModel(), type, id);
	}
	
	/**
	 * Returns a Resource in the specified model that is an instance of the 
	 * specified type with the specified ID.  The Resource will be created 
	 * with an appropriate URI (@link {@link #OUTPUT_URI_PATTERN}).
	 * @param model the model in which to create the new Resource
	 * @param type the type (class) of the new instance
	 * @param id the id of the new instance
	 * @return a Resource view of the new instance
	 */
	public static Resource createInstance(Model model, Resource type, String id)
	{
		Matcher matcher = NS_PATTERN.matcher(type.getURI());
		if (matcher.find()) {
			String uri = OUTPUT_URI_PATTERN.replace("$NS", matcher.group(1)).replace("$ID", id);
			Resource lsrnNode = model.createResource(uri, type);
			addIdentifier(lsrnNode);
			return lsrnNode;
		} else {
			throw new IllegalArgumentException("at present this method only works with LSRN database record classes");
		}
	}
	
	/**
	 * Adds the LSRN SIO identifier structure to an LSRN-typed Resource.
	 * Attempts to use the URL of the Resource to determine the ID.
	 * @param lsrnNode
	 */
	public static void addIdentifier(Resource lsrnNode)
	{
		if (lsrnNode.isURIResource()) {
			Matcher idMatcher = ID_PATTERN.matcher(lsrnNode.getURI());
			if (idMatcher.find()) {
				String id = idMatcher.group(1);
				for (Resource type: RdfUtils.getTypes(lsrnNode).toList()) {
					if (!type.isURIResource())
						continue;
					Matcher typeMatcher = NS_PATTERN.matcher(type.getURI());
					if (typeMatcher.find()) {
						String idTypeURI = OUTPUT_ID_TYPE_PATTERN.replace("$NS", typeMatcher.group(1));
						SIOUtils.createAttribute(lsrnNode, SIO.has_identifier, ResourceFactory.createResource(idTypeURI), id);
					}
				}
			}
		}
	}
	
	/**
	 * Returns the identifier of an LSRN-typed Resource
	 * @param lsrnNode
	 * @return the identifier
	 */
	public static String getID(Resource lsrnNode)
	{
		Set<String> namespaces = new HashSet<String>();
		for (Iterator<Resource> types = RdfUtils.getTypes(lsrnNode); types.hasNext(); ) {
			Resource type = types.next();
			if (type.isURIResource()) {
				String ns = getNamespaceFromLSRNTypeURI(type.getURI());
				if (ns != null)
					namespaces.add(ns);
			}
		}
		
		Iterator<Resource> ids = RdfUtils.getPropertyValues(lsrnNode, SIO.has_identifier, null)
		                .andThen(RdfUtils.getPropertyValues(lsrnNode, SIO.has_attribute, null));
		while (ids.hasNext()) {
			Resource identifier = ids.next();
			for (Iterator<Resource> types = RdfUtils.getTypes(identifier); types.hasNext(); ) {
				Resource type = types.next();
				if (type.isURIResource()) {
					String ns = getNamespaceFromLSRNIdentifierTypeURI(type.getURI());
					if (ns != null && namespaces.contains(ns)) {
						Statement s = identifier.getProperty(SIO.has_value);
						if (s != null) {
							return s.getString();
						}
					}
				}
				
			}
		}
		
		return null;
	}
}
