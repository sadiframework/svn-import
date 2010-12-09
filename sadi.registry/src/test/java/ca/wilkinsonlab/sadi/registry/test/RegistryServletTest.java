package ca.wilkinsonlab.sadi.registry.test;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service;

public class RegistryServletTest
{
	public static void main(String[] args)
	{
		for (Service s: Config.getConfiguration().getMasterRegistry().findServicesByPredicate("http://sadiframework.org/examples/bmi.owl#BMI"))
			System.out.println(s);
	}
}
