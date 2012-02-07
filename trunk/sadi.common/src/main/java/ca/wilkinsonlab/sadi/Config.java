package ca.wilkinsonlab.sadi;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

/**
 * Base configuration class.  This class reads default configuration from the
 * sadi.common.properties file (found in the sadi-common distribution), then 
 * overrides it with custom configuration from the sadi.properties file (found
 * in the working directory, the user's home directory or the classpath).
 * 
 * @author Luke McCarthy
 */
public class Config extends CompositeConfiguration
{
	protected static final String DEFAULT_PROPERTIES_FILENAME = "sadi.common.properties";
	protected static final String LOCAL_PROPERTIES_FILENAME = "sadi.properties";
	
	private static final Logger log = Logger.getLogger(Config.class);
	private static final Class<?>[] CONSTRUCTOR_SIGNATURE = new Class<?>[]{ Configuration.class };
	private static final Config theInstance = new Config(DEFAULT_PROPERTIES_FILENAME, LOCAL_PROPERTIES_FILENAME);
	
	/**
	 * Returns the default SADI configuration.
	 * This class is primarily used internally by the sadi-common
	 * distribution; other developers will almost certainly be better
	 * served by ca.wilkinsonlab.sadi.client.Config or
	 * ca.wilkinsonlab.sadi.service.Config.
	 * @return the default SADI configuration
	 */
	public static Config getConfiguration()
	{
		return theInstance;
	}
	
	/**
	 * Constructs a new configuration object from the specified userspace
	 * properties file.
	 * @param localPropertiesFile name of the userspace properties file 
	 */
	public static Config getConfiguration(String localPropertiesFile)
	{
		return new Config(DEFAULT_PROPERTIES_FILENAME, localPropertiesFile);
	}
	
	/**
	 * Constructs a new configuration object from the specified properties
	 * files.
	 * @param defaultPropertiesFile name of the default properties file
	 * @param localPropertiesFile name of the userspace properties file 
	 */
	protected Config(String defaultPropertiesFile, String localPropertiesFile)
	{
		super();
		
		Configuration baseConfig = getBaseConfiguration();
		
		Configuration defaultConfig;
		try {
			defaultConfig = new PropertiesConfiguration(defaultPropertiesFile);
		} catch (ConfigurationException e) {
			log.error(e.getMessage()); // looks like "Cannot locate configuration source %s"
			defaultConfig = new PropertiesConfiguration();
		}
		
		Configuration localConfig;
		try {
			localConfig = new PropertiesConfiguration(localPropertiesFile);
		} catch (ConfigurationException e) {
			log.debug(String.format("no user-space configuration source %s", localPropertiesFile));
			localConfig = new PropertiesConfiguration();
		}
		
		/* order here is important! values in an earlier configuration
		 * override those in a later configuration.
		 */
		this.addConfiguration(localConfig);
		this.addConfiguration(defaultConfig);
		this.addConfiguration(baseConfig);
	}
	
	/**
	 * Returns the base configuration object.
	 * For now this is empty, but in the future we could set defaults here
	 * instead of in the default properties file.
	 * @return
	 */
	protected Configuration getBaseConfiguration()
	{
		return new BaseConfiguration();
	}

	/**
	 * Instantiates a class specified in a configuration.
	 * 
	 * Consider a configuration generated from the following properties:
	 * 
	 *   foo = com.example.SomeClass
	 *   foo.key1 = value1
	 *   foo.key2 = value2
	 * 
	 * <code>Config.instantiate( config.subset("foo") )</code> will return
	 * the result of <code>new com.example.SomeClass( config.subset("foo") )</code>, 
	 * where <code>config.subset("foo")</code> returns { "key1" => "value1", "key2" => "value2" } .
	 * 
	 * @param instanceConfig the configuration to pass to the constructor
	 *                       (usually a subset configuration)
	 * @return the new instance
	 * @throws Exception if there is a problem instantiating the object
	 *                   (one of ClassNotFoundException, NoSuchMethodException,
	 *                    IllegalArgumentException, SecurityException,
	 *                    InstantiationException, IllegalAccessException,
	 *                    InvocationTargetException)
	 */
	protected static Object instantiate(Configuration instanceConfig) throws Exception 
	{
		String className = instanceConfig.getString("");
		Class<?> clazz = Class.forName(className);
		Constructor<?> constructor = clazz.getConstructor(CONSTRUCTOR_SIGNATURE);
		return constructor.newInstance(instanceConfig);
	}
	
	/**
	 * Given a map and a list of keys in order of priority, return a list of
	 * values in corresponding order of priority.
	 * The values of any keys not present in the priority list will be
	 * appended to the list of values.
	 * @param <K> the type of the keys in the map
	 * @param <V> the type of the values in the map
	 * @param map the map
	 * @param priorityList a list of keys in order of priority
	 * @return a list of values in corresponding order of priority
	 */
	protected static <K, V> List<V> buildPriorityList(Map<K, V> map, List<K> priorityList)
	{
		List<V> values = new ArrayList<V>(map.size());
		Map<K, V> mapCopy = new HashMap<K, V>(map);
		if (priorityList != null) {
			for (K key: priorityList) {
				if (mapCopy.containsKey(key)) {
					values.add(mapCopy.get(key));
					mapCopy.remove(key);
				}
			}
		}
		values.addAll(mapCopy.values());
		return values;
	}
}
