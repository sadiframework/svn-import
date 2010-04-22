package ca.wilkinsonlab.sadi.test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import ca.wilkinsonlab.sadi.share.Config;
import ca.wilkinsonlab.sadi.share.SHAREQueryClient;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

/**
 * Tests for queries that use variables in the predicate
 * positions of triple patterns.
 * 
 * These queries are separate from the demo queries run by
 * QueryClientTest because they are extremely slow.  Also,
 * they will not work when Pellet is being used, because
 * Pellet doesn't allow predicate variables.
 */
public class PredicateVariablesTest 
{
	public final static Logger log = Logger.getLogger(PredicateVariablesTest.class);

	protected Configuration config = Config.getConfiguration(); 
	
	protected final static String PREDICATE_VARIABLES_CONFIG_KEY = "share.allowPredicateVariables";
	protected boolean allowPredicateVariablesOldValue;
	
	protected StopWatch stopWatch = new StopWatch();
	
	@Before
	public void setUp()
	{
		allowPredicateVariablesOldValue = config.getBoolean(PREDICATE_VARIABLES_CONFIG_KEY, false);
		config.setProperty(PREDICATE_VARIABLES_CONFIG_KEY, true);
	}
	
	@After
	public void tearDown()
	{
		config.setProperty(PREDICATE_VARIABLES_CONFIG_KEY, allowPredicateVariablesOldValue);
	}
	
	protected void testQueryFromFile(String queryFile) 
	{
		testQueryFromFile(queryFile, true);
	}
	
	protected void testQueryFromFile(String queryFile, boolean requireResults)
	{
		try {
			String query = SPARQLStringUtils.readFully(getClass().getResource(queryFile));
			testQuery(query, requireResults);
		} catch(IOException e) {
			fail("error reading query file: \n" + ExceptionUtils.getStackTrace(e));
		}
	}
	
	
	protected void testQuery(String query, boolean requireResults)
	{
		log.info( String.format("executing query\n%s", query) );
		
		stopWatch.start();
		List<Map<String, String>> results = new SHAREQueryClient().synchronousQuery(query);
		stopWatch.stop();
		
		StringBuffer buf = new StringBuffer("query finished in ");
		buf.append( DurationFormatUtils.formatDurationHMS(stopWatch.getTime()) );
		if (results.isEmpty())
			buf.append("\nno results");
		else
			for (Map<String, String> binding: results) {
				buf.append("\n");
				buf.append(binding);
			}
		log.info( buf.toString() );
		
		if(requireResults)
			assertFalse(String.format("query \"%s\" returned no results", query), results.isEmpty());
	}

	/* 
	 * These tests are disabled because they are currently very 
	 * slow and consume a huge amount of memory. 
	 */

	@Test
	public void testQuery1()
	{
		testQueryFromFile("predicate.variable.query1.sparql");
	}
	@Test
	public void testQuery2()
	{
		testQueryFromFile("predicate.variable.query2.sparql");
	}
	@Test
	public void testQuery3()
	{
		testQueryFromFile("predicate.variable.query3.sparql");
	}
	@Test
	public void testQuery4()
	{
		testQueryFromFile("predicate.variable.query4.sparql");
	}
	@Test
	public void testQuery5()
	{
		testQueryFromFile("predicate.variable.query5.sparql");
	}
	@Test
	public void testQuery6()
	{
		testQueryFromFile("predicate.variable.query6.sparql");
	}
	@Test
	public void testQuery7()
	{
		/* This query is *supposed* to have no results.
		 * The purpose of the test is to check for graceful
		 * failure in the case where a predicate variable 
		 * has only literal or blank-node bindings.
		 */
		testQueryFromFile("predicate.variable.query7.sparql", false);
	}
	
}
