package ca.wilkinsonlab.sadi.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.admin.Config;

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

	public static class URIRegExBuilder {

		public final static Logger log = Logger.getLogger(URIRegExBuilder.class);

		protected int maxRegExLengthInChars;
		protected Set<String> prefixes = new HashSet<String>();
		protected boolean regExHasExceededMaxLength;
		/** 
		 * Exceptions to the normal procedure of extracting a prefix from a URI,
		 * which is to take the string up to the last occurrence 
		 * of "#", "/", ":".
		 */
		protected Set<String> predefinedPrefixes;
		
		public URIRegExBuilder(int maxRegExLengthInChars) {
			setMaxRegExLengthInChars(maxRegExLengthInChars);
			setRegExHasExceededMaxLength(false);
			predefinedPrefixes = new HashSet<String>();
			for(Object prefix : Config.getConfiguration().getList("sadi.registry.sparql.predefinedURIPrefix")) {
				predefinedPrefixes.add((String)prefix);
			}
		}
	
		protected int getMaxRegExLengthInChars() {
			return maxRegExLengthInChars;
		}
	
		public void setMaxRegExLengthInChars(int maxRegExLengthInChars) {
			this.maxRegExLengthInChars = maxRegExLengthInChars;
		}
		
		public boolean regExIsTruncated() {
			return regExHasExceededMaxLength;
		}
	
		public void setRegExHasExceededMaxLength(boolean regExHasExceededMaxLength) {
			this.regExHasExceededMaxLength = regExHasExceededMaxLength;
		}
		
		public void addPrefixOfURIToRegEx(String uri) {
			
			if(uri.trim().length() == 0) {
				throw new IllegalArgumentException("expected non-empty string for uri");
			}
			if(!regExIsTruncated()) {
				String prefix = getURIPrefix(uri);
				if (!prefixes.contains(prefix)) {
					log.trace("adding prefix " + prefix + " to uri regex");
					prefixes.add(prefix);
					
					// merge prefixes as needed, in order to keep the regular expression under the required length
					while(getRegEx().length() > getMaxRegExLengthInChars()) {
						if(!collapsePrefixSet()) {
							log.warn("regex exceeds max length of " + getMaxRegExLengthInChars() + " chars, regex will be truncated");
							prefixes.remove(prefix);
							setRegExHasExceededMaxLength(true);
						}
					}
				}
			}
		}
		
		/**
		 * Merge two or more prefixes by replacing them with a shorter 
		 * prefix that matches them all.
		 */
		protected boolean collapsePrefixSet() {
			
			// find the longest prefix string that is shared by two or more
			// of the current prefixes 
			
			String longestSharedPrefix = null;
			int longestSharedPrefixLength = -1;
			
			for(String prefix : prefixes) {
				for(int i = 0; i < prefix.length(); i++) {
					String candidate = StringUtils.substring(prefix, 0, i);
					boolean foundMatch = false;
					for(String prefix2 : prefixes) {
						if(prefix2.equals(prefix)) {
							continue;
						}
						if(prefix2.startsWith(candidate)) {
							foundMatch = true;
							break;
						}
					}
					if(foundMatch && candidate.length() > longestSharedPrefixLength) {
						longestSharedPrefixLength = candidate.length();
						longestSharedPrefix = candidate;
					}
				}
			}
			
			if(longestSharedPrefix == null) {
				log.warn("unable to find common prefix, to reduce prefix set");
				return false;
			}
			
			Set<String> replacedPrefixes = new HashSet<String>();
			for(String prefix : prefixes) {
				if(prefix.startsWith(longestSharedPrefix)) {
					replacedPrefixes.add(prefix);
				}
			}
			
			log.trace("reducing regex by replacing prefixes " + URIRegExBuilder.buildRegExFromPrefixes(replacedPrefixes) 
					+ " with " + URIRegExBuilder.buildRegExFromPrefixes(Collections.singleton(longestSharedPrefix)));
	
			for(String prefix : replacedPrefixes) {
				prefixes.remove(prefix);
			}
			prefixes.add(longestSharedPrefix);
	
			return true;
		}
		
		public String getURIPrefix(String uri) 
		{
			// check for special cases first
			for(String prefix : predefinedPrefixes) {
				if(uri.startsWith(prefix)) {
					return prefix;
				}
			}

			final String delimiters[] = { "/", "#", ":" };
			
			// ignore delimiters that occur as the last character
			if(StringUtils.lastIndexOfAny(uri, delimiters) == (uri.length() - 1))
				uri = StringUtils.left(uri, uri.length() - 1);
			
			int chopIndex = StringUtils.lastIndexOfAny(uri, delimiters);
	
			String prefix;
			if (chopIndex == -1)
				prefix = uri;
			else {
				chopIndex++; // we want to include the last "/", ":", or "#" in the prefix
				prefix = StringUtils.substring(uri, 0, chopIndex);
			}

			return prefix;
		}
		
		public static String buildRegExFromPrefixes(Set<String> uriPrefixes) {
			
			StringBuffer regex = new StringBuffer();
			int count = 0;
			for (String prefix : uriPrefixes) {
				regex.append("^");
				regex.append(escapeRegEx(prefix));
				if (count < uriPrefixes.size() - 1)
					regex.append("|");
				count++;
			}
			return regex.toString();
		}
		
		public String getRegEx() {
			return buildRegExFromPrefixes(this.prefixes);
		}
		
	}
}
