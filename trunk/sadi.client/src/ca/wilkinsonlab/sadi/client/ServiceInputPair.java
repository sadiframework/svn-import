package ca.wilkinsonlab.sadi.client;

import java.util.Collection;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A simple class encapsulating a Service and a potential input to that
 * service.
 * @author Luke McCarthy
 */
public class ServiceInputPair
{
	Service service;
	Resource input;
	
	public ServiceInputPair(Service service, Resource input)
	{
		this.service = service;
		this.input = input;
	}
	
	public Service getService()
	{
		return service;
	}
	
	public Resource getInput()
	{
		return input;
	}
	
	public Collection<Triple> invoke() throws Exception
	{
		return service.invokeService(input);
	}
	
	public String toString()
	{
		return String.format("%s( %s )", service, input);
	}
}
