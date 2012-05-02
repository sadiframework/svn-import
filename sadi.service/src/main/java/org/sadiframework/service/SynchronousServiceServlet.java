package org.sadiframework.service;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * This is the base class extended by synchronous SADI services.
 * @author Luke McCarthy
 */
public abstract class SynchronousServiceServlet extends ServiceServlet
{
	private static final long serialVersionUID = 1L;
	
	/* (non-Javadoc)
	 * @see org.sadiframework.service.ServiceServlet#processInput(org.sadiframework.service.ServiceCall)
	 */
	@Override
	protected void processInput(ServiceCall call) throws Exception
	{
		Resource parameters = call.getParameters();
		boolean needsParameters = !parameters.hasProperty(RDF.type, OWL.Nothing);
		for (Resource inputNode: call.getInputNodes()) {
			Resource outputNode = call.getOutputModel().getResource(inputNode.getURI());
			if (needsParameters)
				processInput(inputNode, outputNode, parameters);
			else
				processInput(inputNode, outputNode);
		}
	}
	
	/**
	 * Process a single input, reading properties from an input node and 
	 * attaching properties to the corresponding output node.
	 * @param input the input node
	 * @param output the output node
	 */
	public void processInput(Resource input, Resource output) throws Exception
	{
	}
	
	/**
	 * Process a single input, reading properties from an input node and 
	 * attaching properties to the corresponding output node.
	 * @param input the input node
	 * @param output the output node
	 * @param parameters the populated parameters object
	 */
	public void processInput(Resource input, Resource output, Resource parameters) throws Exception
	{
	}
}
