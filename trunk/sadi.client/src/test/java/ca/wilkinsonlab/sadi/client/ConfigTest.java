package ca.wilkinsonlab.sadi.client;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigTest
{
	private static Config config;
	
	@BeforeClass
	public static void setUp() throws Exception
	{
		config = Config.getConfiguration();
	}

	@AfterClass
	public static void tearDown() throws Exception
	{
		config = null;
	}

	@Test
	public void testGetRegistries()
	{
		List<Registry> registries = config.getRegistries();
		assertFalse("Client.config contains no registry entries", registries.isEmpty());
	}
}
