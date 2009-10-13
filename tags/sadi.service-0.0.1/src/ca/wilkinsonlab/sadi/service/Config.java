package ca.wilkinsonlab.sadi.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides access to the common service configuration and any
 * service-specific configuration.
 * @author Luke McCarthy
 */
public class Config extends ca.wilkinsonlab.sadi.common.Config
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(Config.class);
	
	protected static final String DEFAULT_PROPERTIES_FILENAME = "sadi.service.properties";
	protected static final String LOCAL_PROPERTIES_FILENAME = "sadi.properties";
	
	public static final String SERVICE_SUBSET_KEY = "sadi.service";
	
	private static final Config theInstance = new Config(DEFAULT_PROPERTIES_FILENAME, LOCAL_PROPERTIES_FILENAME);
	
	/**
	 * 
	 * @return
	 */
	public static Config getConfiguration()
	{
		return theInstance;
	}
	
	/**
	 * 
	 * @param localPropertiesFile
	 * @return
	 */
	public static Config getConfiguration(String localPropertiesFile)
	{
		return new Config(DEFAULT_PROPERTIES_FILENAME, localPropertiesFile);
	}
	
	/**
	 * 
	 * @param servlet
	 * @return
	 * @throws ConfigurationException
	 */
	public Configuration getServiceConfiguration(ServiceServlet servlet)
	throws ConfigurationException
	{
		return getServiceConfiguration(servlet.getClass().getName());
	}
	
	/**
	 * 
	 * @param servletClassName
	 * @return
	 * @throws ConfigurationException
	 */
	public Configuration getServiceConfiguration(String servletClassName)
	throws ConfigurationException
	{
		for (Configuration config: getServiceConfigurations().values())
			if (servletClassName.equals(config.getString("")))
				return config;
		
		throw new ConfigurationException("Unmapped servlet class " + servletClassName);
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
}
