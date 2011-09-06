package ca.wilkinsonlab.utils;

import org.apache.commons.lang.StringUtils;

public class URIUtils 
{
	private static final String[] PREFIX_DELIMITERS = { ":", "/", "#" };

	public static String getURIPrefix(String URI) 
	{
		String origURI = URI;
		
		// ignore delimiters that occur as the last character

		if(StringUtils.lastIndexOfAny(URI, PREFIX_DELIMITERS) == (URI.length() - 1))
			URI = StringUtils.left(URI, URI.length() - 1);
		
		int chopIndex = StringUtils.lastIndexOfAny(URI, PREFIX_DELIMITERS);

		String prefix;
		if (chopIndex == -1)
			prefix = origURI;
		else {
			chopIndex++; // we want to include the delimiter char (e.g. "#") in the prefix
			prefix = StringUtils.substring(URI, 0, chopIndex);
		}

		return prefix;
	}

}

