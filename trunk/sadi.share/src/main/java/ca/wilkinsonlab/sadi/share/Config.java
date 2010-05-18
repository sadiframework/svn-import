package ca.wilkinsonlab.sadi.share;

import java.net.URL;

import net.sf.ehcache.CacheManager;

import org.apache.log4j.Logger;

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
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Config.class);
	
	protected static final String DEFAULT_PROPERTIES_FILENAME = "sadi.share.properties";
	protected static final String LOCAL_PROPERTIES_FILENAME = "sadi.properties";
	protected static final String CACHE_CONFIG_FILENAME = "ehcache.xml";

	private static final Config theInstance = new Config(DEFAULT_PROPERTIES_FILENAME, LOCAL_PROPERTIES_FILENAME);

	private static CacheManager theCacheManager;
	
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
	}

	public static CacheManager getCacheManager() 
	{
		return theCacheManager;
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
