package ca.wilkinsonlab.sadi.utils;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
	
}
