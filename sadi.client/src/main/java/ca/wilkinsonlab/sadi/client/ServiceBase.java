package ca.wilkinsonlab.sadi.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.beans.ServiceBean;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * An abstract class that naively implements most of the invocation methods
 * in the Service interface, allowing a concrete Service class to be defined
 * with as little code as possible.  
 * 
 * @author Luke McCarthy
 */
public abstract class ServiceBase extends ServiceBean implements Service
{
	private static final long serialVersionUID = 1L;

//	/* (non-Javadoc)
//	 * @see ca.wilkinsonlab.sadi.client.Service#getInputClass()
//	 */
//	@Override
//	public OntClass getInputClass() throws SADIException
//	{
//		return OwlUtils.OWL_Nothing;
//	}
//
//	/* (non-Javadoc)
//	 * @see ca.wilkinsonlab.sadi.client.Service#getOutputClass()
//	 */
//	@Override
//	public OntClass getOutputClass() throws SADIException
//	{
//		return OwlUtils.OWL_Nothing;
//	}
//	
//	/* (non-Javadoc)
//	 * @see ca.wilkinsonlab.sadi.client.Service#getRestrictions()
//	 */
//	@Override
//	public Collection<Restriction> getRestrictions() throws SADIException
//	{
//		return Collections.emptyList();
//	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#invokeService(com.hp.hpl.jena.rdf.model.Resource)
     */
	public Model invokeService(Resource inputNode) throws ServiceInvocationException
	{
		return invokeService(Collections.singleton(inputNode).iterator());
	}

	@Override
	public Model invokeService(Iterable<Resource> inputNodes) throws ServiceInvocationException
	{
		return invokeService(inputNodes.iterator());
	}

//	@Override
//	public Model invokeService(Iterator<Resource> inputNodes) throws ServiceInvocationException
//	{
//	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#isInputInstance(com.hp.hpl.jena.rdf.model.Resource)
     */
	@Override
	public boolean isInputInstance(Resource resource) throws SADIException
	{
		Model inputModel = resource.getModel();
		OntModel reasoningModel = getInputClass().getOntModel();
		OntClass inputClass = getInputClass();
		try {
			reasoningModel.addSubModel(inputModel);
			return reasoningModel.getIndividual(resource.getURI()).hasOntClass(inputClass);
		} catch (Exception e) {
			/* we're probably here because the service definition is incorrect,
			 * and we don't want a bad service spoiling everything for everybody...
			 */
			getLog().error(String.format("error classifying %s as an instance of %s", resource, inputClass), e);
			return false;
		} finally {
			reasoningModel.removeSubModel(inputModel);
		}
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#discoverInputInstances(com.hp.hpl.jena.rdf.model.Model)
     */
	@Override
	public synchronized Collection<Resource> discoverInputInstances(Model inputModel) throws SADIException
	{	
		OntModel reasoningModel = getInputClass().getOntModel();
		OntClass inputClass = getInputClass();
		try {
			reasoningModel.addSubModel(inputModel);
			Collection<Resource> instancesInInputModel = new ArrayList<Resource>();
			for (Iterator<? extends OntResource> instances = inputClass.listInstances(); instances.hasNext(); ) {
				OntResource instance = instances.next();
				instancesInInputModel.add(instance.inModel(inputModel).as(Resource.class));
			}
			return instancesInInputModel;
		} catch (Exception e) {
			/* we're probably here because the service definition is incorrect,
			 * and we don't want a bad service spoiling everything for everybody...
			 */
			getLog().error(String.format("error discovering instances of %s", inputClass), e);
			return Collections.emptyList();
		} finally {
			reasoningModel.removeSubModel(inputModel);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getURI();
	}
	
	/**
	 * Returns the log4j Logger associated with the concrete service.
	 * @return the log4j Logger associated with the concrete service
	 */
	protected abstract Logger getLog();
}
