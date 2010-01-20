package ca.wilkinsonlab.sadi.registry;

import com.hp.hpl.jena.rdf.model.ResIterator;

public class ConfigTest
{
	public static void main(String[] args)
	{
		Registry registry = Registry.getRegistry();
		registry.getModel().write(System.out);
		
		for (ResIterator i = registry.getRegisteredServiceNodes(); i.hasNext();)
			System.out.println(i.nextResource());
	}
}
