package org.sadiframework.client.example;

import java.util.Collection;

import org.apache.log4j.Logger;

import org.sadiframework.SADIException;
import org.sadiframework.client.Config;
import org.sadiframework.client.Registry;
import org.sadiframework.client.Service;
import org.sadiframework.client.ServiceFactory;
import org.sadiframework.client.ServiceInvocationException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;

/**
 * This class demonstrates how to find and call services using the SADI API.
 * 
 * @author Luke McCarthy
 */
public class FindAndCallServices
{
	private static final Logger log = Logger.getLogger(FindAndCallServices.class);
	
 	public static void main(String[] args)
 	{
 		/* the master registry is an interface that allows you to query all
 		 * configured registries at the same time;
 		 * see http://code.google.com/p/sadi/wiki/CustomRegistry for
 		 * information about how to add your own custom registries to the
 		 * master configuration...
 		 */
 		Registry registry = Config.getConfiguration().getMasterRegistry();
 		
 		/* find services by the properties they attach;
 		 * see Registry javadoc for more ways to find services...
 		 */
 		Property p = ResourceFactory.createProperty("http://sadiframework.org/hello.owl#greeting");
 		try {
 			for (Service service: registry.findServicesByAttachedProperty(p)) {
 				log.info(String.format("found service %s that attaches property %s", service, p));
 			}
 		} catch (SADIException e) {
 			log.error("error contacting registry", e);
 		}
 		
 		/* or instantiate a service directly...
 		 */
 		Service service;
		try {
			service = ServiceFactory.createService("http://sadiframework.org/examples/hello");
		} catch (SADIException e) {
			log.error("error instantiating service", e);
			service = null;
		}
 		
 		if (service != null) {
 			/* normally you'll have input data already from some other source,
 	 		 * but here we'll construct it programatically...
 	 		 */
 	 		Model model = ModelFactory.createDefaultModel();
 	 		Resource guy = model.createResource("http://example.com/guy");
 	 		guy.addProperty(FOAF.name, "Guy Incognito");
 	 		
 	 		/* invoke the service on a known input...
 	 		 */
			try {
				Model output = service.invokeService(guy);
		 		output.write(System.out, "N3");
		 		output.close();
			} catch (ServiceInvocationException e) {
				log.error("error invoking service", e);
			}
			
			/* dynamically identify instances of the service's input class;
 	 		 * see Service javadoc for more things you can do with the service...
 	 		 */
			try {
				Collection<Resource> inputs = service.discoverInputInstances(model);
				System.out.println(String.format("found input instances %s", inputs));
				Model output = service.invokeService(inputs);
				output.write(System.out, "N3");
		 		output.close();
			} catch (SADIException e) {
				e.printStackTrace();
			}
 		}
	}
}
