package ca.wilkinsonlab.sadi.service.example;

import java.net.MalformedURLException;
import java.net.URL;

import ca.wilkinsonlab.sadi.rdf.RdfService;

public class LocalServiceWrapper extends RdfService
{
	private URL localServiceUrl;
	
	public LocalServiceWrapper(String serviceUri, String localServiceUrl) throws MalformedURLException
	{
		super(serviceUri);
		
		this.localServiceUrl = new URL(localServiceUrl);
	}
	
	@Override
	public URL getServiceURL()
	{
		return localServiceUrl;
	}
	
	@Override
	public String getServiceURI()
	{
		return localServiceUrl.toExternalForm();
	}
}
