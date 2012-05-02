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
	public static final Pattern TYPE_PATTERN = Pattern.compile("http://purl.oclc.org/SADI/LSRN/(.+?)_Record");
	public static final String OUTPUT_TYPE_PATTERN = "http://purl.oclc.org/SADI/LSRN/$NS_Record";
	public static final Pattern URI_PATTERN = Pattern.compile("http://lsrn.org/([^:]+):(.+)");
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
		return TYPE_PATTERN.matcher(uri).matches();
	}

	public static boolean isLSRNURI(String uri)
	{
		return URI_PATTERN.matcher(uri).matches();
	}

	public static String getNamespaceFromLSRNTypeURI(String uri)
	{
		Matcher typeMatcher = TYPE_PATTERN.matcher(uri);
		if (typeMatcher.matches())
			return typeMatcher.group(1);
		else
			return null;
	}

	public static String getNamespaceFromLSRNURI(String uri)
	{
		Matcher matcher = URI_PATTERN.matcher(uri);
		if (matcher.matches())
			return matcher.group(1);
		else
			return null;
	}

	public static String getIDFromLSRNURI(String uri)
	{
		Matcher matcher = URI_PATTERN.matcher(uri);
		if (matcher.matches())
			return matcher.group(2);
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
		Matcher matcher = TYPE_PATTERN.matcher(type.getURI());
		if (matcher.find()) {
			String namespace = matcher.group(1);
			String uri = getURI(namespace, id);
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
					Matcher typeMatcher = TYPE_PATTERN.matcher(type.getURI());
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

	private static final String constructQueryPattern =
		"PREFIX lsrn: <http://purl.oclc.org/SADI/LSRN/>\n" +
		"PREFIX sio: <http://semanticscience.org/resource/>\n" +
		"CONSTRUCT {\n" +
		"  ?input a lsrn:$NS_Record .\n" +
		"  ?input sio:SIO_000671 ?id .\n" +
		"  ?id a lsrn:$NS_Identifier .\n" +
		"  ?id sio:SIO_000300 ?value .\n" +
		"}";
	public static String getConstructQuery(String lsrnTypeURI)
	{
		Matcher typeMatcher = TYPE_PATTERN.matcher(lsrnTypeURI);
		if (!typeMatcher.matches())
			throw new IllegalArgumentException("not an LSRN type");

		return constructQueryPattern.replaceAll("\\$NS", typeMatcher.group(1));
	}

	public static String getURI(String namespace, String id)
	{
		return OUTPUT_URI_PATTERN.replace("$NS", namespace).replace("$ID", id);
	}

	public static String getClassURI(String namespace)
	{
		return OUTPUT_TYPE_PATTERN.replace("$NS", namespace);
	}

	public static String getIdentifierClassURI(String namespace)
	{
		return OUTPUT_ID_TYPE_PATTERN.replace("$NS", namespace);
	}
}