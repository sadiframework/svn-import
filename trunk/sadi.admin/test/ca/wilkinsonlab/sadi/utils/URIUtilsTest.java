package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class URIUtilsTest 
{

	@Test
	public void testGetURIPrefix()
	{
		// typical URI
		assertTrue(URIUtils.getURIPrefix("http://bio2rdf.org/uniprot:P12345").equals("http://bio2rdf.org/uniprot:"));
		
		// URI ends with prefix delimiter
		assertTrue(URIUtils.getURIPrefix("http://one/two/").equals("http://one/"));

		// URI consists solely of a prefix delimiter
		assertTrue(URIUtils.getURIPrefix("#") == null);

		// URI ends with prefix delimiter, but has no other delimiters
		assertTrue(URIUtils.getURIPrefix("contains.no.delimiters#") == null);
		
		// URI has no prefix delimiters
		assertTrue(URIUtils.getURIPrefix("contains.no.delimiters") == null);
		
		// URI is empty string
		assertTrue(URIUtils.getURIPrefix("") == null);

		// URI is null
		assertTrue(URIUtils.getURIPrefix(null) == null);
		
		
	}
	
}
