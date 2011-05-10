package ca.wilkinsonlab.sadi.service.example;

import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.ResourceUtils;

@SuppressWarnings("serial")
public class EchoServiceServlet extends SimpleSynchronousServiceServlet
{	
	public void processInput(Resource input, Resource output)
	{
		output.getModel().add(ResourceUtils.reachableClosure(input));
	}
}
