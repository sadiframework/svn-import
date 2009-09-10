package ca.wilkinsonlab.sadi.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Client configuration class, containing information about which service
 * registries are available.  The defaults can be overridden in
 * local.properties. 
 * (see {@link ca.wilkinsonlab.sadi.common.Config} for details)
 * 
 * Most users' interaction with this class will be limited to the
 * {@link getMasterRegistry()} method.
 * 
 * @author Luke McCarthy
 */
public class Config extends ca.wilkinsonlab.sadi.common.Config
{
	private static final Log log = LogFactory.getLog(Config.class);
	
	protected static final String DEFAULT_PROPERTIES_FILENAME = "sadi.client.properties";
	protected static final String LOCAL_PROPERTIES_FILENAME = "sadi.properties";
	
	private static final String REGISTRY_SUBSET_KEY = "sadi.registry";
	private static final String REGISTRY_PRIORITY_KEY = "sadi.registryPriority";

	private static final Config theInstance = new Config(DEFAULT_PROPERTIES_FILENAME, LOCAL_PROPERTIES_FILENAME);
	
	public static Config getConfiguration()
	{
		return theInstance;
	}
	
	/**
	 * Return a registry that aggregrates results from all
	 * configured registries. The master registry also handles and logs
	 * exceptions from the configured registries.
	 * @return the aggregated Registry object
	 */
	public static MultiRegistry getMasterRegistry()
	{
		return getConfiguration().masterRegistry;
	}

	/**
	 * Return a list of configured registries.
	 * @return the configured registries
	 */
	public static List<Registry> getRegistries()
	{
		return getConfiguration().registries;
	}

	private List<Registry> registries;
	private MultiRegistry masterRegistry;

	private Config(String defaultPropertiesFile, String localPropertiesFile)
	{
		super(defaultPropertiesFile, localPropertiesFile);
		
		registries = configureRegistries();
		masterRegistry = new MultiRegistry(registries);
	}
	
	private List<Registry> configureRegistries()
	{
		StopWatch stopWatch = new StopWatch();
		Map<String, Registry> registries = new HashMap<String, Registry>();
		Configuration registryConfig = subset(REGISTRY_SUBSET_KEY);
		for (Iterator<?> registryKeys = registryConfig.getKeys(); registryKeys.hasNext(); ) {
			String registryKey = (String)registryKeys.next();
			if (registryKey.contains("."))
				continue; // only interested in the root property
			try {
				stopWatch.start();
				registries.put(registryKey, (Registry)instantiate(registryConfig.subset(registryKey)));
			} catch (Exception e) {
				log.error(String.format("Error configuring registry %s", registryKey), e);
			} finally {
				stopWatch.stop();
				log.info(String.format("instantiated registry %s in %dms", registryKey, stopWatch.getTime()));
				stopWatch.reset();
			}
		}
		
		return buildPriorityList(registries, getString(REGISTRY_PRIORITY_KEY));
	}
	
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
