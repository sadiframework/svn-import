package ca.wilkinsonlab.sadi.optimizer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import ca.wilkinsonlab.sadi.optimizer.statistics.PredicateStatsDBAdmin;
import ca.wilkinsonlab.sadi.pellet.DynamicKnowledgeBase;
import ca.wilkinsonlab.sadi.utils.PredicateUtils;

import static org.junit.Assert.*;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryEngine;
import org.mindswap.pellet.query.impl.ARQParser;
import org.mindswap.pellet.utils.ATermUtils;

/**
 * Test a static query optimizer using a mock query,
 * composed of mock predicates with associated mock statistics.
 * For a graphical representation of the test query see
 * testquery.png, in ca.wilkinsonlab.sadi.optimizer.resources.
 *
 * @author Ben Vandervalk
 */
public abstract class StaticOptimizerTest {

	protected static PredicateStatsDBAdmin statsdb; 
	protected static final TestPredicate[] testPredicates = 
		{ 
			// args: uri, forward selectivity, reverse selectivity, forward time, reverse time
			// -1 means not applicable
			new TestPredicate("pred:a", 5, -1, 1000, -1),
			new TestPredicate("pred:b", 4, 3, 1500, 1500),
			new TestPredicate("pred:c", 3, -1, 2000, -1),
			new TestPredicate("pred:d", 2, -1, 1000, -1),
			new TestPredicate("pred:e", 1, 4, 500, 2000),
			new TestPredicate("pred:f", 4, 4, 1000, 1000),
			new TestPredicate("pred:g", 4, -1, 3000, -1),
		};
	
	protected static Set<String> hasInverse = new HashSet<String>();
	protected static KnowledgeBase testKB = new TestDynamicKnowledgeBase();
	
	protected static final String testQueryString = 
		"SELECT *\n" +
		"WHERE {\n" +
		"   <const:a> <pred:a> ?var1 .\n" +
		"   <const:a> <pred:b> ?var2 .\n" +
		"   ?var1 <pred:f> ?var4 .\n" +
		"   ?var1 <pred:c> ?var3 .\n" +
		"   ?var2 <pred:d> ?var1 .\n" +
		"   ?var3 <pred:e> ?var2 .\n" +
		"   ?var4 <pred:g> ?var3 .\n" +
		"}";
		
	protected static Query testQuery;
	
	@BeforeClass 
	public static void oneTimeSetup()
	{
		try {
			statsdb = new PredicateStatsDBAdmin();
			initPredicates();
			insertStatsForTestPredicates();
			initTestQuery();
		}
		catch(Exception e) {
			fail("Failed to initialize predicate stats db: " + e);
		}
	}
	
	@AfterClass
	public static void oneTimeTearDown()
	{
		try {
			removeStatsForTestPredicates();
		}
		catch(Exception e) {
			fail("Failed to remove stats for test predicates");
		}
	}
	
	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {
	}
	
	public static void initPredicates()
	{
		for(int i = 0; i < testPredicates.length; i++)
			testKB.addObjectProperty(ATermUtils.makeTermAppl(testPredicates[i].uri));

		hasInverse.add("pred:b");
		hasInverse.add("pred:e");
		hasInverse.add("pred:f");
	}

	public static void initTestQuery() throws IOException
	{
        ARQParser parser = (ARQParser) QueryEngine.createParser();
	    testQuery = parser.parse(IOUtils.toInputStream(testQueryString), testKB);
	}
	
	public static void insertStatsForTestPredicates() throws IOException
	{
		for(int i = 0; i < testPredicates.length; i++) {
			TestPredicate p = testPredicates[i];
			statsdb.recordSelectivitySample(p.uri, p.forwardSelectivity, true);
			statsdb.recordSelectivitySample(p.uri, p.reverseSelectivity, false);
			statsdb.recordTimeSample(p.uri, p.forwardTime, true);
			statsdb.recordTimeSample(p.uri, p.reverseTime, false);
		}
		
	}
	
	public static void removeStatsForTestPredicates() throws IOException
	{
		for(int i = 0; i < testPredicates.length; i++) {
			TestPredicate p = testPredicates[i];
			statsdb.removeStatsForPredicate(p.uri);
		}
	}
	
	private static class TestPredicate
	{
		String uri;
		long forwardSelectivity;
		long reverseSelectivity;
		int forwardTime;
		int reverseTime;
		
		public TestPredicate(String uri, 
				long forwardSelectivity,
				long reverseSelectivity,
				int forwardTime,
				int reverseTime)
		{
			this.uri = uri;
			this.forwardSelectivity = forwardSelectivity;
			this.reverseSelectivity = reverseSelectivity;
			this.forwardTime = forwardTime;
			this.reverseTime = reverseTime;
		}
	}
	
	private static class TestDynamicKnowledgeBase extends DynamicKnowledgeBase
	{
		@Override
		public boolean isResolvable(String predicate)
		{	
			boolean isInverted = false;
			if(PredicateUtils.isInverted(predicate)) {
				predicate = PredicateUtils.invert(predicate);
				isInverted = true;
			}
			
			for(int i = 0; i < testPredicates.length; i++) {
				TestPredicate p = testPredicates[i];
				if(!isInverted && predicate.equals(p.uri))
					return true;
				else if(isInverted && predicate.equals(p.uri) && hasInverse.contains(p.uri))
					return true;
			}
			return false;
		}
		
	}
}
