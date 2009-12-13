package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.rmi.AccessException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;

import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint.EndpointType;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;

public interface SPARQLRegistryAdmin
{
	public abstract String getURI();
	public abstract String getIndexGraphURI();
	public abstract void clearRegistry() throws HttpException, HttpResponseCodeException, IOException, AccessException;
	public abstract void indexEndpoint(String URI, EndpointType type, long maxResultsPerQuery) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException;
	public abstract void removeEndpoint(String URI) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException;
	public void addEndpoint(String endpointURI, EndpointType type) throws IOException;
	//public void updateRegex(String endpointURI, EndpointType type, boolean positionIsSubject) throws IOException;
	public void indexEndpoint(String endpointURI, EndpointType type) throws IOException;
	public void removeAmbiguousPropertiesFromOntology() throws IOException;
	public void setEndpointStatus(String endpointURI, ServiceStatus status) throws IOException;
	public void addPredicatesBySubjectURI(String endpointURI, EndpointType type, String subjectURI) throws IOException; 
}
