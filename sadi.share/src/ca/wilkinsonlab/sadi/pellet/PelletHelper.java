package ca.wilkinsonlab.sadi.pellet;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryPattern;
import org.mindswap.pellet.query.impl.QueryPatternImpl;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

import ca.wilkinsonlab.sadi.optimizer.StaticOptimizer;
import ca.wilkinsonlab.sadi.vocab.W3C;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;

public class PelletHelper 
{
	
	public static void optimizeQuery(Query query, StaticOptimizer opt)
	{
    	List<Triple> triples = opt.optimize(PelletHelper.getTriplesFromPelletQuery(query));
    	query.getQueryPatterns().clear();
    	query.getQueryPatterns().addAll(PelletHelper.getQueryPatternsFromTriples(triples));
	}
	
	public static List<Triple> getTriplesFromPelletQuery(Query query) 
	{
		List<Triple> triples = new ArrayList<Triple>();
		List<QueryPattern> queryPatterns = query.getQueryPatterns();
		for(QueryPattern pattern : queryPatterns)
			triples.add(getTripleFromQueryPattern(pattern));
		return triples;
	}
	
	public static List<QueryPattern> getQueryPatternsFromTriples(List<Triple> triples)
	{
		List<QueryPattern> queryPatterns = new ArrayList<QueryPattern>();
		for(Triple triple : triples) 
			queryPatterns.add(getQueryPatternFromTriple(triple));
		return queryPatterns;
	}
	
	public static QueryPattern getQueryPatternFromTriple(Triple triple)
	{
		Node s = triple.getSubject();
		Node p = triple.getPredicate();
		Node o = triple.getObject();
		
		// Triple patterns with 'rdf:type' as the predicate are treated as a special case in Pellet.
		if(triple.getPredicate().toString().equals(W3C.PREDICATE_RDF_TYPE))
			return new QueryPatternImpl(getATermFromNode(s), getATermFromNode(o));
		else
			return new QueryPatternImpl(getATermFromNode(s), getATermFromNode(p), getATermFromNode(o));
	}
	
	public static Triple getTripleFromQueryPattern(QueryPattern pattern) 
	{
		ATermAppl s = pattern.getSubject();
		ATermAppl p;
		ATermAppl o = pattern.getObject();
		
		// Internally, Pellet represents rdf:type by setting the predicate to null.
		if(pattern.isTypePattern())
			p = ATermUtils.makeTermAppl(W3C.PREDICATE_RDF_TYPE);
		else 
			p = pattern.getPredicate();
		
		return getTripleFromATermAppls(s, p, o);
	}
	
	public static Triple getTripleFromATermAppls(ATermAppl s, ATermAppl p, ATermAppl o) 
	{   
		// Reverse the triple, if inv() has been applied to the predicate.
		if(ATermUtils.isInv(p)) {
			ATermAppl temp = s;
			s = o;
			o = temp;
			p = ATermUtils.makeTermAppl(p.getArguments().getFirst().toString());
		}
		
		return new Triple( getNodeFromATerm(s),	getNodeFromATerm(p),getNodeFromATerm(o));
	}
	
	public static Node getNodeFromATerm(ATermAppl aterm) 
	{
		Node node;
		
		if(ATermUtils.isInv(aterm)) {
			throw new IllegalArgumentException("Cannot converted an ATerm representing an inverted predicate '" 
					+ aterm.toString() + "' to a Jena Node");
		}
			
		if(ATermUtils.isVar(aterm))
			node = NodeCreateUtils.create("?" + aterm.getArgument(0).toString());
		else if(ATermUtils.isLiteral(aterm)) 
			node = NodeCreateUtils.create("'" + aterm.getArgument(0).toString() + "'");
		else if(ATermUtils.isBnode(aterm)) {

			// Blank nodes are identified in Pellet by a special prefix (PelletOptions.BNODE).
			// We need to remove this prefix before we create the Jena node.
			String label = aterm.toString();
			
			if(!label.startsWith(PelletOptions.BNODE))
				throw new RuntimeException("The blank node " + label + " was expected to start with " + PelletOptions.BNODE);
			
			label = StringUtils.substringAfter(label, PelletOptions.BNODE);
			node = NodeCreateUtils.create("_" + label);

		}
		else // URI
			node = NodeCreateUtils.create(aterm.toString());
		
		return node;
	}
	
	public static ATermAppl getATermFromNode(Node node) 
	{
		ATermAppl aterm;
		
		if(node.isVariable())
			aterm = ATermUtils.makeVar(node.getName());
		else if(node.isURI())
			aterm = ATermUtils.makeTermAppl(node.getURI());
		else if(node.isBlank())
			aterm = ATermUtils.makeTermAppl(PelletOptions.BNODE + node.getBlankNodeLabel());
		else
			aterm = ATermUtils.makeLiteral(ATermUtils.makeTermAppl(node.getLiteral().toString()));
		
		return aterm;
	}
}
