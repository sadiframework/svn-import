package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ca.wilkinsonlab.sadi.utils.StringUtil;

public class StringUtilTest
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
		assertTrue(StringUtil.CONVERSION_SPECIFIERS.matcher("simple %u%").find());
		assertTrue(StringUtil.CONVERSION_SPECIFIERS.matcher("multi-line\n%u%").find());
		assertTrue(StringUtil.CONVERSION_SPECIFIERS.matcher("simple %s%").find());
		assertTrue(StringUtil.CONVERSION_SPECIFIERS.matcher("multi-line\n%s%").find());
		assertTrue(StringUtil.CONVERSION_SPECIFIERS.matcher("simple %v%").find());
		assertTrue(StringUtil.CONVERSION_SPECIFIERS.matcher("multi-line\n%v%").find());
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
		assertEquals(uResult, StringUtil.strFromTemplate(uTemplate, url));
		assertEquals(sResult, StringUtil.strFromTemplate(sTemplate, url));
		assertEquals(vResult, StringUtil.strFromTemplate(vTemplate, url));
	}

//	@Test
//	public void testStrFromTemplateURLStringArray()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testEscapeString()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testEscapeURI()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testReadFully()
//	{
//		fail("Not yet implemented");
//	}
}
