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
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.service.simple.SimpleAsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.Properties;

import com.hp.hpl.jena.rdf.model.Resource;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/pdb2uniprot.input.1.rdf",
				output = "http://sadiframework.org/examples/t/pdb2uniprot.output.1.rdf"
		)
)
public class Pdb2UniProtServiceServlet extends SimpleAsynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(Pdb2UniProtServiceServlet.class);

	@Override
	public void processInput(Resource input, Resource output)
	{
		String pdbId = ServiceUtils.getDatabaseId(input, LSRN.PDB);

		if(pdbId == null) {
			log.error(String.format("unable to determine PDB ID for %s", input));
			return;
		}

		UniProtQueryService uniProtQueryService = UniProtJAPI.factory.getUniProtQueryService();
	    Query query = UniProtQueryBuilder.buildDatabaseCrossReferenceQuery(DatabaseType.PDB, pdbId.toUpperCase());

	    EntryIterator<UniProtEntry> entryIterator = uniProtQueryService.getEntryIterator(query);

	    for (UniProtEntry uniprotEntry : entryIterator) {
    		String uniprotId = uniprotEntry.getPrimaryUniProtAccession().getValue();
	    	Resource uniprotNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.UniProt, uniprotId);
	    	output.addProperty(Properties.is3DStructureOf, uniprotNode);
	    }
	}
}
