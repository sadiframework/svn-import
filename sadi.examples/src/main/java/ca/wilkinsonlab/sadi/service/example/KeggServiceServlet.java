package ca.wilkinsonlab.sadi.service.example;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.rpc.ServiceException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.utils.KeggUtils;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN.LSRNRecordType;

@SuppressWarnings("serial")
public abstract class KeggServiceServlet extends AsynchronousServiceServlet 
{
	private static final Log log = LogFactory.getLog(KeggServiceServlet.class);
	
	@Override
	public int getInputBatchSize()
	{
		// override input batch size to 1024, the maximum for the KEGG API...
		return 100;
	}
	
	@Override
	protected InputProcessingTask getInputProcessingTask(Model inputModel, Collection<Resource> inputNodes)
	{
		return new KeggInputProcessingTask(inputModel, inputNodes);
	}
	
	protected abstract void processInput(String keggRecordId, String keggRecord, Resource output);
	
	protected abstract LSRNRecordType getInputRecordType();

	protected String getInputIdPrefix() 
	{
		return "";
	}

	private class KeggInputProcessingTask extends InputProcessingTask
	{	
		public KeggInputProcessingTask(Model inputModel, Collection<Resource> inputNodes)
		{
			super(inputModel, inputNodes);
		}

		public void run()
		{
			Map<String, Resource> idToOutputNode = new HashMap<String, Resource>(inputNodes.size());
			for (Resource inputNode: inputNodes) {
				String id = ServiceUtils.getDatabaseId(inputNode, getInputRecordType());
				if(id == null) {
					log.warn(String.format("skipping input node %s, unable to determine KEGG record ID", inputNode.getURI()));
					continue;
				}
				id = String.format("%s%s", getInputIdPrefix(), id);
				idToOutputNode.put(id, outputModel.getResource(inputNode.getURI()));
			}
			
			Set<Entry<String,String>> entries;
			try {
				entries = KeggUtils.getKeggRecords(idToOutputNode.keySet()).entrySet();
			} catch(ServiceException e) {
				throw new RuntimeException("error initializing KEGG API", e);
			} catch(RemoteException e) {
				throw new RuntimeException("error contacting KEGG service", e);
			}
			
			log.debug(String.format("retrieved %d entries", entries.size()));
			for (Entry<String,String> entry: entries) {
				String keggId = StringUtils.removeStart(entry.getKey(), getInputIdPrefix());
				processInput(keggId, entry.getValue(), idToOutputNode.get(entry.getKey()));
			}
			success();
		}
	}
}
