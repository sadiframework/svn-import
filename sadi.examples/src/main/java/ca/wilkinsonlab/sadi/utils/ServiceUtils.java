package ca.wilkinsonlab.sadi.utils;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Resource;

public class ServiceUtils 
{
	protected static final Log log = LogFactory.getLog(ServiceUtils.class);

	/**
	 * Determine the database identifier of the given node. The method will first try
	 * to obtain an explicitly specified ID as encoded by the SIO attribute structure: 
	 *
	 *   root [
	 *     'has attribute' (SIO_000008)
	 *       [
	 *         rdf:type $identifierClass
	 *         'has value' (SIO_000300) $ID
	 *       ]
	 *   ]
	 * 
	 * If no such structure is attached to root, then the method will fall back
	 * to parsing the ID from the root's URI, using the given regular expression
	 * patterns. Each pattern should have one capturing group that indicates 
	 * the place in the URI where the ID occurs.
	 * 
	 * @param root the root resource
	 * @param identifierClass type of database identifier
	 * @param uriPatterns a collection of regular expression pattern for parsing the 
	 * database identifier from the given node's URI 
	 * @return the database identifier of the given node 
	 */
	public static String getDatabaseId(Resource root, Resource identifierClass, Pattern[] uriPatterns) 
	{
		Collection<String> identifiers = SIOUtils.getAttributeValues(root, identifierClass);
		
		if (identifiers.size() > 0) {
			if(identifiers.size() > 1) {
				SIOUtils.log.warn(String.format("%s has database IDs of type %s, returning only the first ID", root, identifierClass));
			}
			return identifiers.iterator().next();
		}
		
		SIOUtils.log.info(String.format("%s has no explicit ID, attempting to parse URI", root));
		if (!root.isURIResource()) {
			SIOUtils.log.warn("could not determine the database ID, resource has no attached ID and no URI");
			return null;
		}
		
		String uri = root.getURI();
		for (Pattern pattern: uriPatterns) {
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

}
