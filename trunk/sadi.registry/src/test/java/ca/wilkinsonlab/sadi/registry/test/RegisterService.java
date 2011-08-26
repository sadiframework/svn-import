package ca.wilkinsonlab.sadi.registry.test;

import java.io.FileInputStream;
import java.io.IOException;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceFactory;
import ca.wilkinsonlab.sadi.registry.Registry;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RegisterService
{
	public static void main(String[] args) throws SADIException, IOException
	{	
		Model serviceModel = ModelFactory.createDefaultModel();
		String serviceURI = "http://bolleman.local:8080/blast/";
		serviceModel.read(new FileInputStream("/Users/luke/Downloads/blast-uniprot-def.rdf"), serviceURI);
		Service service = ServiceFactory.createService(serviceModel, serviceURI);
		Registry registry = Registry.getRegistry();
		registry.registerService(service);
	}
}
