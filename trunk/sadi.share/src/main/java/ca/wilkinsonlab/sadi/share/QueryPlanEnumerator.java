package ca.wilkinsonlab.sadi.share;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.utils.PropertyResolvabilityCache;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntProperty;

/**
 * A class that enumerates all orderings of a SPARQL query that are
 * resolvable by a SHAREKnowledgeBase.
 * 
 * @author Ben Vandervalk
  */
public class QueryPlanEnumerator 
{
	private PropertyResolvabilityCache resolvabilityCache;
	private SHAREKnowledgeBase kb;
	
	public QueryPlanEnumerator(SHAREKnowledgeBase kb)
	{
		this.kb = kb;
		resolvabilityCache = new PropertyResolvabilityCache(Config.getConfiguration().getMasterRegistry());
	}
	
	protected PropertyResolvabilityCache getResolvabilityCache()
	{
		return resolvabilityCache; 
	}
	
	/**
	 * <p>Return a list of all orderings of the given query that are resolvable by 
	 * SHAREKnowledgeBase.</p>
	 * 
	 * <p>The caller should be aware that this method may "invert" the given triple patterns in addition to 
	 * reordering them, and that this inversion has significance.  The following paragraph explains 
	 * why.</p>
	 * 
	 * <p>During query execution, a triple pattern may have both a bound subject and a bound object prior 
	 * to being resolved, and such a pattern can be resolved in either the forward or reverse direction.
	 * If adaptive query planning is off, SHAREKnowledgeBase will always resolve such patterns in 
	 * the forward (left-to-right) direction.  In order to force such a pattern to be resolved in 
	 * the reverse direction, getAllResolvableQueryPlans() inverts the triple (i.e. swaps the subject
	 * and object, and replaces the predicate with its inverse.)</p>
	 * 
	 * <p>This method of forcing a pattern to resolved in the reverse direction works fine, except
	 * for one flaw: In cases where the object of the triple pattern is a literal constant, the
	 * triple pattern cannot be inverted, because SPARQL syntax does not allow a literal constant
	 * in the subject position of a pattern.  getAllResolvableQueryPlans() omits the query plans
	 * that require such an inversion.</p>
	 * 
	 * @param query a list of triples representing the input query
	 * @returns a list of all query orderings that are resolvable by SHAREKnowledgeBase (barring
	 * the exceptions described above) 
	 */
	public List<List<Triple>> getAllResolvableQueryPlans(List<Triple> query)
	{
		Set<Node> boundVars = new HashSet<Node>();
		return getAllResolvableQueryPlans(query, boundVars); 
	}

	protected List<List<Triple>> getAllResolvableQueryPlans(List<Triple> remainingPatterns, Set<Node> boundVars) 
	{
		List<List<Triple>> queryPlans = new ArrayList<List<Triple>>();
		
		for(Triple pattern : remainingPatterns) {
			
			Node s = pattern.getSubject();
			Node p = pattern.getPredicate();
			Node o = pattern.getObject();
			
			boolean sIsUnboundVar = s.isVariable() && !boundVars.contains(s); 
    		boolean oIsUnboundVar = o.isVariable() && !boundVars.contains(o);			
    		
    		if(!sIsUnboundVar || !oIsUnboundVar) {

    			OntProperty property = kb.getOntProperty(p.getURI());
				OntProperty inverseProperty = kb.getInverseProperty(property);
				
				boolean patternIsForwardResolvable = (!sIsUnboundVar && getResolvabilityCache().isResolvable(property));
				boolean patternIsReverseResolvable = (!oIsUnboundVar && getResolvabilityCache().isResolvable(inverseProperty));
				
    			if(!(patternIsForwardResolvable || patternIsReverseResolvable)) {
    				continue;
    			}

    			Set<Node> boundVarsCopy = new HashSet<Node>(boundVars);

    			if(sIsUnboundVar) {
    				boundVarsCopy.add(s);
    			}
    			if(oIsUnboundVar) {
    				boundVarsCopy.add(o);
    			}

    			List<Triple> remainingPatternsCopy = new ArrayList<Triple>(remainingPatterns.size() - 1);
    		
    			for(Triple pattern2 : remainingPatterns) {
    				if(!pattern2.equals(pattern)) {
    					remainingPatternsCopy.add(pattern2);
    				}
    			}

    			List<List<Triple>> queryPlansTail;

    			if(remainingPatterns.size() == 1) {
    			
        			/*
        			 * Handle the base case for the recursion by setting 
        			 * the remaining part of the query plan to an empty Triple
        			 * list.
        			 */
    				queryPlansTail = new ArrayList<List<Triple>>();
    				queryPlansTail.add(new ArrayList<Triple>());
    			
    			} else {
    				
    				queryPlansTail = getAllResolvableQueryPlans(remainingPatternsCopy, boundVarsCopy);
    			
    			}
    			
    			for(List<Triple> queryPlanTail : queryPlansTail) {
    				
    				
    				if(patternIsForwardResolvable || (sIsUnboundVar && patternIsReverseResolvable)) {

    					List<Triple> queryPlan = new ArrayList<Triple>(queryPlanTail.size() + 1);

    					queryPlan.add(pattern);
    					queryPlan.addAll(queryPlanTail);

    					queryPlans.add(queryPlan);
    				
    				}
    				
    				if(patternIsReverseResolvable  && !sIsUnboundVar && !oIsUnboundVar) {
    					
    					/* 
    					 * If both s and o are both bound, the query plan can either resolve
    					 * the pattern in the forward direction or the reverse direction.
    					 * Each case constitutes a different query plan.
    					 * 
    					 * The query engine always evaluates such patterns from left to
    					 * right, and so we force the reverse direction by inverting
    					 * the predicate and swapping the subject and object.
    					 * 
    					 * The one flaw in this approach is that patterns with a
    					 * a literal constant in the object position cannot be
    					 * resolved in the reverse direction, because SPARQL syntax
    					 * does not permit a literal constant in the subject position.
    					 * Query plans that require this are omitted.  
    					 */
    					
    					if(o.isLiteral()) {
    						continue;
    					}
    					
    					List<Triple> queryPlan = new ArrayList<Triple>(queryPlanTail.size() + 1);
    					Triple invertedPattern = new Triple(o, NodeCreateUtils.create(inverseProperty.getURI()), s);

    					queryPlan = new ArrayList<Triple>(queryPlanTail.size() + 1);
    					queryPlan.add(invertedPattern);
    					queryPlan.addAll(queryPlanTail);
    					
    					queryPlans.add(queryPlan);

    				}
    				
    			}
    			
    		}
		}
		
		return queryPlans;
	}
	
}
