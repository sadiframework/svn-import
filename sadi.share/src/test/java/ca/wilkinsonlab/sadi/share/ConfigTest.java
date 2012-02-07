package ca.wilkinsonlab.sadi.share;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ConfigTest
{
	@Test
	public void testRegistryConfiguration()
	{
		ca.wilkinsonlab.sadi.client.Config config = ca.wilkinsonlab.sadi.client.Config.getConfiguration();
		assertNotNull("missing main SADI registry", config.getRegistry("sadi"));
		assertNotNull("missing SPARQL endpoint registry", config.getRegistry("sparql"));
		assertNull("BioMoby registry shouldn't be visible here", config.getRegistry("biomoby"));
	}
}
