package ca.wilkinsonlab.sadi.share;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.bag.HashBag;
import org.apache.log4j.Logger;
import org.junit.Test;

import ca.wilkinsonlab.sadi.share.QueryPlanEnumerator;
import ca.wilkinsonlab.sadi.utils.PropertyResolvabilityCache;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;

public class QueryPlanEnumeratorTest 
{
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(QueryPlanEnumeratorTest.class);
	
	private static final String PROPERTY_PREFIX = "http://test.property/";
	private static final String NODE_PREFIX = "http://test.node/";
	
	private static final String PROPERTY_A_URI = PROPERTY_PREFIX + "A";
	private static OntProperty propertyA;
	private static OntProperty propertyAInverse;
	
	private static final String PROPERTY_B_URI = PROPERTY_PREFIX + "B";
	private static OntProperty propertyB;
	private static OntProperty propertyBInverse;
	
	private static final String NODE_A_URI = NODE_PREFIX + "A";
	private static final String NODE_B_URI = NODE_PREFIX + "B";
	
	private String testQuery;
	
	/* there are four possible plans for the test query */
	private String queryPlan1;
	private String queryPlan2;
	private String queryPlan3;
	private String queryPlan4;
	
	private List<Triple> testQueryAsTriplesBlock;
	
	private Triple testQueryPattern1;
	private Triple testQueryPattern1Inverted;
	private Triple testQueryPattern2;
	private Triple testQueryPattern2Inverted;
	
	private SHAREKnowledgeBase kb;
	
	public QueryPlanEnumeratorTest() throws IOException
	{
		
		testQuery = SPARQLStringUtils.readFully(QueryPlanEnumeratorTest.class.getResource("test.query.for.enumerator.sparql"));
		
		kb = new SHAREKnowledgeBase(ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM), true);
		
		/* 
		 * Note: We get the fake inverse URIs from the KB instead of
		 * hardcoding them, in case the URI form (e.g. "-inverse") changes
		 * in the future.
		 */
		
		propertyA = kb.getOntProperty(PROPERTY_A_URI);
		propertyAInverse = kb.getInverseProperty(propertyA);
		String propertyAInverseUri = propertyAInverse.getURI(); 
		
		propertyB = kb.getOntProperty(PROPERTY_B_URI);
		propertyBInverse = kb.getInverseProperty(propertyB);
		String propertyBInverseUri = propertyBInverse.getURI();
		
		testQueryPattern1 = new Triple(
				NodeCreateUtils.create(NODE_A_URI),
				NodeCreateUtils.create(PROPERTY_A_URI),
				NodeCreateUtils.create("?var"));

		testQueryPattern1Inverted = new Triple(
				NodeCreateUtils.create("?var"),
				NodeCreateUtils.create(propertyAInverseUri),
				NodeCreateUtils.create(NODE_A_URI));
		
		testQueryPattern2 = new Triple(
				NodeCreateUtils.create("?var"),
				NodeCreateUtils.create(PROPERTY_B_URI),
				NodeCreateUtils.create(NODE_B_URI));
		
		testQueryPattern2Inverted = new Triple(
				NodeCreateUtils.create(NODE_B_URI),
				NodeCreateUtils.create(propertyBInverseUri),
				NodeCreateUtils.create("?var"));
		
		testQueryAsTriplesBlock = new ArrayList<Triple>();
		testQueryAsTriplesBlock.add(testQueryPattern1);
		testQueryAsTriplesBlock.add(testQueryPattern2);
		
		List<Triple> plan1 = new ArrayList<Triple>(2);
		plan1.add(testQueryPattern1);
		plan1.add(testQueryPattern2);
		queryPlan1 = getSelectStarQuery(plan1);

		List<Triple> plan2 = new ArrayList<Triple>(2);
		plan2.add(testQueryPattern1);
		plan2.add(testQueryPattern2Inverted);
		queryPlan2 = getSelectStarQuery(plan2);

		List<Triple> plan3 = new ArrayList<Triple>(2);
		plan3.add(testQueryPattern2);
		plan3.add(testQueryPattern1);
		queryPlan3 = getSelectStarQuery(plan3);
		
		List<Triple> plan4 = new ArrayList<Triple>(2);
		plan4.add(testQueryPattern2);
		plan4.add(testQueryPattern1Inverted);
		queryPlan4 = getSelectStarQuery(plan4);
		
	}
	
	@Test
	public void testEnumeratorWithQueryString()
	{
		List<String> expectedQueryPlans = new ArrayList<String>(4);

		expectedQueryPlans.add(queryPlan1);
		expectedQueryPlans.add(queryPlan2);
		expectedQueryPlans.add(queryPlan3);
		expectedQueryPlans.add(queryPlan4);

		List<OntProperty> resolvableProperties = new ArrayList<OntProperty>();
		resolvableProperties.add(propertyA);
		resolvableProperties.add(propertyAInverse);
		resolvableProperties.add(propertyB);
		resolvableProperties.add(propertyBInverse);
		
		QueryPlanEnumerator enumerator = new QueryPlanEnumeratorWithMockPropertyResolution(resolvableProperties);
		
		Collection<String> queryPlans = enumerator.getAllResolvableQueryPlans(testQuery);
		assertTrue(queryStringCollectionsAreEqual(queryPlans, expectedQueryPlans));
	}

	@Test
	public void testEnumeratorJenaQuery()
	{
		List<OntProperty> resolvableProperties = new ArrayList<OntProperty>();
		resolvableProperties.add(propertyA);
		resolvableProperties.add(propertyAInverse);
		resolvableProperties.add(propertyB);
		resolvableProperties.add(propertyBInverse);
		
		QueryPlanEnumerator enumerator = new QueryPlanEnumeratorWithMockPropertyResolution(resolvableProperties);
		
		/*
		 * Test that the enumeration works correctly with both 
		 * allowable query syntaxes.
		 */
		Syntax syntaxes[] = { Syntax.syntaxSPARQL, Syntax.syntaxARQ };
		
		for(int i = 0; i < syntaxes.length; i++) {
			
			Syntax syntax = syntaxes[i];
			Query jenaQuery = QueryFactory.create(testQuery, syntax);

			List<Query> expectedQueryPlans = new ArrayList<Query>(4);

			expectedQueryPlans.add(QueryFactory.create(queryPlan1, syntax));
			expectedQueryPlans.add(QueryFactory.create(queryPlan2, syntax));
			expectedQueryPlans.add(QueryFactory.create(queryPlan3, syntax));
			expectedQueryPlans.add(QueryFactory.create(queryPlan4, syntax));

			Collection<Query> queryPlans = enumerator.getAllResolvableQueryPlans(jenaQuery);
			assertTrue(queryCollectionsAreEqual(queryPlans, expectedQueryPlans));
		}
		
	}
	
	@Test
	public void testEnumeratorWithUnidirectionalProperty()
	{
		List<String> expectedQueryPlans = new ArrayList<String>(4);

		expectedQueryPlans.add(queryPlan2);
		expectedQueryPlans.add(queryPlan3);
		expectedQueryPlans.add(queryPlan4);

		List<OntProperty> resolvableProperties = new ArrayList<OntProperty>();
		resolvableProperties.add(propertyA);
		resolvableProperties.add(propertyAInverse);
		resolvableProperties.add(propertyBInverse);
		
		QueryPlanEnumerator enumerator = new QueryPlanEnumeratorWithMockPropertyResolution(resolvableProperties);
		
		Collection<String> queryPlans = enumerator.getAllResolvableQueryPlans(testQuery);
		assertTrue(queryStringCollectionsAreEqual(queryPlans, expectedQueryPlans));
		
	}
	
	protected static boolean queryStringCollectionsAreEqual(Collection<String> queryPlans1, Collection<String> queryPlans2) 
	{
		Collection<Query> jenaQueries1 = new ArrayList<Query>(queryPlans1.size());
		Collection<Query> jenaQueries2 = new ArrayList<Query>(queryPlans2.size());
		
		for(String query : queryPlans1) {
			jenaQueries1.add(QueryFactory.create(query, Syntax.syntaxARQ));
		}
		
		for(String query : queryPlans2) {
			jenaQueries2.add(QueryFactory.create(query, Syntax.syntaxARQ));
		}
		
		return queryCollectionsAreEqual(jenaQueries1, jenaQueries2);
	}
	
	protected static boolean queryCollectionsAreEqual(Collection<Query> queryPlans1, Collection<Query> queryPlans2) 
	{
		/* 
		 * I should be able to use Query.equals() here, but it doesn't seem to work.  
		 * (perhaps because of differences in defined PREFIXes?).  Instead,
		 * I extract the basic graph patterns and do the comparison based on those.
		 */
		
		Collection <List<Triple>> plansAsTriples1 = new ArrayList<List<Triple>>(queryPlans1.size());
		Collection <List<Triple>> plansAsTriples2 = new ArrayList<List<Triple>>(queryPlans2.size());
		
		for(Query query : queryPlans1) {
			plansAsTriples1.add(QueryPlanEnumerator.getBasicGraphPattern(query));
		}
		
		for(Query query : queryPlans2) {
			plansAsTriples2.add(QueryPlanEnumerator.getBasicGraphPattern(query));
		}
		
		return tripleListCollectionsAreEqual(plansAsTriples1, plansAsTriples2);
	}
	
	protected static boolean tripleListCollectionsAreEqual(Collection<List<Triple>> queryPlans1, Collection<List<Triple>> queryPlans2)
	{
		return new HashBag(queryPlans1).equals(new HashBag(queryPlans2));
	}

	protected static String getSelectStarQuery(List<Triple> basicGraphPattern)
	{
        Query query = new Query();
        query.setQuerySelectType();
        query.setSyntax(Syntax.syntaxSPARQL);
        
        ElementTriplesBlock whereClause = new ElementTriplesBlock();
        for(Triple pattern : basicGraphPattern) {
        	whereClause.addTriple(pattern);
        }
        
        query.setQueryPattern(whereClause);       

        // Indicates a "*" in the SELECT clause.
        query.setQueryResultStar(true);

        return query.serialize();		
	}
	
	protected static class QueryPlanEnumeratorWithMockPropertyResolution extends QueryPlanEnumerator
	{
		MockResolvabilityCache mockResolvabilityCache;
		
		public QueryPlanEnumeratorWithMockPropertyResolution(Collection<OntProperty> resolvableProperties)
		{
			super(new SHAREKnowledgeBase(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF), true));
			mockResolvabilityCache = new MockResolvabilityCache(resolvableProperties);
		}
		
		@Override
		protected PropertyResolvabilityCache getResolvabilityCache() {
			return mockResolvabilityCache;
		}
	}
	
	protected static class MockResolvabilityCache extends PropertyResolvabilityCache
	{
		Set<String> resolvablePropertyUris;
		
		public MockResolvabilityCache(Collection<OntProperty> resolvableProperties)
		{
			super(null);
			
			resolvablePropertyUris = new HashSet<String>(resolvableProperties.size());
			for(OntProperty resolvableProperty : resolvableProperties) {
				resolvablePropertyUris.add(resolvableProperty.getURI());
			}
		}
		
		@Override 
		public boolean isResolvable(OntProperty property) {
			return resolvablePropertyUris.contains(property.getURI());
		}
	}
}
