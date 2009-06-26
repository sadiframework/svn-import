package ca.wilkinsonlab.sadi.common;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base configuration class.  This class reads default configuration from the
 * default.properties file, then overrides it with custom configuration from
 * a file  called local.properties in the working directory, the user's home
 * directory or the classpath.
 * @author Luke McCarthy
 */
public class Config extends CompositeConfiguration
{
	private static final Log log = LogFactory.getLog(Config.class);

	protected static final String DEFAULT_PROPERTIES_FILENAME = "sadi.common.properties";
	protected static final String LOCAL_PROPERTIES_FILENAME = "sadi.properties";
	private static final Class[] CONSTRUCTOR_SIGNATURE = new Class[]{ Configuration.class };

	private static final Config theInstance = new Config(DEFAULT_PROPERTIES_FILENAME, LOCAL_PROPERTIES_FILENAME);
	
	public static Config getConfiguration()
	{
		return theInstance;
	}
	
	protected Config(String defaultPropertiesFile, String localPropertiesFile)
	{
		super();
		
		Configuration baseConfig = getBaseConfiguration();
		
		Configuration defaultConfig;
		try {
			defaultConfig = new PropertiesConfiguration(defaultPropertiesFile);
		} catch (ConfigurationException e) {
			log.warn(String.format("error loading %s", defaultPropertiesFile), e);
			defaultConfig = new PropertiesConfiguration();
		}
		
		Configuration localConfig;
		try {
			localConfig = new PropertiesConfiguration(localPropertiesFile);
		} catch (ConfigurationException e) {
			log.warn(String.format("error loading %s", localPropertiesFile), e);
			localConfig = new PropertiesConfiguration();
		}
		
		/* order here is important! values in an earlier configuration
		 * override those in a later configuration.
		 */
		this.addConfiguration(localConfig);
		this.addConfiguration(defaultConfig);
		this.addConfiguration(baseConfig);
	}
	
	protected Configuration getBaseConfiguration()
	{
		return new BaseConfiguration();
	}

	protected static Object instantiate(Configuration instanceConfig) throws Exception
	{
		String className = instanceConfig.getString("");
		Class clazz = Class.forName(className);
		Constructor constructor = clazz.getConstructor(CONSTRUCTOR_SIGNATURE);
		return constructor.newInstance(instanceConfig);
	}
	
	protected static <K, V> List<V> buildPriorityList(Map<K, V> map, String priorityList)
	{
		List<V> values = new ArrayList<V>(map.size());
		if (priorityList != null) {
			for (String key: priorityList.split(",\\s*")) {
				values.add(map.get(key));
				map.remove(key);
			}
		}
		values.addAll(map.values());
		return values;
	}
}
