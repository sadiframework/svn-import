package ca.wilkinsonlab.sadi.optimizer;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.query.Query;

import aterm.ATermAppl;
import ca.wilkinsonlab.sadi.optimizer.PrimOptimizer.AdjacencyList;
import ca.wilkinsonlab.sadi.optimizer.PrimOptimizer.EdgeNodePair;
import ca.wilkinsonlab.sadi.pellet.DynamicKnowledgeBase;
import ca.wilkinsonlab.sadi.utils.Pellet2JenaUtils;
import ca.wilkinsonlab.sadi.utils.PredicateUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.vocab.W3C;
import ca.wilkinsonlab.sadi.share.Config;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * <p>Perform static optimization (triple pattern reordering) on a 
 * Pellet query.  The purpose of this class is to maintain a 
 * clean separation between the optimizer code (which uses Jena
 * data structures) and the Pellet code.</p>
 * 
 * <p>The algorithm utilized for optimization is passed
 * in as a StaticOptimizer argument to the constructor.</p>
 *	
 * @author Ben Vandervalk
 */

public class PelletOptimizer {

	public final static Log log = LogFactory.getLog(PelletOptimizer.class);
	
	protected final static String OPTIMIZER_CONFIG_KEY = "share.optimizer";
	
	StaticOptimizer staticOptimizer = null;
	KnowledgeBase kb;
	
	public PelletOptimizer(KnowledgeBase kb) 
	{
		this.kb = kb;
		
		String optimizerClassName = Config.getConfiguration().getString(OPTIMIZER_CONFIG_KEY);
		instantiateOptimizer(optimizerClassName);
	}

	protected StaticOptimizer getStaticOptimizer() { return staticOptimizer; }
	protected void setStaticOptimizer(StaticOptimizer staticOptimizer) { this.staticOptimizer = staticOptimizer; }
	
	private void instantiateOptimizer(String optimizerClassName) 
	{
		if(optimizerClassName != null) {
			try {
				Class clazz = Class.forName(optimizerClassName);
				Constructor constructor = clazz.getConstructor(new Class[] {});
				setStaticOptimizer((StaticOptimizer)constructor.newInstance());
			}
			catch(Exception e) {
				log.warn("unable to instantiate static optimizer", e);
				setStaticOptimizer(null);
			}
		}
	}
	
	public Query optimize(Query query) 
	{
		List<Triple> triples = Pellet2JenaUtils.getTriples(query);
		List<Triple> triplesReordered = null;

		if(getStaticOptimizer() != null) {
			try {
				// Convert info needed for optimization into non-Pellet-dependent classes.
				AdjacencyList adjacencyList = buildAdjacencyList(query, kb); 
				OntModel propertiesModel = buildPropertiesModel(kb);

				// Run the optimizer.
				log.trace("performing static query optimization...");
				triplesReordered = getStaticOptimizer().optimize(triples, propertiesModel, adjacencyList);
			}
			catch(Exception e) {
				log.warn("static optimization failed, falling back to basic reordering", e);
				triplesReordered = reorderForResolutionByWebServices(triples);
			}
		}
		else {
			log.trace("performing basic reordering of query (static optimizer is off)");
			triplesReordered = reorderForResolutionByWebServices(triples);
		}
		
		if(triplesReordered == null)
			throw new RuntimeException("failed to generate reordered query during optimization");

		// Convert the answers back to Pellet form.
		query.getQueryPatterns().clear();
		query.getQueryPatterns().addAll(Pellet2JenaUtils.getQueryPatterns(triplesReordered));

		return query;
	}
	
	/**
	 * Copy the properties of the KnowledgeBase into a Jena model.
	 * Only the type of each property is relevant (i.e. object property
	 * or datatype property) for optimization.  
	 * 
	 * @return a Jena OntModel containing all the properties of the current knowledgebase
	 */
	private static OntModel buildPropertiesModel(KnowledgeBase kb) 
	{
		// OWL_MEM is the most bare-bones option (no inferencing).
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		for(ATermAppl property: kb.getProperties()) {
			if(kb.isDatatypeProperty(property))
				model.createDatatypeProperty(property.toString());
			else
				model.createObjectProperty(property.toString());
		}
		return model;
	}

    /*****************************************************************
     * <p>Reorder the patterns in the query so that each pattern
     * contains at most one unbound variable not present in a
     * previous pattern.</p>
     * 
     * <p>The Pellet query engine will resolve a SPARQL query by
     * attempting to resolve each triple in the WHERE clause one
     * at a time, in the order they are given.</p>
     *    
     * <p>However, in order for a SPARQL query to be resolved by means of 
     * web service calls, there is an additional requirement:</p>
     * 
     * <p>=> Each triple can contain at most one variable that hasn't
     * been solved (bound) by a previous triple.</p>
     * 
     * <p>The purpose of this method is to reorder the triples to
     * satisfy this requirement (if possible).</p>
     * 
     * @param query The query object, containing the list of 
     *              triples to be reordered.
     * @return The same query object, with the reordered triples.
     ****************************************************************/
	public static List<Triple> reorderForResolutionByWebServices(List<Triple> query) 
	{
		List<Triple> patternsRemaining = new LinkedList<Triple>(query); 
		List<Triple> patternsOrdered = new ArrayList<Triple>();
		Set<Node> boundVars = new HashSet<Node>();
		boolean bFoundSolution = true;

		while (!patternsRemaining.isEmpty()) {
			boolean bFoundNextPattern = false;
			for (Iterator<Triple> i = patternsRemaining.iterator(); i.hasNext(); ) {
				Triple pattern = i.next();
				Node s = pattern.getSubject();
				Node o = pattern.getObject();
				boolean bSubjIsUnboundVar = s.isVariable() && !boundVars.contains(s); 
				boolean bObjIsUnboundVar = o.isVariable() && !boundVars.contains(o);
				if (!bSubjIsUnboundVar || !bObjIsUnboundVar) {
					if (bSubjIsUnboundVar)
						boundVars.add(s);
					if (bObjIsUnboundVar)
						boundVars.add(o);
					i.remove();
					patternsOrdered.add(pattern);
					bFoundNextPattern = true;
					break;
				}
			}
			if (!bFoundNextPattern) {
				patternsOrdered.addAll(patternsRemaining);
				bFoundSolution = false;
				break;
			}
		}

		if (!bFoundSolution) {
			log.warn("it is not possible to resolve this query by web services alone.");
		}
		
		log.trace("query after basic reordering: " + SPARQLStringUtils.getSPARQLQueryString(patternsOrdered));
		return patternsOrdered;
	}
	
	/**
	 * Build an adjacent list for the query, indicating the directions
	 * in which each predicate may be resolved.  null is returned if there
	 * are one or more predicates that aren't resolvable to services.
	 * Note that such a predicate does not necessarily indicate an unanswerable 
	 * query; an unresolvable triple pattern may refer to a predicate
	 * in the RDF output of a SADI service which is not directly attached to
	 * the output URI.
	 * 
	 * @param query the query (basic graph pattern) to build the adjacency list from
	 * @param kb the knowledgebase being queried 
	 * @return the adjacency list, or null if one or more of the predicates
	 * in the query is not resolvable to a service.
	 */
	private static AdjacencyList buildAdjacencyList(Query query, KnowledgeBase kb) throws UnresolvablePredicateException
	{
		List<Triple> triples = Pellet2JenaUtils.getTriples(query);
		
		// In a regular Pellet KnowledgeBase, all predicates can be resolved
		// in both directions (from a bound subject to an unbound object, 
		// or from a bound object to an unbound subject).  However, this is
		// not the case with the DynamicKnowledgeBase, since each direction
		// must have an associated service in order to be resolvable.
		
		if(kb instanceof DynamicKnowledgeBase)  {
			
			AdjacencyList adjacencyList = new AdjacencyList(); 
			DynamicKnowledgeBase dynamicKB = (DynamicKnowledgeBase)kb;
			
			for(Triple triple : triples) {
				
				Node s = triple.getSubject();
				Node p = triple.getPredicate();
				Node o = triple.getObject();
				
				String predicate = p.toString();
				boolean predicateIsResolvable = false;
				
				if(dynamicKB.isResolvable(predicate)) { 
					adjacencyList.addNeighbor(s, new EdgeNodePair(p,o,true));
					predicateIsResolvable = true;
				}
								
				// Special case: triple patterns with rdf:type and a defined OWL class
				boolean isClassPattern = predicate.toString().equals(W3C.PREDICATE_RDF_TYPE) && dynamicKB.isClass(Pellet2JenaUtils.getATerm(o));

				if(isClassPattern || dynamicKB.isResolvable(PredicateUtils.invert(predicate))) {
					adjacencyList.addNeighbor(o, new EdgeNodePair(p,s,false));
					predicateIsResolvable = true;
				}
				
				if(!predicateIsResolvable)
					throw new UnresolvablePredicateException("The predicate " + predicate + " does not resolve to any services");
			}

			return adjacencyList;
		}
		else {
			return new AdjacencyList(triples);
		}
	}
	
	static public class UnresolvablePredicateException extends Exception 
	{
		public UnresolvablePredicateException() {}
		public UnresolvablePredicateException(String msg) { super(msg); }
	}
	
}
