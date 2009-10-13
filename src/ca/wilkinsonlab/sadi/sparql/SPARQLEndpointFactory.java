package ca.wilkinsonlab.sadi.sparql;

import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint.EndpointType;

/**
 * 
 * @author Ben Vandervalk
 */
public class SPARQLEndpointFactory 
{
	public static SPARQLEndpoint createEndpoint(String endpointURI, EndpointType type) 
	{
		switch(type) {
		case VIRTUOSO:
			return new VirtuosoSPARQLEndpoint(endpointURI);
		case D2R:
			return new SPARQLEndpoint(endpointURI);
		default:
			throw new IllegalArgumentException("Unsupported SPARQL endpoint type: " + type.toString());
		}
	}
}
