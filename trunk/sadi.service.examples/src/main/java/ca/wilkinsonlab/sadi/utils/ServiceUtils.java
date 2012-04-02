package ca.wilkinsonlab.sadi.utils;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.vocab.SIO;
import ca.wilkinsonlab.sadi.vocab.LSRN.LSRNRecordType;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class ServiceUtils
{
	protected static final Log log = LogFactory.getLog(ServiceUtils.class);

	/**
	 * Determine the database identifier of the given node. The method will first try
	 * to obtain an explicitly specified ID as encoded by the SIO attribute structure:
	 *
	 *   root [
	 *     'has attribute'/'has identifier' (SIO_000008/SIO_000067)
	 *       [
	 *         rdf:type $identifierClass
	 *         'has value' (SIO_000300) $ID
	 *       ]
	 *   ]
	 *
	 * If no such structure is attached to root, then the method will fall back
	 * to parsing the ID from the root's URI, using the regular expressions
	 * associated with the given LSRN record type.
	 *
	 * @param root the root resource
	 * @param recordType the LSRN record type (e.g. "UniProt").  This class
	 * provides the type URI for the identifier and also the regular expressions
	 * for parsing the identifier from a URI.
	 * @return the database identifier of the given node
	 */
	public static String getDatabaseId(Resource root, LSRNRecordType recordType)
	{
		Collection<String> identifiers = SIOUtils.getAttributeValues(root, recordType.getIdentifierTypeURI());
		identifiers.addAll(SIOUtils.getAttributeValues(root, SIO.has_identifier, recordType.getIdentifierTypeURI()));

		if (identifiers.size() > 0) {
			if(identifiers.size() > 1) {
				SIOUtils.log.warn(String.format("%s has database IDs of type %s, returning only the first ID", root, recordType.getIdentifierTypeURI()));
			}
			return identifiers.iterator().next();
		}

		SIOUtils.log.info(String.format("%s has no explicit ID, attempting to parse URI", root));
		if (!root.isURIResource()) {
			SIOUtils.log.warn("could not determine the database ID, resource has no attached ID and no URI");
			return null;
		}

		String uri = root.getURI();
		for (Pattern pattern: recordType.getURIPatterns()) {
			Matcher matcher = pattern.matcher(uri);
			if (matcher.groupCount() < 1) {
				SIOUtils.log.warn("URI pattern '%s' does not contain any capturing groups");
				continue;
			}
			if (matcher.find()) {
				String match = matcher.group(1);
				if (!StringUtils.isEmpty(match))
					return match;
			}
		}

		SIOUtils.log.warn(String.format("could not determine database ID for %s, it has no attached ID and does not match any of the given URI patterns",root));
		return null;
	}

	public static Resource createLSRNRecordNode(Model model, LSRNRecordType recordType, String id)
	{
		String uri = String.format("%s%s", recordType.getUriPrefix(), id);
		Resource recordNode = model.createResource(uri, recordType.getRecordTypeURI());

		// add SIO identifier structure
		SIOUtils.createAttribute(recordNode, recordType.getIdentifierTypeURI(), id);

		return recordNode;
	}
}
