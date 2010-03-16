package ca.wilkinsonlab.sadi.service.example;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.Test;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.uuw.services.remoting.Query;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryBuilder;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class UniProtUtilsTest
{
	private static final Logger log = Logger.getLogger(UniProtUtilsTest.class);
	
	@Test
	public void testGetUniprotId()
	{
		String message = "incorrect UniProt ID from URI";
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://purl.uniprot.org/uniprot/P12345")));
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://www.uniprot.org/uniprot/P12345")));
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://www.uniprot.org/uniprot/P12345.rdf")));
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://biordf.net/moby/UniProt/P12345")));
		assertEquals(message, "P12345", UniProtUtils.getUniProtId(ResourceFactory.createResource("http://lsrn.org/UniProt:P12345")));
	}
	
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
