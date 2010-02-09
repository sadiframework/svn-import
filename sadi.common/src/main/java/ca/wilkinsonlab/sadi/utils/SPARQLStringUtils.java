package ca.wilkinsonlab.sadi.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

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
	public final static Logger log = Logger.getLogger(SPARQLStringUtils.class);

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
	throws IllegalArgumentException
	{
		Matcher matcher = CONVERSION_SPECIFIERS.matcher(template);
		String output = template;
		int i;
		for (i=0; i<substStrings.length && matcher.find(); ++i) {
			String match = matcher.group();
			if (match.equals("%u%")) {
				//output = output.replaceFirst(match, String.format("<%s>", escapeURI(substStrings[i])));
				output = StringUtils.replaceOnce(output, match, String.format("<%s>", escapeURI(substStrings[i])));
			}
			else if (match.equals("%s%")) {
				String escaped = escapeSPARQL(substStrings[i]); //escapeSPARQL(escapeSPARQL(substStrings[i]));
				//output = output.replaceFirst(match, String.format("\"%s\"", escaped));
				output = StringUtils.replaceOnce(output, match, String.format("\"%s\"", escaped));
			}
			else if (match.equals("%v%")) {
				//output = output.replaceFirst(match, substStrings[i]);
				output = StringUtils.replaceOnce(output, match, substStrings[i]);
			}
		}
		
		if (matcher.find())
			throw new IllegalArgumentException("template specified a substitution point that was never filled");
		if (i < substStrings.length)
			throw new IllegalArgumentException("a supplied value was never used in the template");
			
		return output;
	}

	/**
	 * Return a quoted version of the given string, that is safe to insert
	 * into a SPARQL query.  
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
	public static String escapeURI(String uri)
	{
		try {
			return new URI(uri, false).getEscapedURIReference();
		} catch (URIException e) {
			throw new IllegalArgumentException(e.toString());
		}
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

	public static String getSPARQLQueryString(List<Triple> basicGraphPattern) 
	{
		StringBuilder query = new StringBuilder();
		query.append("SELECT * WHERE {\n");
		for(Triple triple : basicGraphPattern) {
			query.append(getTriplePattern(triple));
			query.append("\n");	
		}
		query.append("}");
		String queryStr = query.toString();
	
		return queryStr;
	}

	public static String getTriplePattern(Triple triple) 
	{
		StringBuilder template = new StringBuilder();
		Node sNode = triple.getSubject();
		Node pNode = triple.getPredicate();
		Node oNode = triple.getObject();
		
		if(sNode.isURI())
			template.append("%u% ");
		else
			template.append("%v% ");
		
		if(pNode.isURI())
			template.append("%u% ");
		else
			template.append("%v% ");
		
		String objectDatatypeURI = null;
		
		if(oNode.isURI())
			template.append("%u% .");
		else if(oNode.isVariable()) 
			template.append("%v% .");
		else if(oNode.isLiteral()) {
			if(oNode.getLiteralDatatype() == null) {
				template.append("%s% .");
			}
			else {
				String typeURI = oNode.getLiteralDatatypeURI();
				
				// These builtin types can be included in the query
				// without quotes, and their type will be correctly 
				// interpreted.  For any other types, the value must
				// be quoted and the datatype explicitly stated. --BV
				
				if(typeURI.equals(XSDDatatype.XSDboolean.getURI()) ||
				   typeURI.equals(XSDDatatype.XSDinteger.getURI()) ||
				   typeURI.equals(XSDDatatype.XSDdecimal.getURI()) ||
				   typeURI.equals(XSDDatatype.XSDfloat.getURI()) ) 
				{
					template.append("%v% .");
				}
				else {
					template.append("%s%^^%u% .");
					objectDatatypeURI = typeURI;
				}
			}
		}
		else if(oNode.isBlank()) {
			template.append("%u% .");
		}

		String s = RdfUtils.getPlainString(sNode);
		String p = RdfUtils.getPlainString(pNode);
		String o = RdfUtils.getPlainString(oNode);
		String tripleStr = null;
		
		if(objectDatatypeURI != null)
			tripleStr = strFromTemplate(template.toString(), s, p, o, objectDatatypeURI);
		else
			tripleStr = strFromTemplate(template.toString(), s, p, o);
		
		return tripleStr;
	}
}
