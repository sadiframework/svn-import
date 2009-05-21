package ca.wilkinsonlab.sadi.client;

import java.util.Collection;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * The main Service interface.
 * @author Luke McCarthy
 */
public interface Service
{
	/**
	 * Returns the URI of the service.
	 * @return the URI of the service
	 */
	public abstract String getServiceURI();
	
	/**
	 * Returns the name of the service.
	 * @return the name of the service
	 */
	public abstract String getName();
	
	/**
	 * Returns a detailed description of the service.
	 * @return a detailed description of the service
	 */
	public abstract String getDescription();
	
	/**
	 * Calls the service with the specified input.
	 * @param inputNode the input
	 * @return a collection of triples output from the service
	 * @throws Exception
	 */
	public abstract Collection<Triple> invokeService(Resource inputNode) throws Exception;
	
	/**
	 * Calls the service with the specified input and filters the output so
	 * that only triples with the specified predicate are returned.
	 * @param inputNode the input
	 * @param predicate the desired predicate
	 * @return a filtered collection of triples output from the service
	 * @throws Exception
	 */
	public abstract Collection<Triple> invokeService(Resource inputNode, String predicate) throws Exception;
}
