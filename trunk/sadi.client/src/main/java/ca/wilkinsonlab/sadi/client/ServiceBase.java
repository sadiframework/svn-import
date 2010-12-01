package ca.wilkinsonlab.sadi.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import ca.wilkinsonlab.sadi.SADIException;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * An abstract class that naively implements most of the invocation methods
 * in the Service interface, allowing a concrete Service class to be defined
 * with as little code as possible.  
 * 
 * @author Luke McCarthy
 */
public abstract class ServiceBase implements Service
{
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#discoverInputInstances(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	public Collection<Resource> discoverInputInstances(Model inputModel)
	{
		return new ArrayList<Resource>(0);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getInputClass()
	 */
	@Override
	public OntClass getInputClass() throws SADIException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getOutputClass()
	 */
	@Override
	public OntClass getOutputClass() throws SADIException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getServiceURI()
	 */
	@Override
	public String getURI()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#invokeService(com.hp.hpl.jena.rdf.model.Resource)
     */
	public Collection<Triple> invokeService(Resource inputNode) throws ServiceInvocationException
	{
		return invokeService(Collections.singletonList(inputNode));
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#invokeService(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
     */
	public Collection<Triple> invokeService(Resource inputNode, String predicate) throws ServiceInvocationException
	{
		return filterByPredicate(invokeService(inputNode), predicate);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(java.util.Collection, java.lang.String)
	 */
	public Collection<Triple> invokeService(Collection<Resource> inputNodes, String predicate) throws ServiceInvocationException
	{
		return filterByPredicate(invokeService(inputNodes), predicate);
	}

	/* Filter a collection of triples to pass only those with the
	 * specified predicate.
	 */
	private Collection<Triple> filterByPredicate(Collection<Triple> results, String predicate)
	{
		Collection<Triple> filteredTriples = new ArrayList<Triple>();
		for (Triple triple: results) {
			if (triple.getPredicate().getURI().equals(predicate))
				filteredTriples.add(triple);
		}
		return filteredTriples;
	}
}
