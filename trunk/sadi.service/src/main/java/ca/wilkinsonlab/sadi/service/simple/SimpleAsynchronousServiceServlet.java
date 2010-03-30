package ca.wilkinsonlab.sadi.service.simple;

import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;

@SuppressWarnings("serial")
public abstract class SimpleAsynchronousServiceServlet extends AsynchronousServiceServlet
{
	@Override
	protected InputProcessingTask getInputProcessingTask(Model inputModel, Collection<Resource> inputNodes)
	{
		return new SimpleInputProcessingTask(inputModel, inputNodes);
	}
	
	protected abstract void processInput(Resource input, Resource output);
	
	protected class SimpleInputProcessingTask extends InputProcessingTask
	{
		public SimpleInputProcessingTask(Model inputModel, Collection<Resource> inputNodes)
		{
			super(inputModel, inputNodes);
		}

		public void run()
		{
			for (Resource inputNode: inputNodes) {
				Resource outputNode = outputModel.getResource(inputNode.getURI());
				processInput(inputNode, outputNode);
			}
			success();
		}
	}
}
