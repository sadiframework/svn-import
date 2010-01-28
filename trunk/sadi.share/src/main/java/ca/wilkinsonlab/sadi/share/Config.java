package ca.wilkinsonlab.sadi.share;

import java.net.URL;

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

	private static final Config theInstance = new Config(DEFAULT_PROPERTIES_FILENAME, LOCAL_PROPERTIES_FILENAME);
	
	public static Config getConfiguration()
	{
		return theInstance;
	}

	private Config(String defaultPropertiesFile, String localPropertiesFile)
	{
		super(defaultPropertiesFile, localPropertiesFile);
		initJenaLocationMapper();
	}
	
	/**
	 * Load URL prefix mappings into Jena, for cases when the query engine should not 
	 * retrieve a document from the real URL.
	 */
	private void initJenaLocationMapper() {
		URL mappingFile = Config.class.getResource("jena.url.mapping.n3"); 
		FileManager.get().setLocationMapper(new LocationMapper(mappingFile.toString()));
		/* by default, OntDocumentManager does not point to the global FileManager 
		 * (see javadoc for OntDocumentManager) -- BV */
		OntDocumentManager.getInstance().setFileManager(FileManager.get());
	}
}
