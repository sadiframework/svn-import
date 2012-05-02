package org.sadiframework.service.simple;

import org.sadiframework.service.SynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Luke McCarthy
 */
public abstract class SimpleSynchronousServiceServlet extends SynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	
	/* (non-Javadoc)
	 * @see org.sadiframework.service.SynchronousServiceServlet#processInput(com.hp.hpl.jena.rdf.model.Resource, com.hp.hpl.jena.rdf.model.Resource)
	 */
	@Override
	public abstract void processInput(Resource input, Resource output);
}
