package ca.wilkinsonlab.sadi.registry.test;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.registry.Registry;
import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;

public class UnregisterService
{
	public static void main(String[] args) throws SADIException
	{
		String serviceURI = "http://131.156.145.69:8080/WSDL2SADI-0.0.1-SNAPSHOT/SAWSDL2SADIServlet/getDataSet_input";
		Registry registry = Registry.getRegistry();
		Resource serviceNode = registry.getModel().getResource(serviceURI);
		exploreService(serviceNode);
		registry.unregisterService(serviceURI);
	}
	
	private static void exploreService(Resource serviceNode)
	{
		StmtIterator decorationNodes = serviceNode.listProperties(SADI.decoratesWith);
		try {
			while (decorationNodes.hasNext()) {
				Resource decorationNode = decorationNodes.next().getResource();
				printProperties(decorationNode);
				Resource onProperty = decorationNode.getRequiredProperty(OWL.onProperty).getResource();
				printProperties(onProperty);
				Resource valuesFrom = null;
				if (decorationNode.hasProperty(OWL.someValuesFrom))
					valuesFrom = decorationNode.getRequiredProperty(OWL.someValuesFrom).getResource();
				if (valuesFrom != null)
					printProperties(valuesFrom);
			}
		} finally {
			decorationNodes.close();
		}		
	}
	
	private static void printProperties(Resource s)
	{
		StmtIterator properties = s.listProperties();
		while (properties.hasNext()) {
			System.out.println(properties.next());
		}
		properties.close();
	}
}
