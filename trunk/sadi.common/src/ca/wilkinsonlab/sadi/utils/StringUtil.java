package ca.wilkinsonlab.sadi.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

/*********************************************************
 * Various routines for filling string templates and 
 * performing string and URI escaping.
 * 
 * This entire class was necessary because the Virtuoso
 * JDBC driver doesn't seem to support prepared statements for
 * SPARQL (at least not with parameter substitution).
 * 
 * @author Ben Vandervalk
 **********************************************************/

public class StringUtil
{
	static Pattern CONVERSION_SPECIFIERS = Pattern.compile("%[usv]%");
	
//	/*****************************************************
//	 * Return the string in 'str', after the rightmost
//	 * occurence of 'ch'.  If 'ch' is not found in 'str',
//	 * return 'str' in its entirety.
//	 ****************************************************/
//	
//	static public String strRightOf(String str, char ch)
//	{
//		Pattern p = Pattern.compile("^.*" + ch);
//		Matcher m = p.matcher(str);
//
//		String output = m.replaceAll("");
//		
//		return output;
//	}
	
	/**
	 * Makes the specified variable substitutions in the
	 * specified template.
	 * This method is needed because the Virtuoso
	 * JDBC driver does not seem to support prepared statements
	 * for SPARQL (at least not with parameter substitution).
	 * 
	 * Spots where string substitutions are made are indicated
	 * by either "%u%", "%s%", and "%v%".  "%u%" will cause the 
	 * substituted string to be surrounded by angle brackets 
	 * and escaped according to the rules for absolute URIs.   
	 * "%s" will cause the substituted string to be surrounded
	 * by double quotes and escaped according to the rules
	 * for strings. "%v%" (stands for "verbatim"), will insert
	 * the given string as is, without any escaping.
	 * 
	 * Example: strFromTemplate("file.txt", "Johnny");
	 * 
	 * file.txt: 'Hello, my name is %s%.'
	 * 
	 * returns: 'Hello, my name is "Johnny"'
	 * 
	 * @param template the template string, into which the
	 * given list of strings will be inserted ('substStrings')
	 * 
	 * @param substStrings a list of strings to be inserted into
	 * 'template' at the points in template that look like 
	 * '%u%' (for absolute URIs) or '%s%' (for double quoted
	 * strings).
	 * 
	 * @return The string that results from inserting the given
	 * strings into 'template'.
	 */
	public static String strFromTemplate(String template, String ... substStrings)
	throws URIException, IllegalArgumentException
	{
		Matcher matcher = CONVERSION_SPECIFIERS.matcher(template);
		String output = template;
		int i;
		for (i=0; i<substStrings.length && matcher.find(); ++i) {
			String match = matcher.group();
			if (match.equals("%u%"))
				output = output.replaceFirst(match, String.format("<%s>", escapeURI(substStrings[i])));
			else if (match.equals("%s%"))
				output = output.replaceFirst(match, String.format("\"%s\"", escapeString(substStrings[i])));
			else if (match.equals("%v%"))
				output = output.replaceFirst(match, substStrings[i]);
		}
		
		if (matcher.find())
			throw new IllegalArgumentException("template specified a substitution point that was never filled");
		if (i < substStrings.length)
			throw new IllegalArgumentException("a supplied value was never used in the template");
			
		return output;
	}

	/**
	 * Loads a multi-line text file into a string, 
	 * and makes the specified variable substitutions
	 * according to strFromTemplate(String, String).
	 * 
	 * @param template The URL of the text file that contains
	 * the template.
	 * 
	 * @param substStrings a list of strings to be inserted into
	 *  the template
	 * 
	 * @throws URIException if there is an error parsing one of
	 * the strings as a URI.
	 * 
	 * @return The string that results from inserting the given
	 * strings into 'template'.
	 */	
	public static String strFromTemplate(URL template, String ... substStrings)
	throws URIException
	{
		try {
			return strFromTemplate(readFully(template), substStrings);
		} catch (IOException e) {
			/* this should never happen if the template is there, so don't
			 * force people to catch it...
			 */
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Escapes a string so that it may safely inserted, inside
	 * either double or single quotes, into a SPARQL query. 
	 * Tested against SPARQL-injection attack on Virtuoso.  
	 *
	 * @param str The string to be escaped. 
	 * @return The escaped string.
	 */
	public static String escapeString(String s)
	{
		String escaped = new String(s);
		
		escaped.replaceAll("\"", "\\\"");
		escaped.replaceAll("'","\'");
		escaped.replaceAll("\0", "\\\0");
		escaped.replaceAll("\n","\\n");
		escaped.replaceAll("\r", "\\r");
		escaped.replaceAll("\t", "\\t");
		
		// This one is a bit tricky.  All it does is escape any
		// slashes in the string, by replacing "\" with "\\".
		// However, the first argument (the regular expression)
		// has its escapes expanded twice - once by Java
		// and then a second time by the regular expression engine.
		// Hence "\\\\" really represents a literal "\" in the final
		// regular expression.
		
		escaped.replaceAll("\\\\", "\\\\");
		
		return escaped;
	}
	
	/**
	 * Escapes special characters ('>','<',' ','\t','\n','\r','\0','{','}')
	 * in a string representing a URI.  Tested against SPARQL-injection attack
	 * on Virtuoso.
	 * 
	 * @return a new escaped string.
	 */
	public static String escapeURI(String uri) throws URIException
	{
		return new URI(uri, false, "UTF-8").getEscapedURIReference();
	}
	
	/**
	 * Read a URL into a string.
	 * @throws IOException 
	 */
	public static String readFully(URL url) throws IOException
	{
		return readFully(url.openStream());
	}
	
	/**
	 * Read an InputStream into a string.
	 * @throws IOException
	 */
	public static String readFully(InputStream is) throws IOException
	{
		BufferedReader input = new BufferedReader(new InputStreamReader(is));
		StringBuilder buf = new StringBuilder();
		String line;
		while((line = input.readLine()) != null) {
			buf.append(line);
			buf.append("\n");
		}
		input.close();
		return buf.toString();
	}
}
