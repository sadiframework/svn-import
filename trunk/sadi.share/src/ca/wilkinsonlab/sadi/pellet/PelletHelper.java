package ca.wilkinsonlab.sadi.pellet;

import java.util.ArrayList;
import java.util.List;

import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryPattern;
import org.mindswap.pellet.query.impl.QueryPatternImpl;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

import ca.wilkinsonlab.sadi.optimizer.StaticOptimizer;

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
		Node s1 = triple.getSubject();
		Node p1 = triple.getPredicate();
		Node o1 = triple.getObject();
		
		Node node[] = { s1, p1, o1 };
		ATermAppl aterm[] = new ATermAppl[3];

		for(int i = 0; i < 3; i++) {
			if(node[i].isVariable())
				aterm[i] = ATermUtils.makeVar(node[i].getName());
			else if(node[i].isURI())
				aterm[i] = ATermUtils.makeTermAppl(node[i].getURI());
			else
				aterm[i] = ATermUtils.makeLiteral(ATermUtils.makeTermAppl(node[i].getLiteral().toString()));
		}
		
		return new QueryPatternImpl(aterm[0], aterm[1], aterm[2]);
	}
	
	public static Triple getTripleFromQueryPattern(QueryPattern pattern) 
	{
		return getTripleFromATermAppls(pattern.getSubject(), pattern.getPredicate(), pattern.getObject());
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
		
		ATermAppl input[] = { s, p, o };
		Node output[] = { null, null, null };
		for(int i = 0; i < input.length; i++) {
			ATermAppl cur = input[i];
			if(ATermUtils.isVar(cur))
				output[i] = NodeCreateUtils.create("?" + cur.getArgument(0).toString());
			else if(i == 2 && ATermUtils.isLiteral(cur)) 
				output[i] = NodeCreateUtils.create("'" + cur.getArgument(0).toString() + "'");
			else 
				output[i] = NodeCreateUtils.create(cur.toString());
		}
		
		return new Triple(output[0], output[1], output[2]);
	}

	public static boolean isInverted(String predicate)
	{
		return ATermUtils.isInv(ATermUtils.makeTermAppl(predicate));
	}
	
	public static String invert(String predicate)
	{
		return ATermUtils.makeInv(ATermUtils.makeTermAppl(predicate)).toString();
	}	
	
}
