package ca.wilkinsonlab.sadi.client.virtual.sparql;

import java.io.IOException;
import java.util.List;

import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint.EndpointType;

public interface SPARQLRegistryAdmin
{
	public abstract String getIndexGraph();
	public abstract void indexEndpoint(String endpointURI, EndpointType type) throws IOException;
	public abstract void indexEndpoint(String endpointURI, EndpointType type, long maxResultsPerQuery) throws IOException;
	public abstract void indexEndpointByTraversal(String endpointURI, EndpointType type, List<String> rootURIs) throws IOException;
	public abstract void removeEndpoint(String endpointURI) throws IOException;
	public abstract void addEndpoint(String endpointURI, EndpointType type) throws IOException;
	public abstract void setEndpointStatus(String endpointURI, ServiceStatus status) throws IOException;
	public abstract void updateStatusOfAllEndpoints() throws IOException;
	public abstract void clearRegistry() throws IOException;
	public abstract void updateEndpointResultsLimit(String arg, EndpointType endpointType) throws IOException;
	public void setEndpointResultsLimit(String endpointURI, long resultsLimit);
}
