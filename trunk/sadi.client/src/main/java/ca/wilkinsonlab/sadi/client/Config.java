package ca.wilkinsonlab.sadi.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.reflections.Reflections;

/**
 * Client configuration class, containing information about which service
 * registries are available.  The defaults can be overridden in
 * sadi.properties. 
 * (see {@link ca.wilkinsonlab.sadi.common.Config} for details)
 * 
 * Most users' interaction with this class will be limited to the
 * {@link getMasterRegistry()} method.
 * 
 * @author Luke McCarthy
 */
public class Config extends ca.wilkinsonlab.sadi.Config
{
	private static final Logger log = Logger.getLogger(Config.class);
	
	protected static final String DEFAULT_PROPERTIES_FILENAME = "sadi.client.properties";
	protected static final String LOCAL_PROPERTIES_FILENAME = "sadi.properties";
	
	private static final String REGISTRY_SUBSET_KEY = "sadi.registry";
	private static final String REGISTRY_PRIORITY_KEY = "sadi.registryPriority";
	private static final String REGISTRY_EXCLUDE_KEY = "exclude";

	private static final Config theInstance = new Config(DEFAULT_PROPERTIES_FILENAME, LOCAL_PROPERTIES_FILENAME);
	
	/**
	 * Returns the default SADI client configuration.
	 * @return the default SADI client configuration
	 */
	public static Config getConfiguration()
	{
		return theInstance;
	}

	private Map<String, Registry> registries;
	private List<Registry> priorityList;
	private MultiRegistry masterRegistry;

	/**
	 * Constructs a new configuration object from the specified properties
	 * files.
	 * @param defaultPropertiesFile name of the default properties file
	 * @param localPropertiesFile name of the userspace properties file 
	 */
	protected Config(String defaultPropertiesFile, String localPropertiesFile)
	{
		super(defaultPropertiesFile, localPropertiesFile);
		
		/* scan the classpath for subclasses of this Config object and read
		 * the properties files they reference; if you can come up with a
		 * better way of automatically reading properties files included with
		 * unknown-at-compile-time Maven artifacts, please change this...
		 */
		Reflections reflections = new Reflections("ca.wilkinsonlab.sadi.client");
		for (Class<? extends Config> c: reflections.getSubTypesOf(Config.class)) {
			try {
				String props = (String)c.getField("DEFAULT_PROPERTIES_FILENAME").get(null);
				addConfiguration(new PropertiesConfiguration(props));
			} catch (SecurityException e) {
				log.error(String.format("%s.DEFAULT_PROPERTIES_FILENAME is not accessible", c));
			} catch (NoSuchFieldException e) {
				log.error(String.format("%s.DEFAULT_PROPERTIES_FILENAME does not exist", c));
			} catch (IllegalAccessException e) {
				log.error(String.format("%s.DEFAULT_PROPERTIES_FILENAME is not accessible", c));
			} catch (ConfigurationException e) {
				log.error(e.getMessage()); // looks like "Cannot locate configuration source %s"
			} catch (IllegalArgumentException e) {
				// this shouldn't happen...
				log.error(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Return a registry that aggregrates results from all
	 * configured registries. The master registry also handles and logs
	 * exceptions from the configured registries.
	 * @return the aggregated Registry object
	 */
	public MultiRegistry getMasterRegistry()
	{
		synchronized(this) {
			if (masterRegistry == null) {
				masterRegistry = new MultiRegistry(priorityList);
			}
			return masterRegistry;
		}
	}

	/**
	 * Return a list of configured registries.
	 * @return the configured registries
	 */
	@SuppressWarnings("unchecked")
	public List<Registry> getRegistries()
	{
		synchronized(this) {
			if (priorityList == null) {
				priorityList = buildPriorityList(getRegistryMap(), getList(REGISTRY_PRIORITY_KEY));
			}
			return priorityList;
		}
	}
	
	/**
	 * Returns a specific registry by name.
	 * @param key the configuration key associated with the registry
	 * @return the registry associated with the specified name
	 */
	public Registry getRegistry(String key)
	{
		return getRegistryMap().get(key);
	}
	
	/**
	 * Returns a map of name to registry.
	 * @return a map of name to registry
	 */
	public Map<String, Registry> getRegistryMap()
	{
		synchronized(this) {
			if (registries == null) {
				registries = configureRegistries();
			}
			return registries;
		}
	}
	
	private Map<String, Registry> configureRegistries()
	{
		StopWatch stopWatch = new StopWatch();
		registries = new HashMap<String, Registry>();
		Configuration registryConfig = subset(REGISTRY_SUBSET_KEY);
		for (Iterator<?> registryKeys = registryConfig.getKeys(); registryKeys.hasNext(); ) {
			String registryKey = (String)registryKeys.next();
			if (registryKey.contains("."))
				continue; // only interested in the root property
			Configuration registrySubset = registryConfig.subset(registryKey);
			try {
				if (registrySubset.getBoolean(REGISTRY_EXCLUDE_KEY, false)) {
					if (log.isDebugEnabled())
						log.debug(String.format("excluding registry %s", registryKey));
					continue;
				}
			} catch (ConversionException e) {
				// TODO better error message...
				log.error(String.format("error configuring registry %s", registryKey), e);
			}
			try {
				stopWatch.start();
				registries.put(registryKey, (Registry)instantiate(registrySubset));
			} catch (Exception e) {
				log.error(String.format("error configuring registry %s", registryKey), e);
			} finally {
				stopWatch.stop();
				if (log.isDebugEnabled())
					log.debug(String.format("instantiated registry %s in %dms", registryKey, stopWatch.getTime()));
				stopWatch.reset();
			}
		}
		return registries;
	}
	
//	/** 
//	 * Return the SADI SPARQL registry.  For now, we assume that there is exactly
//	 * one such registry. In the long run, this method should no longer be
//	 * needed, as all SADI registries should be treated uniformly.
//	 * 
//	 * @return the SADI SPARQL registry
//	 */
//	public static SPARQLRegistry getSPARQLRegistry() 
//	{
//		for(Registry r: getRegistries()) {
//			if(r instanceof SPARQLRegistry)
//				return (SPARQLRegistry)r;
//		}
//		return null;
//	}
//	
//	/** 
//	 * Return the SADI BioMoby registry.  For now, we assume that there is exactly
//	 * one such registry. In the long run, this method should no longer be
//	 * needed, as all SADI registries should be treated uniformly.
//	 * 
//	 * @return the SADI BioMoby registry
//	 */
//	public static BioMobyRegistry getMobyRegistry()
//	{
//		for (Registry reg: Config.getRegistries())
//			if (reg instanceof BioMobyRegistry)
//				return (BioMobyRegistry)reg;
//		return null;
//	}
	
//	private List<Resolver> configureResolvers()
//	{
//		Map<String, Resolver> resolvers = new HashMap<String, Resolver>();
//		Configuration resolverConfig = subset(RESOLVER_SUBSET_KEY);
//		for (Iterator resolverKeys = resolverConfig.getKeys(); resolverKeys.hasNext(); ) {
//			String resolverKey = (String)resolverKeys.next();
//			if (resolverKey.contains("."))
//				continue; // only interested in the root property
//			try {
//				Resolver resolver = (Resolver)instantiate(resolverConfig.subset(resolverKey));
//				resolvers.put(resolverKey, resolver);
//			} catch (Exception e) {
//				log.error(String.format("Error configuring resolver %s", resolverKey), e);
//			}
//		}
//		
//		return buildPriorityList(resolvers, getConfiguration().getString(RESOLVER_PRIORITY_KEY));
//	}
}
