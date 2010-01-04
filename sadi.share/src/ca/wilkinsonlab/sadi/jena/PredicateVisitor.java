package ca.wilkinsonlab.sadi.jena;

import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.ElementVisitorBase;

/**
 * Build a list of predicate URIs used in a Jena query.
 * Pellet's ARQParser class has a parse() method for converting Jena 
 * queries to Pellet queries, which would be handy here.  Unfortunately, 
 * that method assumes that all predicates have already been defined in 
 * the KnowledgeBase. 
 */
public class PredicateVisitor extends ElementVisitorBase
{
	Set<String> predicates;
	public PredicateVisitor(Set<String> predicateURIs) 
	{
		predicates = predicateURIs;
	}
	
	public void visit(ElementTriplesBlock el) 
	{
		BasicPattern triples = el.getPattern();
		for(int i = 0; i < triples.size(); i++) {
			Triple triple = triples.get(i);
			Node predicate = triple.getPredicate();
			if(predicate.isConcrete())
				predicates.add(predicate.toString());
		}
	}
}
