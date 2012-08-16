package org.sadiframework.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.service.simple.SimpleAsynchronousServiceServlet;
import org.sadiframework.utils.ServiceUtils;
import org.sadiframework.vocab.LSRN;
import org.sadiframework.vocab.SIO;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.uuw.services.remoting.EntryIterator;
import uk.ac.ebi.kraken.uuw.services.remoting.Query;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryBuilder;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;

import com.hp.hpl.jena.rdf.model.Resource;


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
@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/entrezGene2Uniprot.input.1.rdf",
				output = "http://sadiframework.org/examples/t/entrezGene2Uniprot.output.1.rdf"
		)
)
public class EntrezGene2UniProtServiceServlet extends SimpleAsynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(EntrezGene2UniProtServiceServlet.class);

	@Override
	public void processInput(Resource input, Resource output)
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
