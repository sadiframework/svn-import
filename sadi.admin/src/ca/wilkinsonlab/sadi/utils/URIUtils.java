package ca.wilkinsonlab.sadi.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ca.wilkinsonlab.sadi.admin.Config;

public class URIUtils 
{
	/** 
	 * Exceptions to the normal procedure of extracting a prefix from a URI,
	 * which is to take the string up to the last occurrence 
	 * of "#", "/", ":".
	 */
	private final static Set<String> predefinedURIPrefixes;
	private final static String URIPrefixDelimiters[] = { "/", "#", ":" };
	
	static {
		predefinedURIPrefixes = new HashSet<String>();
		for(Object prefix : Config.getConfiguration().getList("sadi.registry.sparql.predefinedURIPrefix")) {
			predefinedURIPrefixes.add((String)prefix);
		}
	}

	static public String getURIPrefix(String URI) 
	{
		if (URI == null) {
			return null;
		}
		
		// check for special cases first
		for(String prefix : predefinedURIPrefixes) {
			if(URI.startsWith(prefix)) {
				return prefix;
			}
		}
		
		if(URI.length() <= 1) {
			return null;
		}
		
		// ignore delimiters that occur as the last character
		URI = StringUtils.left(URI, URI.length() - 1);
		
		int chopIndex = StringUtils.lastIndexOfAny(URI, URIPrefixDelimiters);

		String prefix;
		if (chopIndex == -1)
			return null;
		else {
			chopIndex++; // we want to include the last "/", ":", or "#" in the prefix
			prefix = StringUtils.substring(URI, 0, chopIndex);
		}

		return prefix;
	}
	
}
