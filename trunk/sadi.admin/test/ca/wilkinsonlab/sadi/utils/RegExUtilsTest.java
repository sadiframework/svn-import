package ca.wilkinsonlab.sadi.utils;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import ca.wilkinsonlab.sadi.utils.RegExUtils.URIRegExBuilder;

public class RegExUtilsTest {

	@Test
	public void testEscapeForRegEx() 
	{
		final String input = "abcd\\.*efgh^$[]-+ijkl|()mnop";
		final String expectedOutput = "abcd\\\\\\.\\*efgh\\^\\$\\[\\]\\-\\+ijkl\\|\\(\\)mnop";

		try {
			String actualOutput = RegExUtils.escapeRegEx(input); 
			assertTrue(actualOutput.equals(expectedOutput));
		} catch (Exception e) {
			fail("Failed to escape metacharacters in regular expression: " + e);
		}
	}
	
	@Test
	public void testCollapsingURIRegexes() {
		
		/*
		 * smallRegExBuilder should recognize that all three prefixes don't
		 * fit under the length limit, identify that "http://kegg.bio2rdf.org/kegg:"
		 * is the longest common prefix, and then replace "http://kegg.bio2rdf.org/kegg:a:"
		 * and "http://kegg.bio2rdf.org/kegg:b:" with "http://kegg.bio2rdf.org/kegg:".
		 */
		
		Set<String> testURIs = new HashSet<String>();
		
		testURIs.add("http://kegg.bio2rdf.org/kegg:a:gene1");
		testURIs.add("http://kegg.bio2rdf.org/kegg:b:gene2");
		testURIs.add("http://uniprot.org/core:protein1");

		URIRegExBuilder largeRegExBuilder = new URIRegExBuilder(2048);
		for(String uri : testURIs) {
			largeRegExBuilder.addURIPrefixToRegEx(uri);
		}
		
		int totalLength = largeRegExBuilder.getRegEx().length();
		
		// set the max regex length to one character less than is needed for all three prefixes
		URIRegExBuilder smallRegExBuilder = new URIRegExBuilder(totalLength - 1);
		
		for(String uri : testURIs) {
			smallRegExBuilder.addURIPrefixToRegEx(uri);
		}
		
		assertFalse(smallRegExBuilder.getRegEx().contains("^http://kegg\\.bio2rdf\\.org/kegg:a:"));
		assertFalse(smallRegExBuilder.getRegEx().contains("^http://kegg\\.bio2rdf\\.org/kegg:b:"));
		assertTrue(smallRegExBuilder.getRegEx().contains("^http://kegg\\.bio2rdf\\.org/kegg:"));
		assertTrue(smallRegExBuilder.getRegEx().contains("^http://uniprot\\.org/core:"));
	}
}
