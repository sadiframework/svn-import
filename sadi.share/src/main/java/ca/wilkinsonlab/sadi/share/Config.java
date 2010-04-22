package ca.wilkinsonlab.sadi.share;

import java.io.IOException;
import java.net.URL;

import net.sf.ehcache.CacheManager;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.stats.PredicateStatsDB;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;

/**
 * SHARE configuration class.  The defaults can be overridden in
 * sadi.properties. 
 * (see {@link ca.wilkinsonlab.sadi.common.Config} for details)
 * 
 * @author Luke McCarthy
 */
public class Config extends ca.wilkinsonlab.sadi.common.Config
{
	private static final Logger log = Logger.getLogger(Config.class);
	
	protected static final String DEFAULT_PROPERTIES_FILENAME = "sadi.share.properties";
	protected static final String LOCAL_PROPERTIES_FILENAME = "sadi.properties";
	protected static final String CACHE_CONFIG_FILENAME = "ehcache.xml";

	private static final Config theInstance = new Config(DEFAULT_PROPERTIES_FILENAME, LOCAL_PROPERTIES_FILENAME);

	private static CacheManager theCacheManager;
	private static PredicateStatsDB theStatsDB;
	
	public static Config getConfiguration()
	{
		return theInstance;
	}

	private Config(String defaultPropertiesFile, String localPropertiesFile)
	{
		super(defaultPropertiesFile, localPropertiesFile);
		
		/*
		 * If CacheManager can't find the specified file, it will search for ehcache.xml in the CLASSPATH,
		 * and if it can't find that, it will print a warning and use ehcache-failsafe.xml (which
		 * is included with the ehcache jar).
		 */
		theCacheManager = new CacheManager(Config.class.getResource(CACHE_CONFIG_FILENAME));		

		initJenaLocationMapper();
		initStatsDB();
	}

	public static CacheManager getCacheManager() 
	{
		return theCacheManager;
	}
	
	/**
	 * Return the singleton stats DB, which stores statistics about predicates
	 * for query optimization. The returned value may be null, if there was
	 * an error initializing the stats DB at startup. Users of the stats DB
	 * should fail gracefully in the case of null, as access to the stats DB is 
	 * not vital to the operation of the query engine. 
	 * 
	 * @return the stats DB, or null if the statsDB was not successfully initialized
	 * at startup
	 */
	public static PredicateStatsDB getStatsDB() 
	{
		return theStatsDB;
	}
	
	/**
	 * Load URL prefix mappings into Jena, for cases when the query engine should not 
	 * retrieve a document from the real URL.
	 */
	private void initJenaLocationMapper() 
	{
		URL mappingFile = Config.class.getResource("jena.url.mapping.n3"); 
		FileManager.get().setLocationMapper(new LocationMapper(mappingFile.toString()));
		
		/* by default, OntDocumentManager does not point to the global FileManager 
		 * (see javadoc for OntDocumentManager) -- BV */
		OntDocumentManager.getInstance().setFileManager(FileManager.get());
	}
	
	protected void initStatsDB()
	{
		try {
			theStatsDB = new PredicateStatsDB(subset(PredicateStatsDB.ROOT_CONFIG_KEY));
		} catch(IOException e) {
			log.error("error initializing predicate stats db: ", e);
		}
	}
	
	@Override
	protected void finalize() throws Throwable 
	{
		try {
			/* not strictly necessary, but best practice */
			if(theCacheManager != null)
				theCacheManager.shutdown();
		} finally {
			super.finalize();
		}
	}
	
}
