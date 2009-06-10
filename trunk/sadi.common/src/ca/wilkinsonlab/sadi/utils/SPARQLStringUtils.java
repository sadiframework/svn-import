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
import org.apache.commons.lang.StringEscapeUtils;

/*********************************************************
 * Various routines for building SPARQL query strings
 * from templates, escaping URIs, etc.
 * 
 * This entire class was necessary because the Virtuoso
 * JDBC driver doesn't seem to support prepared statements for
 * SPARQL (at least not with parameter substitution).
 * 
 * @author Ben Vandervalk
 **********************************************************/

public class SPARQLStringUtils
{
	static Pattern CONVERSION_SPECIFIERS = Pattern.compile("%[usv]%");
	
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
			else if (match.equals("%s%")) {
				String escaped = escapeSPARQL(escapeSPARQL(substStrings[i]));
				output = output.replaceFirst(match, String.format("\"%s\"", escaped));
			}
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
	 * Return a quoted version of the given string, that is safe to insert
	 * into a SPARQL query.  SPARQL has a special triple-quote mechanism which 
	 * allows for strings with embedded newlines and quotes.  (Escape codes 
	 * are treated just as they are in double-quoted or single-quoted strings.)
	 */
	static public String escapeSPARQL(String s)
	{
		// NOTE: Backslashes appearing in the arguments to replaceAll
		// are escaped twice -- once by Java and once by the regular 
		// expression engine.  Hence "\\\\" represents a single 
		// literal "\".
		
		s = s.replaceAll("\\\\", "\\\\\\\\");
		s = s.replaceAll("\"", "\\\\\\\"");
		s = s.replaceAll("'", "\\\\'");
		s = s.replaceAll("\n", "\\\\n");
		s = s.replaceAll("\r", "\\\\r");
		s = s.replaceAll("\t", "\\\\t");
		s = s.replaceAll("\f", "\\\\f");
		s = s.replaceAll("\b", "\\\\b");
		
		return s;
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
	 * Escapes special characters ('>','<',' ','\t','\n','\r','\0','{','}')
	 * in a string representing a URI.  Tested against SPARQL-injection attack
	 * on Virtuoso.
	 * 
	 * @return a new escaped string.
	 */
	public static String escapeURI(String uri) throws URIException
	{
		return new URI(uri, false).getEscapedURIReference();
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
