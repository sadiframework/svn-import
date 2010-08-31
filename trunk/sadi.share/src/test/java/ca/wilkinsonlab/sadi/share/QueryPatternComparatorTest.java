package ca.wilkinsonlab.sadi.share;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.client.MultiRegistry;
import ca.wilkinsonlab.sadi.client.Registry;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceBase;
import ca.wilkinsonlab.sadi.client.ServiceInvocationException;
import ca.wilkinsonlab.sadi.stats.PredicateStatsDB;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import org.apache.log4j.Logger;

/*
 * These tests are for QueryPatternComparator, an inner 
 * class of SHAREKnowledgeBase which performs cost 
 * estimations/comparisons of triple patterns 
 * (for query optimization).
 * 
 * QueryPatternComparator accesses the internal
 * variable bindings and reasoningModel of SHAREKnowledgeBase, 
 * so mock objects are included here to simulate these.
 */

public class QueryPatternComparatorTest 
{
	protected static final Logger log = Logger.getLogger(QueryPatternComparatorTest.class);
	
	protected static final String NODE_PREFIX = "http://dummynode/";
	protected static final String PREDICATE_PREFIX = "http://dummypredicate/";

	// predicate A is equivalent to predicate C
	
	protected static final String INVERSE_SUFFIX = "-inverse";
	
	protected static final String PREDICATE_A = PREDICATE_PREFIX + "A";
	protected static final String PREDICATE_A_INVERSE = PREDICATE_A + INVERSE_SUFFIX;
	protected static final String PREDICATE_B = PREDICATE_PREFIX + "B";
	protected static final String PREDICATE_B_INVERSE = PREDICATE_B + INVERSE_SUFFIX;
	protected static final String PREDICATE_C = PREDICATE_PREFIX + "C";
	protected static final String PREDICATE_C_INVERSE = PREDICATE_C + INVERSE_SUFFIX;
	
	protected static final String NO_STATS_PREDICATE_D = PREDICATE_PREFIX + "D"; 
	protected static final String NO_STATS_PREDICATE_D_INVERSE = NO_STATS_PREDICATE_D + INVERSE_SUFFIX;
	protected static final String NO_STATS_PREDICATE_E = PREDICATE_PREFIX + "E"; 
	protected static final String NO_STATS_PREDICATE_E_INVERSE = NO_STATS_PREDICATE_E + INVERSE_SUFFIX;
	
	protected static final String UNRESOLVABLE_PREDICATE_F = PREDICATE_PREFIX + "F";
	protected static final String PREDICATE_F_INVERSE = UNRESOLVABLE_PREDICATE_F + INVERSE_SUFFIX;
	
	protected static final String NODE_A = NODE_PREFIX + "A";
	protected static final String NODE_B = NODE_PREFIX + "B";
	protected static final String NODE_C = NODE_PREFIX + "C";
	protected static final String NODE_D = NODE_PREFIX + "D";
	
	protected static MockKnowledgeBase mockKB;
	
	public QueryPatternComparatorTest() throws IOException
	{
		OntModel reasoningModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
		
		OntProperty pA = reasoningModel.createOntProperty(PREDICATE_A);
		OntProperty pB = reasoningModel.createOntProperty(PREDICATE_B);
		OntProperty pC = reasoningModel.createOntProperty(PREDICATE_C);
		OntProperty pD = reasoningModel.createOntProperty(NO_STATS_PREDICATE_D);
		OntProperty pE = reasoningModel.createOntProperty(NO_STATS_PREDICATE_E);
		OntProperty pF = reasoningModel.createOntProperty(UNRESOLVABLE_PREDICATE_F);
		
		pA.addInverseOf(reasoningModel.createOntProperty(PREDICATE_A_INVERSE));
		pB.addInverseOf(reasoningModel.createOntProperty(PREDICATE_B_INVERSE));
		pC.addInverseOf(reasoningModel.createOntProperty(PREDICATE_C_INVERSE));
		pD.addInverseOf(reasoningModel.createOntProperty(NO_STATS_PREDICATE_D_INVERSE));
		pE.addInverseOf(reasoningModel.createOntProperty(NO_STATS_PREDICATE_E_INVERSE));
		pF.addInverseOf(reasoningModel.createOntProperty(PREDICATE_F_INVERSE));
		
		pA.addEquivalentProperty(pC);
		
		Resource rA = reasoningModel.createResource(NODE_A);
		Resource rB = reasoningModel.createResource(NODE_B);
		reasoningModel.createResource(NODE_C);

		reasoningModel.add(rA, pB, pA);
		reasoningModel.add(rB, pB, pB);
		reasoningModel.add(rA, pB, pC);
		
		mockKB = new MockKnowledgeBase(reasoningModel);
		
		/* we need predicate variables for our tests */
		mockKB.setAllowPredicateVariables(true);
		
		/* assign bindings to some variables, without accessing the internals of SHAREKnowledgeBase */
		try {
			mockKB.executeQuery(SPARQLStringUtils.strFromTemplate("SELECT * WHERE { ?var1 %u% ?var2 }", PREDICATE_B));
		} catch (Exception e) {
			log.error("error executing query", e);
		}
	}
	
	@Test
	public void testCompareWithStats()
	{
		/* stats say pattern1 is more expensive */
		Triple pattern1 = createTriple(NODE_A, PREDICATE_A, "?unbound");
		Triple pattern2 = createTriple("?unbound", PREDICATE_B, NODE_B);
		assertTrue(mockKB.comparator.compare(pattern1, pattern2) == 1);
	}
	
	@Test
	public void testCompareWithoutStats1()
	{

		/*
		 * these patterns will be compared by the number of 
		 * subject bindings, because no stats are available for
		 * NO_STATS_PREDICATE_E.  ?var2 has more bindings than
		 * ?var1.
		 */
		Triple pattern1 = createTriple("?var1", NO_STATS_PREDICATE_D, "?unbound");
		Triple pattern2 = createTriple("?var2", NO_STATS_PREDICATE_D, "?unbound");
		
		assertTrue(mockKB.comparator.compare(pattern1, pattern2) == -1);
	}
	
	@Test
	public void testCompareWithoutStats2()
	{
		/* 
		 * no stats are available for either predicate, and the patterns
		 * both have a single subject/object binding.  Thus the
		 * patterns should be considered equal.
		 */
		Triple pattern1 = createTriple(NODE_A, NO_STATS_PREDICATE_D, "?unbound");
		Triple pattern2 = createTriple("?unbound", NO_STATS_PREDICATE_E, NODE_B);
		assertTrue(mockKB.comparator.compare(pattern1, pattern2) == 0);
	}

	@Test
	public void testCompareWithoutStats3()
	{
		/* stats are only available for pattern1, so the patterns should instead 
		 * be compared by the number of inputs, and determined to be equal */
		Triple pattern1 = createTriple(NODE_A, PREDICATE_A, "?unbound");
		Triple pattern2 = createTriple("?unbound", NO_STATS_PREDICATE_E, NODE_B);
		assertTrue(mockKB.comparator.compare(pattern1, pattern2) == 0);
	}
	
	@Test
	public void testCompareWithUnboundPredicate()
	{
		/* 
		 * a pattern with an unbound predicate should be considered
		 * worse than a pattern with a bound predicate
		 */
		Triple pattern1 = createTriple(NODE_A, "?unbound", "?unbound");
		Triple pattern2 = createTriple(NODE_A, PREDICATE_A, "?unbound");
		assertTrue(mockKB.comparator.compare(pattern1, pattern2) == 1);
	}

	@Test
	public void testCompareWithUnresolvablePattern()
	{
		Triple pattern1 = createTriple("?unbound", "?unbound", "?unbound");
		Triple pattern2 = createTriple(NODE_A, PREDICATE_A, "?unbound");
		assertTrue(mockKB.comparator.compare(pattern1, pattern2) == 1);
	}

	@Test
	public void testCompareWithUnresolvablePattern2()
	{
		Triple pattern1 = createTriple(NODE_A, UNRESOLVABLE_PREDICATE_F, "?unbound");
		Triple pattern2 = createTriple(NODE_A, PREDICATE_A, "?unbound");
		assertTrue(mockKB.comparator.compare(pattern1, pattern2) == 1);
	}
	
	@Test
	public void testCompareWithUnresolvablePattern3()
	{
		/*
		 * At the current time, all unresolvable patterns are considered equal.
		 */
		Triple pattern1 = createTriple(NODE_A, UNRESOLVABLE_PREDICATE_F, "?unbound");
		Triple pattern2 = createTriple("?unbound", PREDICATE_A, "?unbound");
		assertTrue(mockKB.comparator.compare(pattern1, pattern2) == 0);
	}

	@Test
	public void testCompareWithUnresolvablePattern4()
	{
		/*
		 * An expensive pattern should be preferred to an unresolvable pattern.
		 */
		Triple pattern1 = createTriple(NODE_A, "?unbound", "?unbound");
		Triple pattern2 = createTriple("?unbound", PREDICATE_A, "?unbound");
		assertTrue(mockKB.comparator.compare(pattern1, pattern2) == -1);
	}
	
	@Test
	public void testBestDirectionIsForwardWithStats()
	{
		/*
		 * according to stats, PREDICATE_A is cheaper
		 * in the reverse direction
		 */
		Triple pattern = createTriple("?var1", PREDICATE_A, "?var1");
		assertFalse(mockKB.comparator.bestDirectionIsForward(pattern));
	}
	
	@Test
	public void testBestDirectionIsForwardWithoutStats()
	{
		/*
		 * since the predicate is unbound, the system
		 * should compare the number of bindings of the
		 * subject and object (and decide that reverse
		 * is best).  
		 */
		Triple pattern = createTriple("?var2", "?unbound", "?var1");
		assertFalse(mockKB.comparator.bestDirectionIsForward(pattern));
	}
	
	@Test
	public void testBestDirectionIsForwardWithUnresolvablePredicate()
	{
		/* predicate F is only resolvable in the reverse direction */
		Triple pattern = createTriple("?var1", UNRESOLVABLE_PREDICATE_F, "?var2");
		assertFalse(mockKB.comparator.bestDirectionIsForward(pattern));
	}
	
	@Test
	public void testCostWithMultipleSubjects()
	{
		/*
		 * Subject variable is bound to:
		 * 
		 * NODE_A, NODE_B, NODE_C
		 * 
		 * and cost(PREDICATE_A) = 2 + (2 * numInputs) 
		 * 
		 * Therefore, cost = 2 + (2 * 2) = 6 
		 */
		Triple pattern = createTriple("?var1", PREDICATE_A, "?unbound");
		assertTrue(mockKB.comparator.costByStats(pattern) == 6);
	}
	
	@Test
	public void testCostWithMultiplePredicates()
	{
		/* 
		 * The object of the pattern has 1 binding, and the
		 * predicate has 3 bindings:
		 * 
		 * PREDICATE_A, PREDICATE_B, PREDICATE_C
		 * 
		 * A and C are equivalent, so their cost should only
		 * be counted once.
		 * 
		 * cost(A) = cost(C) = 2,
		 * cost(B) = 2
		 * 
		 * Answer should be cost(A) + cost(B) = 4.
		 */
		Triple pattern = createTriple("?unbound", "?var2", NODE_B);
		assertTrue(mockKB.comparator.costByStats(pattern) == 4);
	}
	
	protected static Triple createTriple(String s, String p, String o) 
	{
		return new Triple(NodeCreateUtils.create(s),
					NodeCreateUtils.create(p),
					NodeCreateUtils.create(o));
	}

	protected class MockKnowledgeBase extends SHAREKnowledgeBase
	{
		private MultiRegistry mockRegistry;
		private PredicateStatsDB mockStatsDB;
		
		public QueryPatternComparator comparator;
		
		public MockKnowledgeBase(OntModel reasoningModel) throws IOException
		{
			super(reasoningModel, true);
			this.mockStatsDB = new MockPredicateStatsDB();
			this.mockRegistry = new MockMultiRegistry();
			this.comparator = new QueryPatternComparator();
		}
		
		@Override 
		protected MultiRegistry getRegistry() {
			return mockRegistry;
		}
		
		@Override
		protected PredicateStatsDB getStatsDB() {
			return mockStatsDB;
		}
		
	}

	/**
	 * A mock version of the MultiRegistry.
	 * 
	 * The QueryPatternComparator must take into account whether
	 * a given predicate is resolvable (i.e. maps to any services)
	 * in the forward and/or reverse direction, and it uses
	 * the MultiRegistry to determine this. 
	 * 
	 * Thus we need a fake implementation of "findServicesByPredicate".
	 * (All the other methods of Registry are unused.) 
	 * 
	 * I could define this class more briefly as an extension of
	 * RegistryBase, but I think it's better if this object doesn't have any
	 * "real" code in it. 
	 */
	protected class MockMultiRegistry extends MultiRegistry
	{
		Map<String, Boolean> isResolvable; 
		
		public MockMultiRegistry() throws IOException
		{
			super(new ArrayList<Registry>());
			
			isResolvable = new HashMap<String, Boolean>();

			isResolvable.put(PREDICATE_A, true);
			isResolvable.put(PREDICATE_A_INVERSE, true);
			isResolvable.put(PREDICATE_B, true);
			isResolvable.put(PREDICATE_B_INVERSE, true);
			isResolvable.put(PREDICATE_C, true);
			isResolvable.put(PREDICATE_C_INVERSE, true);
			isResolvable.put(NO_STATS_PREDICATE_D, true);
			isResolvable.put(NO_STATS_PREDICATE_D_INVERSE, true);
			isResolvable.put(NO_STATS_PREDICATE_E, true);
			isResolvable.put(NO_STATS_PREDICATE_E_INVERSE, true);
			isResolvable.put(UNRESOLVABLE_PREDICATE_F, false);
			isResolvable.put(PREDICATE_F_INVERSE, true);
		}
		
		@Override
		public Collection<Service> findServicesByPredicate(String predicate) 
		{
			if(isResolvable.containsKey(predicate) && (isResolvable.get(predicate) == true)) {
				return Collections.singleton((Service)(new DummyService()));
			}
			
			return new ArrayList<Service>();
		}

		protected class DummyService extends ServiceBase {

			@Override
			public String getDescription() {
				throw new UnsupportedOperationException();
			}

			@Override
			public String getInputClassURI() {
				throw new UnsupportedOperationException();
			}

			@Override
			public String getName() {
				throw new UnsupportedOperationException();
			}

			@Override
			public String getOutputClassURI() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Collection<Triple> invokeService(Collection<Resource> inputNodes) throws ServiceInvocationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean isInputInstance(Resource resource) {
				throw new UnsupportedOperationException();
			}
			
		}

	}
	
	protected class MockPredicateStatsDB extends PredicateStatsDB
	{
		protected Map<String, Integer> baseTimeForward;
		protected Map<String, Integer> timePerInputForward;
		protected Map<String, Integer> baseTimeReverse;
		protected Map<String, Integer> timePerInputReverse;
		
		public MockPredicateStatsDB() throws IOException 
		{
			super("http://dev.biordf.net/sparql", null, null);
				
			baseTimeForward = new HashMap<String, Integer>();
			
			baseTimeForward.put(PREDICATE_A, 2);
			baseTimeForward.put(PREDICATE_C, 2);
			
			timePerInputForward = new HashMap<String, Integer>(); 
			
			timePerInputForward.put(PREDICATE_A, 2);
			timePerInputForward.put(PREDICATE_C, 2);
			
			baseTimeReverse = new HashMap<String, Integer>(); 

			baseTimeReverse.put(PREDICATE_A, 1);
			baseTimeReverse.put(PREDICATE_B, 1);
			baseTimeReverse.put(PREDICATE_C, 1);

			timePerInputReverse = new HashMap<String, Integer>(); 

			timePerInputReverse.put(PREDICATE_A, 1);
			timePerInputReverse.put(PREDICATE_B, 1);
			timePerInputReverse.put(PREDICATE_C, 1);
		
		}
		
		@Override
		public int getEstimatedTime(Property p, boolean directionIsForward, int numInputs) 
		{
			String uri = p.getURI();
			
			Map<String, Integer> baseTimeMap = directionIsForward ? baseTimeForward : baseTimeReverse;
			Map<String, Integer> timePerInputMap = directionIsForward ? timePerInputForward : timePerInputReverse;
			
			if(!baseTimeMap.containsKey(uri) || !timePerInputMap.containsKey(uri)) {
				return PredicateStatsDB.NO_STATS_AVAILABLE;
			}
			
			return baseTimeMap.get(uri) + (numInputs * timePerInputMap.get(uri));
		}
	}
}
