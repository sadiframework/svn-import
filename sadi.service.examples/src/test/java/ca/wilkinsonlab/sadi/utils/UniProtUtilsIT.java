package ca.wilkinsonlab.sadi.utils;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.Test;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.uuw.services.remoting.Query;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryBuilder;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;

public class UniProtUtilsIT
{
	private static final Logger log = Logger.getLogger(UniProtUtilsIT.class);
	
	@Test
	public void testUniProtJAPI()
	{
		log.info("getting UniProt query service");
		UniProtQueryService uniProtQueryService = UniProtJAPI.factory.getUniProtQueryService();
		log.info("building UniProt query");
		Query query = UniProtQueryBuilder.buildIDListQuery(Arrays.asList(new String[]{"P12345"}));
		log.info("executing UniProt query");
		for (UniProtEntry entry: uniProtQueryService.getEntryIterator(query)) {
			log.info(String.format("found entry %s", entry.getUniProtId()));
		}
	}
}
