package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.rmi.AccessException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;

import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;

public interface SPARQLRegistryAdmin 
{
	public abstract String getURI();
	public abstract String getIndexGraphURI();
	public abstract void clearRegistry() throws HttpException, HttpResponseCodeException, IOException, AccessException;
	public abstract void addAndIndexEndpoint(String URI, SPARQLEndpointType type) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException;
	public abstract void removeEndpoint(String URI) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException;
}
