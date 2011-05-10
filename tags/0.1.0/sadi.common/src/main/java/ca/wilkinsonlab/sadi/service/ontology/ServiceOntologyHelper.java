package ca.wilkinsonlab.sadi.service.ontology;

import ca.wilkinsonlab.sadi.ServiceDescription;
import ca.wilkinsonlab.sadi.beans.ServiceBean;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * An interface that reads/writes service configuration from/to an RDF model.
 * Implementing classes will read/write data according to a specific ontology.
 *  
 * @author Luke McCarthy
 */
public interface ServiceOntologyHelper
{
	/**
	 * Returns the rdf:type of a service instance in this ontology.
	 * @return the rdf:type of a service instance in this ontology
	 */
	public Resource getServiceClass();

	/**
	 * Returns a description of the service rooted at the specified resource.
	 * @param serviceRoot an RDF resource representing a SADI service
	 * @return the service description
	 * @throws ServiceOntologyException if the service RDF doesn't match the expected schema
	 */
	public ServiceDescription getServiceDescription(Resource serviceRoot) throws ServiceOntologyException;
	
	/**
	 * Populates the specified ServiceBean with a description of the service 
	 * rooted at the specified resource.
	 * @param serviceRoot an RDF resource representing a SADI service
	 * @param serviceBean the ServiceBean to populate
	 * @return the populated ServiceBean
	 * @throws ServiceOntologyException if the service RDF doesn't match the expected schema
	 */
	public ServiceBean copyServiceDescription(Resource serviceRoot, ServiceBean serviceBean) throws ServiceOntologyException;
	
	/**
	 * Return an RDF representation of the specified service description
	 * using a new in-memory model.
	 * @param service the service description
	 * @return an RDF representation of the service description
	 */
	public Resource createServiceNode(ServiceDescription service);
	
	/**
	 * Return an RDF representation of the specified service description
	 * using the specified model.
	 * @param service the service description
	 * @param model the RDF model to use
	 * @return an RDF representation of the service description
	 * @throws ServiceOntologyException if the service description is missing any required information
	 */
	public Resource createServiceNode(ServiceDescription service, Model model);
}
