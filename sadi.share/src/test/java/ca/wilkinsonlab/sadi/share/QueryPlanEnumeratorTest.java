package ca.wilkinsonlab.sadi.share;

import static org.junit.Assert.assertTrue;

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

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;

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
	
	/*
	 * PREFIX property: <http://test.property/>
	 * PREFIX node: <http://test.node/>
	 * SELECT * 
	 * WHERE {
	 *		node:A property:A ?var .
	 *		?var property:B node:B .		
	 * }
	 */
	private List<Triple> testQuery;
	
	private Triple testQueryPattern1;
	private Triple testQueryPattern1Inverted;
	private Triple testQueryPattern2;
	private Triple testQueryPattern2Inverted;
	
	private SHAREKnowledgeBase kb;
	
	public QueryPlanEnumeratorTest()
	{
		
		kb = new SHAREKnowledgeBase(ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM), true);
		
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
		
		testQuery = new ArrayList<Triple>();
		testQuery.add(testQueryPattern1);
		testQuery.add(testQueryPattern2);
		
	}
	
	@Test
	public void testEnumeratorAllPropertiesResolvable()
	{
		
		List<List<Triple>> expectedQueryPlans = new ArrayList<List<Triple>>(4);

		List<Triple> queryPlan1 = new ArrayList<Triple>(2);
		queryPlan1.add(testQueryPattern1);
		queryPlan1.add(testQueryPattern2);

		List<Triple> queryPlan2 = new ArrayList<Triple>(2);
		queryPlan2.add(testQueryPattern1);
		queryPlan2.add(testQueryPattern2Inverted);

		List<Triple> queryPlan3 = new ArrayList<Triple>(2);
		queryPlan3.add(testQueryPattern2);
		queryPlan3.add(testQueryPattern1);
		
		List<Triple> queryPlan4 = new ArrayList<Triple>(2);
		queryPlan4.add(testQueryPattern2);
		queryPlan4.add(testQueryPattern1Inverted);
		
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

		assertTrue(planListsAreEqual(enumerator.getAllResolvableQueryPlans(testQuery), expectedQueryPlans));
		
	}
	
	@Test
	public void testEnumeratorWithUnidirectionalProperty()
	{
		
		List<List<Triple>> expectedQueryPlans = new ArrayList<List<Triple>>(4);

		List<Triple> queryPlan1 = new ArrayList<Triple>(2);
		queryPlan1.add(testQueryPattern1);
		queryPlan1.add(testQueryPattern2Inverted);

		List<Triple> queryPlan2 = new ArrayList<Triple>(2);
		queryPlan2.add(testQueryPattern2);
		queryPlan2.add(testQueryPattern1);
		
		List<Triple> queryPlan3 = new ArrayList<Triple>(2);
		queryPlan3.add(testQueryPattern2);
		queryPlan3.add(testQueryPattern1Inverted);

		expectedQueryPlans.add(queryPlan1);
		expectedQueryPlans.add(queryPlan2);
		expectedQueryPlans.add(queryPlan3);

		List<OntProperty> resolvableProperties = new ArrayList<OntProperty>();
		resolvableProperties.add(propertyA);
		resolvableProperties.add(propertyAInverse);
		resolvableProperties.add(propertyBInverse);
		
		QueryPlanEnumerator enumerator = new QueryPlanEnumeratorWithMockPropertyResolution(resolvableProperties);

		assertTrue(planListsAreEqual(enumerator.getAllResolvableQueryPlans(testQuery), expectedQueryPlans));
		
	}
	
	protected static boolean planListsAreEqual(List<List<Triple>> queryPlans1, List<List<Triple>> queryPlans2)
	{
		return new HashBag(queryPlans1).equals(new HashBag(queryPlans2));
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
