package ca.wilkinsonlab.sadi.client;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;

public class ServiceFactory
{
	/**
	 * Create a service object by loading the service description at the
	 * specified URL.
	 * @param serviceURL the service URL
	 * @return the new Service object
	 * @throws SADIException if there is an error reading the service description
	 */
	public static Service createService(String serviceURI) throws SADIException
	{
		ServiceImpl service = new ServiceImpl();
		service.setURI(serviceURI);
		service.loadServiceModel();
		return service;
	}
	
	/**
	 * Create a service object by using the service description rooted at
	 * the specified Resource.
	 * @param serviceNode the root of the service description
	 * @return the new Service object
	 * @throws SADIException if there is an error reading the service description
	 */
	public static Service createService(Resource serviceNode) throws SADIException
	{
		ServiceImpl service = new ServiceImpl();
		MyGridServiceOntologyHelper ontologyHelper = new MyGridServiceOntologyHelper();
		ontologyHelper.copyServiceDescription(serviceNode, service);
		ontologyHelper.createServiceNode(service, service.model);
		return service;
	}
	
	public static Service createService(Model model, String serviceURI) throws SADIException
	{
		return createService(model.getResource(serviceURI));
	}
	
//	/**
//	 * Create a Service object from the details in the supplied map.
//	 * This map must contain values for all the required service fields.
//	 * In practice, the map will be a variable binding for an appropriate 
//	 * SPARQL query on the registry endpoint, which allows the registry to 
//	 * return an immediately useful Service object without the client having
//	 * to fetch the full service description every time.
//	 * @deprecated this method doesn't need to be here; only RegistryImpl should
//	 * use it; anything else is almost certainly incorrect.
//	 * @param serviceInfo the service map
//	 * @return the new Service object
//	 */
//	static Service createService(Map<String, String> serviceInfo)
//	{
//		ServiceImpl service = new ServiceImpl();
//		service.setURI(serviceInfo.get("serviceURI"));
//		service.setName(serviceInfo.get("name"));
//		service.setDescription(serviceInfo.get("description"));
//		service.setInputClassURI(serviceInfo.get("inputClassURI"));
//		service.setOutputClassURI(serviceInfo.get("outputClassURI"));
//		new MyGridServiceOntologyHelper().createServiceNode(service, service.getServiceModel());
//		return service;
//	}
}
