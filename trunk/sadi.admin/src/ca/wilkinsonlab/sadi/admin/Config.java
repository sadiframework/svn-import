package ca.wilkinsonlab.sadi.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * sadi.admin configuration class.  The defaults can be overridden in
 * sadi.properties. 
 * (see {@link ca.wilkinsonlab.sadi.common.Config} for details)
 * 
 * @author Luke McCarthy
 */
public class Config extends ca.wilkinsonlab.sadi.common.Config
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(Config.class);
	
	protected static final String DEFAULT_PROPERTIES_FILENAME = "sadi.admin.properties";
	protected static final String LOCAL_PROPERTIES_FILENAME = "sadi.properties";

	private static final Config theInstance = new Config(DEFAULT_PROPERTIES_FILENAME, LOCAL_PROPERTIES_FILENAME);
	
	public static Config getConfiguration()
	{
		return theInstance;
	}

	private Config(String defaultPropertiesFile, String localPropertiesFile)
	{
		super(defaultPropertiesFile, localPropertiesFile);
	}
}