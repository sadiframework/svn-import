package ca.wilkinsonlab.sadi.service;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * This is the base class extended by synchronous SADI services.
 * @author Luke McCarthy
 */
@SuppressWarnings("serial")
public abstract class SynchronousServiceServlet extends ServiceServlet
{
	public void processInput(ServiceCall call)
	{
		processInput(call.getInputModel(), call.getOutputModel());
		call.getInputModel().close();
	}
	
	protected abstract void processInput(Model inputModel, Model outputModel);
}
