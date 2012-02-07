package ca.wilkinsonlab.sadi.client.virtual.sparql;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint.EndpointType;

/**
 * 
 * @author Ben Vandervalk
 */
public class SPARQLEndpointFactory 
{
	public final static Logger log = Logger.getLogger(SPARQLEndpoint.class);
	
	public static SPARQLEndpoint createEndpoint(String endpointURI, EndpointType type) throws MalformedURLException
	{
		switch(type) {
		case VIRTUOSO:
			return new VirtuosoSPARQLEndpoint(endpointURI);
		case D2R:
		default:
			return new SPARQLEndpoint(endpointURI, type);
		}
	}
	
	public static SPARQLEndpoint createEndpoint(String endpointURI) {
		return new SPARQLEndpoint(endpointURI);
	}
}
