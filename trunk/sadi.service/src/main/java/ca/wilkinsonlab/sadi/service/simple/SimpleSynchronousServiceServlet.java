package ca.wilkinsonlab.sadi.service.simple;

import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.service.ServiceCall;
import ca.wilkinsonlab.sadi.service.SynchronousServiceServlet;

/**
 * @author Luke McCarthy
 */
public abstract class SimpleSynchronousServiceServlet extends SynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	
	private Resource parameters;
	public Resource getParameters()
	{
		return parameters;
	}
	
	@Override
	public void processInput(ServiceCall call) throws Exception
	{
		synchronized (this) {
			parameters = call.getParameters();
			super.processInput(call);
			parameters = null;
		}
	}
}
