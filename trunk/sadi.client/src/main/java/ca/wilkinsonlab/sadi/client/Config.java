package ca.wilkinsonlab.sadi.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.biomoby.BioMobyRegistry;
import ca.wilkinsonlab.sadi.sparql.SPARQLRegistry;

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
	private static final Logger log = Logger.getLogger(Config.class);
	
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
	
	/** 
	 * Return the SADI SPARQL registry.  For now, we assume that there is exactly
	 * one such registry. In the long run, this method should no longer be
	 * needed, as all SADI registries should be treated uniformly.
	 * 
	 * @return the SADI SPARQL registry
	 */
	public static SPARQLRegistry getSPARQLRegistry() 
	{
		for(Registry r: getRegistries()) {
			if(r instanceof SPARQLRegistry)
				return (SPARQLRegistry)r;
		}
		return null;
	}
	
	/** 
	 * Return the SADI BioMoby registry.  For now, we assume that there is exactly
	 * one such registry. In the long run, this method should no longer be
	 * needed, as all SADI registries should be treated uniformly.
	 * 
	 * @return the SADI BioMoby registry
	 */
	public static BioMobyRegistry getMobyRegistry()
	{
		for (Registry reg: Config.getRegistries())
			if (reg instanceof BioMobyRegistry)
				return (BioMobyRegistry)reg;
		return null;
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
