package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SPARQLStringUtilsTest
{
	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}
	
	@Test
	public void testPattern()
	{
		assertTrue(SPARQLStringUtils.CONVERSION_SPECIFIERS.matcher("simple %u%").find());
		assertTrue(SPARQLStringUtils.CONVERSION_SPECIFIERS.matcher("multi-line\n%u%").find());
		assertTrue(SPARQLStringUtils.CONVERSION_SPECIFIERS.matcher("simple %s%").find());
		assertTrue(SPARQLStringUtils.CONVERSION_SPECIFIERS.matcher("multi-line\n%s%").find());
		assertTrue(SPARQLStringUtils.CONVERSION_SPECIFIERS.matcher("simple %v%").find());
		assertTrue(SPARQLStringUtils.CONVERSION_SPECIFIERS.matcher("multi-line\n%v%").find());
	}
	
	@Test
	public void testReplaceFirst()
	{
		assertEquals("before replaced after", new String("before %u% after").replaceFirst("%u%", "replaced"));
	}

	@Test
	public void testStrFromTemplateStringStringArray()
	throws IOException
	{
		final String uTemplate = "before %u% after";
		final String sTemplate = "before %s% after";
		final String vTemplate = "before %v% after";
		final String url = "http://example.com";
		final String uResult = "before <http://example.com> after";
		final String sResult = "before \"http://example.com\" after";
		final String vResult = "before http://example.com after";
		assertEquals(uResult, SPARQLStringUtils.strFromTemplate(uTemplate, url));
		assertEquals(sResult, SPARQLStringUtils.strFromTemplate(sTemplate, url));
		assertEquals(vResult, SPARQLStringUtils.strFromTemplate(vTemplate, url));
	}
	
	@Test 
	public void testStrFromTemplate_EscapeChars() throws IOException
	{
		final String sTemplate = "before %s% after";
		final String uTemplate = "before %u% after";
		
		final String str1 = "'string'";
		final String str2 = "\bstring\f";
		final String str3 = "\r\n\t";
		final String str4 = "line1\nline2\n";
		
		final String str1Result = "before \"\\'string\\'\" after";
		final String str2Result = "before \"\\bstring\\f\" after";
		final String str3Result = "before \"\\r\\n\\t\" after";
		final String str4Result = "before \"line1\\nline2\\n\" after";
		
		assertEquals(str1Result, SPARQLStringUtils.strFromTemplate(sTemplate, str1));
		assertEquals(str2Result, SPARQLStringUtils.strFromTemplate(sTemplate, str2));
		assertEquals(str3Result, SPARQLStringUtils.strFromTemplate(sTemplate, str3));
		assertEquals(str4Result, SPARQLStringUtils.strFromTemplate(sTemplate, str4));
		
		final String URI1 = "http://with. aspace .com/";
		final String URI2 = "http://newline\n\n.com";
		final String URI3 = "http://<anglebracket>.org/";
		final String URI4 = "http://uri.containing.escaped.chars.com/%25"; 
		final String URI5 = "_http://not.a.valid.uri/<even>/<after>/<escaping>";
		
		final String URI1Result = "before <http://with.%20aspace%20.com/> after";
		final String URI2Result = "before <http://newline%0A%0A.com> after";
		final String URI3Result = "before <http://%3Canglebracket%3E.org/> after";
		final String URI4Result = "before <http://uri.containing.escaped.chars.com/%25> after"; // want to leave already-escaped chars alone
		
		assertEquals(URI1Result, SPARQLStringUtils.strFromTemplate(uTemplate, URI1));
		assertEquals(URI2Result, SPARQLStringUtils.strFromTemplate(uTemplate, URI2));
		assertEquals(URI3Result, SPARQLStringUtils.strFromTemplate(uTemplate, URI3));
		assertEquals(URI4Result, SPARQLStringUtils.strFromTemplate(uTemplate, URI4));
		
		// Some parts of a URI cannot be fixed by URL-encoding.  In these cases,
		// we should throw an IllegalArgumentException.
		
		boolean threwException = false;
		try {
			SPARQLStringUtils.strFromTemplate(uTemplate, URI5); 
		} catch(IllegalArgumentException e) {
			threwException = true;
		}
		assertTrue(threwException);
		
	}
	
}
