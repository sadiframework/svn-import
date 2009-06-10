package ca.wilkinsonlab.sadi.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * @author Ben Vandervalk
 */
public class PredicateUtils
{
	static private Pattern invertedPredicatePattern = Pattern.compile("^inv\\((.+)\\)$");
	
	public static boolean isInverted(String predicate)
	{
		predicate = StringUtils.deleteWhitespace(predicate);
		Matcher matcher = invertedPredicatePattern.matcher(predicate);
		return matcher.find();
	}
	
	public static String invert(String predicate)
	{
		predicate = StringUtils.deleteWhitespace(predicate);
		if(isInverted(predicate)) {
			Matcher matcher = invertedPredicatePattern.matcher(predicate);
			if(!matcher.find()) 
				throw new RuntimeException("predicate did not match expected regular expression '^inv(.+)$'");
			predicate = matcher.group(1);
		}
		else
			predicate = "inv(" + predicate + ")";

		return predicate;
	}		
}
