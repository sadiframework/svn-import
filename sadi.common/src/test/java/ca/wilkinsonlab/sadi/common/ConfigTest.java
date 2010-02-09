package ca.wilkinsonlab.sadi.common;

import static org.junit.Assert.*;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigTest
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
	public void testGetConfiguration()
	{
		Configuration config = Config.getConfiguration();
		assertTrue("Client.Config missing global key", config.containsKey("sadi.decompose.undefinedPropertiesPolicy"));
		assertTrue("Client.Config missing local key", config.containsKey("sadi.client.localKey"));
	}
}