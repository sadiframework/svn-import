package org.sadiframework.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sadiframework.utils.JsonUtils;

public class JsonUtilsTest
{
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
	}

	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testReadWrite()
	{
		Map<String, String> before = new HashMap<String, String>();
		before.put("foo", "FOO");
		before.put("bar", "BAR");
		String s = JsonUtils.write(before);
		assertFalse("JSON string is empty", s.isEmpty());
		Map<?, ?> after = (Map<?, ?>)JsonUtils.read(s);
		assertEquals("decoded object not equal to encoded object", before, after);
	}
}
