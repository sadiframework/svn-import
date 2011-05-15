package ca.wilkinsonlab.sadi.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

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
		
		public URIRegExBuilder(int maxRegExLengthInChars) {
			setMaxRegExLengthInChars(maxRegExLengthInChars);
			setRegExHasExceededMaxLength(false);
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
				
				String prefix = URIUtils.getURIPrefix(uri);
				
				if(prefix == null) {
					log.warn(String.format("unable to determine URI prefix for %s, omitting from regular expression", uri));
					return;
				}
				
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
