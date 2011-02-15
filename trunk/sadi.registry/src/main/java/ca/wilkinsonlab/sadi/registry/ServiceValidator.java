package ca.wilkinsonlab.sadi.registry;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.beans.ServiceBean;
import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.utils.QueryableErrorHandler;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class ServiceValidator
{
	public ServiceBean validateService(String serviceURI) throws SADIException
	{
		Model model = ModelFactory.createDefaultModel();
		QueryableErrorHandler errorHandler = new QueryableErrorHandler();
		model.getReader().setErrorHandler(errorHandler);
		model.read(serviceURI);
		if (errorHandler.hasLastError())
			throw new SADIException(String.format("error reading RDF model from %s: %s",
					serviceURI, errorHandler.getLastError().getMessage()));
		
		Resource serviceNode = model.getResource(serviceURI);
		if (!model.containsResource(serviceNode))
			throw new SADIException(String.format("RDF model at %s does not contain any statements about %s",
					serviceURI, serviceNode));
		
		ServiceBean service = new ServiceBean();
		new MyGridServiceOntologyHelper().copyServiceDescription(serviceNode, service);
		return service;
	}
}
