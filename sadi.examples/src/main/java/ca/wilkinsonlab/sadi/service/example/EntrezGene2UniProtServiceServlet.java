package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.uuw.services.remoting.EntryIterator;
import uk.ac.ebi.kraken.uuw.services.remoting.Query;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryBuilder;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;

import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.service.simple.SimpleAsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.SIO;

/**
 * Maps Entrez Gene IDs to UniProt IDs.
 * 
 * I am using the UniProt API for this service because there 
 * were a number of obstacles to using the NCBI web services for
 * retrieving Entrez Gene cross-references. The primary problem is 
 * that the XML encodings of Entrez Gene records are huge (between 
 * 500k and 10MB). 
 * 
 * Unfortunately, batch processing of inputs is not possible for this 
 * service when using the UniProt API.  
 * 
 * Note: I tried to accomplish batching by using 
 * "Set operations on entry iterators" as described at
 * {@link http://www.ebi.ac.uk/uniprot/remotingAPI/doc.html#set}. 
 * Unfortunately, this did not work. (The effect is the same as
 * issuing the queries separately.)
 */
@SuppressWarnings("serial")
public class EntrezGene2UniProtServiceServlet extends SimpleAsynchronousServiceServlet 
{
	private static final Log log = LogFactory.getLog(EntrezGene2UniProtServiceServlet.class);
	

	@Override
	protected void processInput(Resource input, Resource output) 
	{
		String entrezGeneId = ServiceUtils.getDatabaseId(input, LSRN.Entrez.Gene);

		if(entrezGeneId == null) {
			log.error(String.format("skipping input, unable to determine EntrezGene ID for %s", input));
			return;
		}
		
		UniProtQueryService uniProtQueryService = UniProtJAPI.factory.getUniProtQueryService();
	    Query query = UniProtQueryBuilder.buildDatabaseCrossReferenceQuery(DatabaseType.GENEID, entrezGeneId);
	    
	    EntryIterator<UniProtEntry> entryIterator = uniProtQueryService.getEntryIterator(query);

	    for (UniProtEntry uniprotEntry : entryIterator) {
	    	String id = uniprotEntry.getPrimaryUniProtAccession().getValue();
	    	Resource uniprotNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.UniProt, id);
	    	output.addProperty(SIO.encodes, uniprotNode);
	    }
	}
	
}
