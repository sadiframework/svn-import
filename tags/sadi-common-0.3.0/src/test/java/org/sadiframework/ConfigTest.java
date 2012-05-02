package org.sadiframework;

import static org.junit.Assert.assertTrue;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sadiframework.Config;

public class ConfigTest
{
	private static Logger log = Logger.getLogger(ConfigTest.class);
	
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
	
	@Test
	public void testGetDefaultReasoner()
	{
		log.info(String.format("default reasoner spec is %s", Config.getConfiguration().getString("sadi.defaultReasoner")));
	}
}