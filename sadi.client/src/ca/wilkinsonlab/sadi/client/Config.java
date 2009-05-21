package ca.wilkinsonlab.sadi.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
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
	
	private static final String REGISTRY_SUBSET_KEY = "sadi.registry";
	private static final String REGISTRY_PRIORITY_KEY = "sadi.registryPriority";

	private static List<Registry> registries = configureRegistries();
	private static MultiRegistry masterRegistry = new MultiRegistry(registries);
	
	/**
	 * Return a registry that aggregrates results from all
	 * configured registries. The master registry also handles and logs
	 * exceptions from the configured registries.
	 * @return the aggregated Registry object
	 */
	public static MultiRegistry getMasterRegistry()
	{
		return masterRegistry;
	}

	/**
	 * Return a list of configured registries.
	 * @return the configured registries
	 */
	public static List<Registry> getRegistries()
	{
		return registries;
	}
	
	private static List<Registry> configureRegistries()
	{
		Map<String, Registry> registries = new HashMap<String, Registry>();
		Configuration registryConfig = getConfiguration().subset(REGISTRY_SUBSET_KEY);
		for (Iterator registryKeys = registryConfig.getKeys(); registryKeys.hasNext(); ) {
			String registryKey = (String)registryKeys.next();
			if (registryKey.contains("."))
				continue; // only interested in the root property
			try {
				registries.put(registryKey, (Registry)instantiate(registryConfig.subset(registryKey)));
			} catch (Exception e) {
				log.error(String.format("Error configuring registry %s", registryKey), e);
			}
		}
		
		return buildPriorityList(registries, getConfiguration().getString(REGISTRY_PRIORITY_KEY));
	}
	
//	private static List<Resolver> configureResolvers()
//	{
//		Map<String, Resolver> resolvers = new HashMap<String, Resolver>();
//		Configuration resolverConfig = getConfiguration().subset(RESOLVER_SUBSET_KEY);
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
