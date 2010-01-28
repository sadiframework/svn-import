package ca.wilkinsonlab.sadi.client;

import static org.junit.Assert.assertFalse;

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
	public void testGetRegistries()
	{
		assertFalse("Client.config contains no registry entries", Config.getRegistries().isEmpty());
	}
}
