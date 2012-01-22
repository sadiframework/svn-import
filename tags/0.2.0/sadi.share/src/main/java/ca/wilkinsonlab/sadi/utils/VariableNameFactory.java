package ca.wilkinsonlab.sadi.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author Luke McCarthy
 */
public class VariableNameFactory
{
	private static final Pattern nonAlphaPrefixPattern = Pattern.compile("^([^a-zA-Z]+)");
	private static final Pattern numericSuffixPattern = Pattern.compile("(\\d+)$");
	
	public static String getVariableName(Resource r)
	{
		if (r == null)
			return "null";
		
		/* start with the label or local name...
		 */
		String variableName;
		if (r.hasProperty(RDFS.label)) {
			variableName = r.getProperty(RDFS.label).getString();
		} else if (r.isURIResource()) {
			if (r.getURI().contains("#"))
				variableName = StringUtils.substringAfterLast(r.getURI(), "#");
			else
				variableName = r.getLocalName();
		} else {
			return "anonymous";
		}
		
		/* strip non-alphabetic prefix...
		 */
		String nonAlphaPrefix = getNonAlphabeticPrefix(variableName);
		if (nonAlphaPrefix != null)
			variableName = StringUtils.removeStart(variableName, nonAlphaPrefix);
		
		/* replace spaces with underscores...
		 */
		variableName = StringUtils.replace(variableName, " ", "_");
		
		/* any other SPARLQ variable name rules I'm forgetting...?
		 */
		return variableName;
	}
	
	public static String getNonAlphabeticPrefix(String s)
	{
		Matcher matcher = nonAlphaPrefixPattern.matcher(s);
		if (matcher.find()) {
			return matcher.group();
		} else {
			return null;
		}
	}
	
	public static String getNextVariableName(String variableName)
	{
		if (variableName == null)
			throw new IllegalArgumentException("initial variable name cannot be null");
		
		String numericSuffix = getNumericSuffix(variableName);
		if (numericSuffix == null) {
			return variableName.concat("1");
		} else {
			/* the string matched numericSuffixPattern below, so we shouldn't
			 * expect a NumberFormatException...
			 */
			int n = Integer.parseInt(numericSuffix);
			String formatString = createFormatString(numericSuffix);
			return StringUtils.removeEnd(variableName, numericSuffix).concat(String.format(formatString, n+1));
		}
	}

	public static final String getNumericSuffix(String s)
	{
		Matcher matcher = numericSuffixPattern.matcher(s);
		if (matcher.find())
			return matcher.group();
		else
			return null;
	}
	
	public static final String createFormatString(String numericString)
	{
		StringBuilder buf = new StringBuilder("%");
		if (numericString.startsWith("0")) {
			buf.append("0");
			buf.append(numericString.length());
		}
		buf.append("d");
		return buf.toString();
	}
}
