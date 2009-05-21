package ca.wilkinsonlab.sadi.service;

import org.apache.commons.configuration.Configuration;

import junit.framework.TestCase;

public class ConfigTest extends TestCase
{
	public void testGetServiceConfiguration() throws Exception
	{
		Configuration serviceConfig = Config.getServiceConfiguration("ca.wilkinsonlab.sadi.service.example.SimpleServiceServlet");
		assertTrue(serviceConfig.containsKey("rdf"));
	}
}
