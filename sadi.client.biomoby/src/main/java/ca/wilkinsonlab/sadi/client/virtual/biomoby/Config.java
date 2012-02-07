package ca.wilkinsonlab.sadi.client.virtual.biomoby;

public class Config extends ca.wilkinsonlab.sadi.client.Config
{	
	public static final String DEFAULT_PROPERTIES_FILENAME = "sadi.client.biomoby.properties";
	
	private Config(String defaultPropertiesFile, String localPropertiesFile)
	{
		super(defaultPropertiesFile, localPropertiesFile);
	}
}
