package ca.wilkinsonlab.sadi.service.simple;

import ca.wilkinsonlab.sadi.service.SynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Luke McCarthy
 */
public abstract class SimpleSynchronousServiceServlet extends SynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.SynchronousServiceServlet#processInput(com.hp.hpl.jena.rdf.model.Resource, com.hp.hpl.jena.rdf.model.Resource)
	 */
	@Override
	public abstract void processInput(Resource input, Resource output);
}
