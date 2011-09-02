package ca.wilkinsonlab.sadi.test;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service;

public class FindServicesTest
{
	public static void main(String[] args) throws SADIException
	{
		for (Service service: Config.getConfiguration().getMasterRegistry().findServicesByAttachedProperty(ResourceFactory.createProperty("http://semanticscience.org/resource/SIO_000219")) )
		{
			System.out.println(service);
		}	
	}
}
