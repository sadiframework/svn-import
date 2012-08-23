package org.sadiframework.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.vocab.SIO;

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
	protected static final Log log = LogFactory.getLog(LSRNUtils.class);

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
			SIOUtils.createAttribute(lsrnNode, SIO.has_identifier, getIdentifierClass(namespace), id);
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

	/**
	 * <p>Returns the LSRN identifier for the given node. The method will first try
	 * to obtain an explicitly specified ID as encoded by the following
	 * SIO attribute structure:</p>
	 *
	 * <pre>
	 * {@code
	 *   root [
	 *     'has attribute'/'has identifier' (SIO_000008/SIO_000067)
	 *       [
	 *         rdf:type $identifierClass;
	 *         'has value' (SIO_000300) $ID
	 *       ]
	 *   ]
	 * }
	 * </pre>
	 *
	 * <p>If no such structure is attached to root, then the method will fall back
	 * to parsing the ID from the root's URI, using the regular expressions
	 * associated with the given LSRN identifier class.</p>
	 *
	 * @param root the root resource
	 * @param lsrnIdentifierType the LSRN identifier type URI (e.g. lsrn:UniProt_Identifier).
	 * @return the database identifier of the given node
	 */
	public static String getID(Resource root, Resource lsrnIdentifierType)
	{
		Collection<String> identifiers = SIOUtils.getAttributeValues(root, lsrnIdentifierType);
		identifiers.addAll(SIOUtils.getAttributeValues(root, SIO.has_identifier, lsrnIdentifierType));

		if (identifiers.size() > 0) {
			if(identifiers.size() > 1) {
				log.warn(String.format("%s has multiple IDs of type %s, returning only the first ID", root, lsrnIdentifierType));
			}
			return identifiers.iterator().next();
		}

		log.info(String.format("%s has no explicit ID, attempting to parse URI", root));
		if (!root.isURIResource()) {
			log.warn("could not determine the database ID, resource has no attached LSRN ID and is a blank node");
			return null;
		}

		String uri = root.getURI();
		for (Pattern pattern: Config.getConfig().getURIPatterns(lsrnIdentifierType)) {
			Matcher matcher = pattern.matcher(uri);
			if (matcher.groupCount() < 1) {
				log.warn(String.format("URI pattern '%s' does not contain any capturing groups", pattern));
				continue;
			}
			if (matcher.find()) {
				String match = matcher.group(1);
				if (!match.isEmpty())
					return match;
			}
		}

		log.warn(String.format("could not determine lsrn ID for %s, it has no attached ID and does not match any known URI pattern", root));
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

	public static Resource getClass(String namespace)
	{
		return ResourceFactory.createResource(getClassURI(namespace));
	}

	public static String getClassURI(String namespace)
	{
		return OUTPUT_TYPE_PATTERN.replace("$NS", namespace);
	}

	public static Resource getIdentifierClass(String namespace)
	{
		return ResourceFactory.createResource(getIdentifierClassURI(namespace));
	}

	public static String getIdentifierClassURI(String namespace)
	{
		return OUTPUT_ID_TYPE_PATTERN.replace("$NS", namespace);
	}

	public static class Config
	{
		public static final Log log = LogFactory.getLog(Config.class);
		public static final String CONFIG_FILENAME = "lsrn.properties";
		public static final String ROOT_CONFIG_KEY = "lsrn";
		public static final String URI_PATTERN_CONFIG_KEY = "uripattern";
		public static final String FAILSAFE_URI_PATTERN_CONFIG_KEY = "uripattern.failsafe";
		public static final Pattern DEFAULT_FAILSAFE_URI_PATTERN = Pattern.compile(".*[/:#]([^\\s\\.]+)");

		protected static Config theInstance;
		protected Configuration config;
		protected Map<Resource, List<Pattern>> lsrnIdTypeToUriPattern;

		static public synchronized Config getConfig()
		{
			if (theInstance == null)
				theInstance = new Config();
			return theInstance;
		}

		protected Config()
		{
			try {
				config = new PropertiesConfiguration(CONFIG_FILENAME);
			} catch (ConfigurationException e) {
				log.warn(String.format("error reading %s: %s", CONFIG_FILENAME, e));
				config = new PropertiesConfiguration();
			}
			lsrnIdTypeToUriPattern = new Hashtable<Resource, List<Pattern>>();
			Configuration subconfig = config.subset(ROOT_CONFIG_KEY);
			for (Iterator<?> i = subconfig.getKeys(); i.hasNext(); ) {
				String key = (String)i.next();
				String parts[] = key.split("\\.", 2);
				if (parts.length == 2) {
					String lsrnNamespace = parts[0];
					Resource lsrnIdType = ResourceFactory.createResource(getIdentifierClassURI(lsrnNamespace));
					String subkey = parts[1];
					for (Object patternStr : subconfig.getList(key)) {
						Pattern pattern = Pattern.compile((String)patternStr);
						if (subkey.equals(URI_PATTERN_CONFIG_KEY))
							addURIPattern(lsrnIdType, pattern);
						else if (subkey.equals(FAILSAFE_URI_PATTERN_CONFIG_KEY))
							setFailsafeURIPattern(lsrnIdType, pattern);
					}
				}
			}
		}

		protected void addURIPattern(Resource lsrnIdentifierType, Pattern uriPattern)
		{
			if (!lsrnIdTypeToUriPattern.containsKey(lsrnIdentifierType))
				initPatternList(lsrnIdentifierType);
			// failsafe URI pattern must always be last; pop it off and then add it back on after new entry
			List<Pattern> uriPatterns = lsrnIdTypeToUriPattern.get(lsrnIdentifierType);
			Pattern failsafeURIPattern = (Pattern)uriPatterns.remove(uriPatterns.size() - 1);
			uriPatterns.add(uriPattern);
			uriPatterns.add(failsafeURIPattern);
		}

		protected void setFailsafeURIPattern(Resource lsrnIdentifierType, Pattern uriPattern)
		{
			if (!lsrnIdTypeToUriPattern.containsKey(lsrnIdentifierType))
				initPatternList(lsrnIdentifierType);
			List<Pattern> uriPatterns = lsrnIdTypeToUriPattern.get(lsrnIdentifierType);
			uriPatterns.remove(uriPatterns.size() - 1);
			uriPatterns.add(uriPattern);
		}

		protected void initPatternList(Resource lsrnIdentifierType)
		{
			List<Pattern> uriPatterns = new ArrayList<Pattern>();
			// add default LSRN URI pattern
			String lsrnNamespace = getNamespaceFromLSRNIdentifierTypeURI(lsrnIdentifierType.getURI());
			uriPatterns.add(Pattern.compile(String.format("http://lsrn\\.org/%s:(.+)", lsrnNamespace)));
			// add failsafe URI pattern (must always be last in list)
			uriPatterns.add(DEFAULT_FAILSAFE_URI_PATTERN);
			lsrnIdTypeToUriPattern.put(lsrnIdentifierType, uriPatterns);
		}

		public List<Pattern> getURIPatterns(Resource lsrnIdentifierType)
		{
			if (!lsrnIdTypeToUriPattern.containsKey(lsrnIdentifierType))
				initPatternList(lsrnIdentifierType);
			return lsrnIdTypeToUriPattern.get(lsrnIdentifierType);
		}

	}
}
