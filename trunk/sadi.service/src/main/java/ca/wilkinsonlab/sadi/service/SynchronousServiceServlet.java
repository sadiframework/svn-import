package ca.wilkinsonlab.sadi.service;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * This is the base class extended by synchronous SADI services.
 * @author Luke McCarthy
 */
public abstract class SynchronousServiceServlet extends ServiceServlet
{
	private static final long serialVersionUID = 1L;

	@Override
	protected void outputSuccessResponse(HttpServletResponse response, Model outputModel) throws IOException
	{
		super.outputSuccessResponse(response, outputModel);
		closeOutputModel(outputModel);
	}
	
	@Override
	public void processInput(ServiceCall call) throws Exception
	{
		for (Resource inputNode: call.getInputNodes()) {
			Resource outputNode = call.getOutputModel().getResource(inputNode.getURI());
			processInput(inputNode, outputNode);
		}
		closeInputModel(call.getInputModel());
	}
	
	public void processInput(Resource input, Resource output)
	{
	}
}
