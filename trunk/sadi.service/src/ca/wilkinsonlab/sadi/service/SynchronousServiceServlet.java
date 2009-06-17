package ca.wilkinsonlab.sadi.service;

import java.util.Map;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * This is the base class extended by synchronous SADI services.
 * @author Luke McCarthy
 */
public abstract class SynchronousServiceServlet extends ServiceServlet implements InputProcessor
{
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ServiceServlet#processInput(java.util.Map)
	 */
	@Override
	public void processInput(Map<Resource, Resource> inputOutputMap)
	{
		for (Resource input: inputOutputMap.keySet())
			getInputProcessor().processInput(input, inputOutputMap.get(input));
	}
	
	protected InputProcessor getInputProcessor()
	{
		return this;
	}
}
