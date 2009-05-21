package ca.wilkinsonlab.sadi.service;

import java.util.Iterator;

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
	
	private static final String SERVICE_SUBSET_KEY = "sadi.service";
	
	public static Configuration getServiceConfiguration(ServiceServlet servlet)
	throws ConfigurationException
	{
		return getServiceConfiguration(servlet.getClass().getName());
	}
	
	public static Configuration getServiceConfiguration(String servletClassName)
	throws ConfigurationException
	{
		/* TODO cache this lookup?
		 */
		Configuration servicesConfig = getConfiguration().subset(SERVICE_SUBSET_KEY);
		for (Iterator serviceKeys = servicesConfig.getKeys(); serviceKeys.hasNext(); ) {
			String serviceKey = (String)serviceKeys.next();
			if (serviceKey.contains("."))
				continue; // only interested in the root property
			if (servletClassName.equals(servicesConfig.getString(serviceKey)))
				return servicesConfig.subset(serviceKey);
		}
		throw new ConfigurationException("Unmapped servlet class " + servletClassName);
	}
}
