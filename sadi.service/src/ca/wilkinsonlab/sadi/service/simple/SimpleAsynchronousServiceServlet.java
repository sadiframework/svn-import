package ca.wilkinsonlab.sadi.service.simple;

import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;

public abstract class SimpleAsynchronousServiceServlet extends AsynchronousServiceServlet
{
	@Override
	protected InputProcessingTask getInputProcessingTask(Collection<Resource> inputNodes)
	{
		return new SimpleInputProcessingTask(inputNodes);
	}
	
	protected abstract void processInput(Resource input, Resource output);
	
	protected class SimpleInputProcessingTask extends InputProcessingTask
	{
		public SimpleInputProcessingTask(Collection<Resource> inputNodes)
		{
			super(inputNodes);
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
