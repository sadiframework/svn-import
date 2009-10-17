package ca.wilkinsonlab.sadi.service.example;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Resource;

@SuppressWarnings("serial")
public abstract class UniProtServiceServlet extends AsynchronousServiceServlet
{
	protected UniProtServiceServlet()
	{
		super();
		
		// override input batch size to 1024, the maximum for the UniProt API...
		inputBatchSize = 1024;
	}
	
	@Override
	protected InputProcessingTask getInputProcessingTask(Collection<Resource> inputNodes)
	{
		return new UniProtInputProcessingTask(inputNodes);
	}
	
	protected abstract void processInput(UniProtEntry input, Resource output);
	
	private class UniProtInputProcessingTask extends InputProcessingTask
	{	
		public UniProtInputProcessingTask(Collection<Resource> inputNodes)
		{
			super(inputNodes);
		}

		public void run()
		{
			Map<String, Resource> idToOutputNode = new HashMap<String, Resource>(inputNodes.size());
			for (Resource inputNode: inputNodes) {
				String id = UniProtUtils.getUniProtId(inputNode);
				idToOutputNode.put(id, outputModel.getResource(inputNode.getURI()));
			}
			for (Entry<String, UniProtEntry> entry: UniProtUtils.getUniProtEntries(idToOutputNode.keySet()).entrySet()) {
				processInput(entry.getValue(), idToOutputNode.get(entry.getKey()));
			}
			success();
		}
	}
}
