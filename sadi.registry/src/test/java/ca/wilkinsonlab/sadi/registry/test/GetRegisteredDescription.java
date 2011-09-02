package ca.wilkinsonlab.sadi.registry.test;

import ca.wilkinsonlab.sadi.beans.ServiceBean;
import ca.wilkinsonlab.sadi.registry.Registry;

public class GetRegisteredDescription
{
	public static void main(String[] args) throws Exception
	{
		Registry registry = Registry.getRegistry();
		for (String uri: args) {
			ServiceBean service = registry.getServiceBean(uri);
			System.out.println(service);
		}
	}
}
