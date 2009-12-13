package ca.wilkinsonlab.sadi.optimizer.statistics;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ca.wilkinsonlab.sadi.optimizer.PredicateStatsDB;
import ca.wilkinsonlab.sadi.optimizer.statistics.PredicateStatsDBAdmin;
import ca.wilkinsonlab.sadi.sparql.SPARQLRegistry;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.vocab.PredicateStats;
import ca.wilkinsonlab.sadi.client.Config;

public class PredicateStatsDBAdminTest {

	private PredicateStatsDBAdmin statsDBAdmin;
	private PredicateStatsDB statsDB;
	private SPARQLRegistry sparqlRegistry = Config.getSPARQLRegistry();
	
	/* TODO: These tests depend on third-party endpoints, which is bad. Fix
	 * this by setting up a test registry containing only test (dummy) SPARQL
	 * endpoints.
	 * 
	 * For the time being, the affected unit tests have been commented out.
	 */
	
	/*
	private static final String EXAMPLE_PREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"; //"http://bio2rdf.org/ns/bio2rdf#xGO";
	private static final String EXAMPLE_ENDPOINT = "http://omim.bio2rdf.org/sparql";
	*/
	
	@Before
	public void setUp() throws Exception {
		 statsDBAdmin = new PredicateStatsDBAdmin();
		 statsDB = new PredicateStatsDB();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testUpdateAverageStats() {
		
		try {

			if(!statsDB.isPopulated())
				throw new RuntimeException("Stats DB must first be initialized with some example statistics");
			
			statsDBAdmin.updateAverageStats();
			
			// Don't know what values to expect here, just test that we can get them.

			// avg reverse time
			assertTrue(statsDB.getAverageStat(false, false) > 0);
			// avg forward time
			assertTrue(statsDB.getAverageStat(false, true) > 0);
			// avg reverse selectivity
			assertTrue(statsDB.getAverageStat(true, false) > 0);
			// avg forward selectivity
			assertTrue(statsDB.getAverageStat(true, true) > 0);
			
		}
		catch(Exception e) {
			e.printStackTrace();
			fail("Failed to calculate averages for samples: " + e);
		}
		
		
	}
	
	/* TODO: These tests are to remain disabled until they no longer depend on
	 * third-party endpoints (see TODO at top of file).
	 */

	/*
	@Test
	public void testComputeStatsForPredicate()
	{
		try {
			if(sparqlRegistry.findEndpointsByPredicate(EXAMPLE_PREDICATE).size() == 0)
				throw new RuntimeException("There must be at least one endpoint indexed in the SPARQL registry which contains " + EXAMPLE_PREDICATE);

			statsDBAdmin.removeStatsForPredicate(EXAMPLE_PREDICATE);
			// 1 sample for selectivity and 1 sample for query execution time
			statsDBAdmin.computeStatsForPredicate(EXAMPLE_PREDICATE, 1, new Date());

			// forward selectivity
			assertTrue(statsDB.getAverageSampleValue(EXAMPLE_PREDICATE, true, true) != PredicateStatsDB.NO_SAMPLES_AVAILABLE);
			// reverse selectivity
			assertTrue(statsDB.getAverageSampleValue(EXAMPLE_PREDICATE, true, false) != PredicateStatsDB.NO_SAMPLES_AVAILABLE);
			// forward query execution time
			assertTrue(statsDB.getAverageSampleValue(EXAMPLE_PREDICATE, false, true) != PredicateStatsDB.NO_SAMPLES_AVAILABLE);
			// reverse query execution time
			assertTrue(statsDB.getAverageSampleValue(EXAMPLE_PREDICATE, false, false) != PredicateStatsDB.NO_SAMPLES_AVAILABLE);
		}
		catch(Exception e) {
			e.printStackTrace();
			fail("Failed to compute stats for test predicate " + EXAMPLE_PREDICATE + ": " + e);
		}
	}
	
	@Test
	public void testComputeStatsForPredicateWithStalenessDate()
	{
		try {
			
			if(sparqlRegistry.findEndpointsByPredicate(EXAMPLE_PREDICATE).size() == 0)
				throw new RuntimeException("There must be at least one endpoint indexed in the SPARQL registry which contains " + EXAMPLE_PREDICATE);
			
			statsDBAdmin.removeStatsForPredicate(EXAMPLE_PREDICATE);
			statsDBAdmin.computeStatsForPredicate(EXAMPLE_PREDICATE, 1, new Date());
			long timestamp = new Date().getTime();
			statsDBAdmin.computeStatsForPredicate(EXAMPLE_PREDICATE, 2, new Date(timestamp));
			
			// Ensure that the original samples were replaced.
			String query = 
				"SELECT * FROM %u% WHERE {\n" +
				"    %u% ?p ?sample .\n" +
				"    ?sample %u% ?timestamp .\n" +
				"    FILTER (?timestamp <= %v%)\n" +
				"}\n";

			query = SPARQLStringUtils.strFromTemplate(query, 
						statsDB.getGraphName(),
						EXAMPLE_PREDICATE,
						PredicateStats.PREDICATE_TIMESTAMP,
						String.valueOf(timestamp));
			
			assertTrue(statsDBAdmin.selectQuery(query).size() == 0);
		}
		catch(Exception e) {
			e.printStackTrace();
			fail("Failed to update stats for " + EXAMPLE_PREDICATE + " using a staleness date: " + e);
		}
	}
	
	@Test 
	public void testComputeStatsForEndpoint()
	{
		try {
			// 1 sample for selectivity and 1 sample for query execution time
			statsDBAdmin.computeStatsForEndpoint(EXAMPLE_ENDPOINT, 1, new Date());

			// Pick any predicate and make sure there were stats recorded for it
			String examplePredicate = sparqlRegistry.getPredicatesForEndpoint(EXAMPLE_ENDPOINT).iterator().next(); 
			
			// forward selectivity
			assertTrue(statsDB.getAverageSampleValue(examplePredicate, true, true) != PredicateStatsDB.NO_SAMPLES_AVAILABLE);
			// reverse selectivity
			assertTrue(statsDB.getAverageSampleValue(examplePredicate, true, false) != PredicateStatsDB.NO_SAMPLES_AVAILABLE);
			// forward query execution time
			assertTrue(statsDB.getAverageSampleValue(examplePredicate, false, true) != PredicateStatsDB.NO_SAMPLES_AVAILABLE);
			// reverse query execution time
			assertTrue(statsDB.getAverageSampleValue(examplePredicate, false, false) != PredicateStatsDB.NO_SAMPLES_AVAILABLE);
			
		}
		catch(Exception e) {
			e.printStackTrace();
			fail("Failed to compute stats for endpoint " + EXAMPLE_ENDPOINT + ": " + e);
		}
	}
	*/
}
