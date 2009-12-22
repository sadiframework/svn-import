package ca.wilkinsonlab.sadi.sparql;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint.EndpointType;

/**
 * 
 * @author Ben Vandervalk
 */
public class SPARQLEndpointFactory 
{
	public final static Logger log = Logger.getLogger(SPARQLEndpoint.class);
	
	public static SPARQLEndpoint createEndpoint(String endpointURI, EndpointType type) 
	{
		switch(type) {
		case VIRTUOSO:
			return new VirtuosoSPARQLEndpoint(endpointURI);
		case D2R:
			return createEndpoint(endpointURI);
		default:
			log.warn(String.format("unrecognized SPARQL endpoint type %s, creating plain SPARQLEndpoint object", type.toString()));
			return createEndpoint(endpointURI);
		}
	}
	
	public static SPARQLEndpoint createEndpoint(String endpointURI) {
		return new SPARQLEndpoint(endpointURI);
	}
}
