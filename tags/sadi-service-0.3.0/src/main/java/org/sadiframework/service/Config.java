package org.sadiframework.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

/**
 * This class provides access to the common service configuration and any
 * service-specific configuration.
 * @author Luke McCarthy
 */
public class Config extends org.sadiframework.Config
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Config.class);
	
	protected static final String DEFAULT_PROPERTIES_FILENAME = "sadi.service.properties";
	protected static final String LOCAL_PROPERTIES_FILENAME = "sadi.properties";
	
	public static final String SERVICE_SUBSET_KEY = "sadi.service";
	
	private static final Config theInstance = new Config(DEFAULT_PROPERTIES_FILENAME, LOCAL_PROPERTIES_FILENAME);
	
	/**
	 * Returns the default SADI configuration.
	 * @return the default SADI configuration
	 */
	public static Config getConfiguration()
	{
		return theInstance;
	}
	
	/**
	 * Returns the configuration mapped to the specified ServiceServlet.
	 * @param servlet the ServiceServlet
	 * @return the configuration mapped to the specified ServiceServlet
	 */
	public Configuration getServiceConfiguration(ServiceServlet servlet)
	{
		return getServiceConfiguration(servlet.getClass().getName());
	}
	
	/**
	 * Returns the configuration mapped to the specified service servlet
	 * class name.
	 * @param servletClassName the service servlet class name
	 * @return the configuration mapped to the specified service servlet class name
	 */
	public Configuration getServiceConfiguration(String servletClassName)
	{
		for (Configuration config: getServiceConfigurations().values())
			if (servletClassName.equals(config.getString("")))
				return config;
		
		return null;
	}
	
	/**
	 * Returns a map of service URI to service configuration.
	 * @return a map of service URI to service configuration
	 */
	public Map<String, Configuration> getServiceConfigurations()
	{
		/* TODO cache this lookup?
		 */
		Configuration servicesConfig = subset(SERVICE_SUBSET_KEY);
		Map<String, Configuration> configs = new HashMap<String, Configuration>();
		for (Iterator<?> serviceKeys = servicesConfig.getKeys(); serviceKeys.hasNext(); ) {
			String serviceKey = (String)serviceKeys.next();
			if (serviceKey.contains("."))
				continue; // only interested in the root property
			configs.put(serviceKey, servicesConfig.subset(serviceKey));
		}
		return configs;
	}
	
	private Config(String defaultPropertiesFile, String localPropertiesFile)
	{
		super(defaultPropertiesFile, localPropertiesFile);
	}
	
	/**
	 * Constructs a new configuration from the specified local properties file.
	 * TODO this is only used by the service generator; can we strip it out
	 *      and put it in that codebase?
	 * @param localPropertiesFile
	 */
	public Config(String localPropertiesFile)
	{
		this(DEFAULT_PROPERTIES_FILENAME, localPropertiesFile);
	}
}
