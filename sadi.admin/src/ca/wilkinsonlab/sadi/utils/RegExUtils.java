package ca.wilkinsonlab.sadi.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExUtils {

	final static Pattern regexMetaChars = Pattern.compile("[\\[\\]$^.()+*|\\\\-]");
	
	/**
	 * Escape any regular expression metacharacters (e.g. ".") appearing in a string, so 
	 * that they are interpreted as literal characters by the Java regular expression engine.
	 * 
	 * @param literalRegEx the input string 
	 * @return the escaped version of literalRegEx
	 */
	static public String escapeRegEx(String literalRegEx) 
	{
		Matcher matcher = regexMetaChars.matcher(literalRegEx);
		return matcher.replaceAll("\\\\$0");
	}

}
