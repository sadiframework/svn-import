package org.sadiframework.service;

import org.apache.commons.configuration.Configuration;
import org.sadiframework.service.Config;

import junit.framework.TestCase;

public class ConfigTest extends TestCase
{
	public void testGetServiceConfiguration() throws Exception
	{
		Configuration serviceConfig = Config.getConfiguration().getServiceConfiguration("org.sadiframework.service.example.SimpleServiceServlet");
		assertTrue(serviceConfig.containsKey("rdf"));
	}
}
