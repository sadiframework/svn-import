package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.SynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.ResourceUtils;

public class EchoServiceServlet extends SynchronousServiceServlet
{	
	public void processInput(Resource input, Resource output)
	{
		output.getModel().add(ResourceUtils.reachableClosure(input));
	}
}
