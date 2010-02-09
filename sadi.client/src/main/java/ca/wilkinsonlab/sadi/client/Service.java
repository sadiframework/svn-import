package ca.wilkinsonlab.sadi.client;

import java.util.Collection;

import ca.wilkinsonlab.sadi.common.SADIException;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * The main Service interface.
 * @author Luke McCarthy
 */
public interface Service
{
	public enum ServiceStatus { OK,	SLOW, DEAD };

	/**
	 * Returns the URI of the service.
	 * @return the URI of the service
	 */
	public abstract String getURI();
	
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
	 * Returns the URI of an OWL class describing the input this service
	 * can consume. This URI should resolve to a definition of the class.
	 * @return the URI of the service's input class
	 */
	public abstract String getInputClassURI();
	
	/**
	 * Returns an OntClass describing the input this service can consume.
	 * Any input to this service must be an instance of this class and must
	 * be explicitly typed as such. This operation causes the ontology
	 * defining the input class to be loaded along with any imported
	 * ontologies.
	 * @return an OntClass describing the input this service can consume
	 * @throws SADIException if there is a problem with the class definition
	 */
	public abstract OntClass getInputClass() throws SADIException;
	
	/**
	 * Returns the URI of an OWL class describing the output this service
	 * produces. This URI should resolve to a definition of the class.
	 * @return the URI of the service's output class
	 */
	public abstract String getOutputClassURI();
	
	/**
	 * Returns an OntClass describing the output this service produces.
	 * Any output from this service will be an instance of this class. This
	 * operation causes the ontology defining the input class to be loaded
	 * along with any imported ontologies.
	 * @return an OntClass describing the output this service produces
	 * @throws SADIException if there is a problem with the class definition
	 */
	public abstract OntClass getOutputClass() throws SADIException;
	
	/**
	 * Calls the service with the specified input.
	 * @param inputNode the input
	 * @return a collection of triples output from the service
	 * @throws Exception
	 */
	public abstract Collection<Triple> invokeService(Resource inputNode) throws ServiceInvocationException;
	
	/**
	 * Calls the service with the specified inputs.
	 * @param inputNode the input
	 * @return a collection of triples output from the service
	 * @throws Exception
	 */
	public abstract Collection<Triple> invokeService(Collection<Resource> inputNodes) throws ServiceInvocationException;
	
	/**
	 * Calls the service with the specified input and filters the output so
	 * that only triples with the specified predicate are returned.
	 * @param inputNode the input
	 * @param predicate the desired predicate
	 * @return a filtered collection of triples output from the service
	 * @throws Exception
	 */
	public abstract Collection<Triple> invokeService(Resource inputNode, String predicate) throws ServiceInvocationException;
	
	/**
	 * Calls the service with the specified inputs and filters the output so
	 * that only triples with the specified predicate are returned.
	 * @param inputNode the input
	 * @param predicate the desired predicate
	 * @return a filtered collection of triples output from the service
	 * @throws Exception
	 */
	public abstract Collection<Triple> invokeService(Collection<Resource> inputNodes, String predicate) throws ServiceInvocationException;
	
	/**
	 * Returns true if the specified Resource is an instance of this service's
	 * input class, false otherwise.
	 * @param resource an input node
	 * @return true if the specified Resource is an instance of this service's input class,
	 *         false otherwise.
	 */
	public abstract boolean isInputInstance(Resource resource);
	
	/**
	 * Returns a collection of Resources in the specified model that are
	 * instances of this service's input class
	 * @param inputModel the input data
	 * @return all instances of this service's input class in the model
	 */
	public abstract Collection<Resource> discoverInputInstances(Model inputModel);
}
