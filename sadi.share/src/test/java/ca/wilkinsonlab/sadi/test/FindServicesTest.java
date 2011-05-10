package ca.wilkinsonlab.sadi.test;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service;

public class FindServicesTest
{
	public static void main(String[] args)
	{
		for (Service service: Config.getConfiguration().getMasterRegistry().findServicesByPredicate("http://semanticscience.org/resource/SIO_000219") )
		{
			System.out.println(service);
		}	
	}
}
