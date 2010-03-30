package ca.wilkinsonlab.sadi.service;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * This is the base class extended by synchronous SADI services.
 * @author Luke McCarthy
 */
@SuppressWarnings("serial")
public abstract class SynchronousServiceServlet extends ServiceServlet
{
	@Override
	protected void outputSuccessResponse(HttpServletResponse response, Model outputModel) throws IOException
	{
		super.outputSuccessResponse(response, outputModel);
		closeOutputModel(outputModel);
	}
	
	@Override
	public void processInput(ServiceCall call)
	{
		processInput(call.getInputModel(), call.getOutputModel());
		closeInputModel(call.getInputModel());
	}
	
	protected abstract void processInput(Model inputModel, Model outputModel);
}
