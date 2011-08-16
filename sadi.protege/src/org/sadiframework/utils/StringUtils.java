package org.sadiframework.utils;

import java.util.regex.Pattern;

import org.apache.commons.lang.WordUtils;

public class StringUtils {

	private static Pattern startsWithLetter = Pattern.compile("^[a-zA-Z]");
    private static Pattern nonLetterOrDigit = Pattern.compile("\\W+");
	
	static public String escapeSingleQuotes(String str)	{
		if (str == null)
			return str;
		return str.replace("'", "\\'");
	}
	
	static public boolean isNullOrEmpty(String str) {
		if (str == null)
			return true;
		else if (str.trim().equals(""))
			return true;
		else
			return false;
	}

	static public String getPerlModuleName(String name) {
		String className = startsWithLetter.matcher(name).find() ? name : "SADI" + name;
		className = WordUtils.capitalize(className);
		return nonLetterOrDigit.matcher(className).replaceAll("");
	}

}
