package ca.wilkinsonlab.sadi.sparql;

import ca.wilkinsonlab.sadi.sparql.SPARQLEndpointType;

/**
 * 
 * @author Ben Vandervalk
 */
public class SPARQLEndpointFactory 
{
	public static SPARQLService createEndpoint(String endpointURI, SPARQLEndpointType type) 
	{
		switch(type) {
		case VIRTUOSO:
			return new VirtuosoSPARQLEndpoint(endpointURI);
		default:
			throw new IllegalArgumentException("Only Virtuoso SPARQL endpoints are currently supported");
		}
	}
}
