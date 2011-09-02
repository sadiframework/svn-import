package ca.wilkinsonlab.sadi.client;

import java.util.Collection;
import java.util.Iterator;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.ServiceDescription;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * The main Service interface.
 * @author Luke McCarthy
 */
public interface Service extends ServiceDescription
{	
	/**
	 * Returns an OntClass view of the service's input OWL class.
	 * This operation causes the ontology defining the input class to be
	 * loaded along with any imported ontologies.
	 * 
	 * @return an OntClass view of the service's input OWL class
	 * @throws SADIException if there is an error loading the input class definition
	 */
	public abstract OntClass getInputClass() throws SADIException;
	
	/**
	 * Returns an OntClass view of the service's output OWL class.
	 * This operation causes the ontology defining the output class to be 
	 * loaded along with any imported ontologies.
	 * 
	 * @return an OntClass describing the output this service produces
	 * @throws SADIException if there is a problem with the output class definition
	 */
	public abstract OntClass getOutputClass() throws SADIException;
	
	/**
	 * Returns the property restrictions attached by this service.
	 * @return the property restrictions attached by this service
	 * @throws SADIException
	 */
	public abstract Collection<Restriction> getRestrictions() throws SADIException;

	/**
	 * Calls the service with the specified input.
	 * @param inputNode the input RDF root node
	 * @return the output from the service
	 * @throws ServiceInvocationException if an error occurs
	 */
	public abstract Model invokeService(Resource inputNode) throws ServiceInvocationException;
	
	/**
	 * Calls the service with the specified inputs.
	 * @param inputNodes the input RDF root nodes
	 * @return the output from the service
	 * @throws ServiceInvocationException if an error occurs
	 */
	public abstract Model invokeService(Iterable<Resource> inputNodes) throws ServiceInvocationException;
	
	/**
	 * Calls the service with the specified inputs.
	 * @param inputNodes the input RDF root nodes
	 * @return the output from the service
	 * @throws ServiceInvocationException if an error occurs
	 */
	public abstract Model invokeService(Iterator<Resource> inputNodes) throws ServiceInvocationException;
	
//	/**
//	 * Calls the service with the specified input and filters the output so
//	 * that only triples with the specified predicate are returned.
//	 * @param inputNode the input
//	 * @param predicate the desired predicate
//	 * @return a filtered collection of triples output from the service
//	 * @throws Exception
//	 * @deprecated
//	 */
//	public abstract Collection<Triple> invokeService(Resource inputNode, String predicate) throws ServiceInvocationException;
//	
//	/**
//	 * Calls the service with the specified inputs and filters the output so
//	 * that only triples with the specified predicate are returned.
//	 * @param inputNode the input
//	 * @param predicate the desired predicate
//	 * @return a filtered collection of triples output from the service
//	 * @throws Exception
//	 * @deprecated
//	 */
//	public abstract Collection<Triple> invokeService(Collection<Resource> inputNodes, String predicate) throws ServiceInvocationException;
	
	/**
	 * Returns true if the specified node is an instance of this service's
	 * input OWL class, false otherwise.
	 * This operation causes the ontology defining the input class to be
	 * loaded along with any imported ontologies.  Further, this operation
	 * involves reasoning over the specified node's data model; this can
	 * use a lot of memory and take a lot of time.
	 * @param resource an RDF node
	 * @return true if the specified node is an instance of this service's input class,
	 *         false otherwise.
	 * @throws SADIException if there is an error loading the input class definition
	 */
	public abstract boolean isInputInstance(Resource resource) throws SADIException;
	
	/**
	 * Returns all instances of this service's input OWL class in the 
	 * specified model.
	 * This operation causes the ontology defining the input class to be
	 * loaded along with any imported ontologies.  Further, this operation
	 * involves reasoning over the specified data model; this can
	 * use a lot of memory and take a lot of time.
	 * @param inputModel the input data model
	 * @return all instances of this service's input class in the specified model
	 * @throws SADIException if there is an error loading the input class definition
	 */
	public abstract Collection<Resource> discoverInputInstances(Model inputModel) throws SADIException;
}
