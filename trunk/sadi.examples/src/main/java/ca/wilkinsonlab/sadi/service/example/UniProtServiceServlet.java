package ca.wilkinsonlab.sadi.service.example;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.utils.UniProtUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

@SuppressWarnings("serial")
public abstract class UniProtServiceServlet extends AsynchronousServiceServlet
{
	private static final Log log = LogFactory.getLog(UniProtServiceServlet.class);
	
	@Override
	public int getInputBatchSize()
	{
		// override input batch size to 1024, the maximum for the UniProt API...
		return 1024;
	}
	
	@Override
	protected InputProcessingTask getInputProcessingTask(Model inputModel, Collection<Resource> inputNodes)
	{
		return new UniProtInputProcessingTask(inputModel, inputNodes);
	}
	
	protected abstract void processInput(UniProtEntry input, Resource output);
	
	private class UniProtInputProcessingTask extends InputProcessingTask
	{	
		public UniProtInputProcessingTask(Model inputModel, Collection<Resource> inputNodes)
		{
			super(inputModel, inputNodes);
		}

		public void run()
		{
			Map<String, Resource> idToOutputNode = new HashMap<String, Resource>(inputNodes.size());
			for (Resource inputNode: inputNodes) {
				String id = UniProtUtils.getUniProtId(inputNode);
				idToOutputNode.put(id, outputModel.getResource(inputNode.getURI()));
			}
			Set<Entry<String, UniProtEntry>> entries = UniProtUtils.getUniProtEntries(idToOutputNode.keySet()).entrySet();
			log.debug(String.format("retrieved %d entries", entries.size()));
			for (Entry<String, UniProtEntry> entry: entries) {
				processInput(entry.getValue(), idToOutputNode.get(entry.getKey()));
			}
			success();
		}
	}
}
