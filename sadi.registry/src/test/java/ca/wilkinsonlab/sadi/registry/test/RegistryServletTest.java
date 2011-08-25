package ca.wilkinsonlab.sadi.registry.test;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.ServiceFactory;
import ca.wilkinsonlab.sadi.registry.Registry;
import ca.wilkinsonlab.sadi.service.validation.ServiceValidator;

import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

public class RegistryServletTest
{
	public static void main(String[] args) throws SADIException
	{
		for (ResIterator i = Registry.getRegistry().getRegisteredServiceNodes(); i.hasNext(); ) {
			Resource service = i.next();
			System.out.println("validating service " + service);
			try {
				ServiceValidator.validateService(service);
				ServiceFactory.createService(service.getURI()).getInputClass();
			} catch (SADIException e) {
				e.printStackTrace();
			}
		}
	}
}
