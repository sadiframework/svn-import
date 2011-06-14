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
import ca.wilkinsonlab.sadi.service.ServiceCall;
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
	protected void processInputBatch(ServiceCall call)
	{
		Collection<Resource> inputNodes = call.getInputNodes();
		Model outputModel = call.getOutputModel();
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
	}

	public abstract void processInput(UniProtEntry input, Resource output);
}
