package ca.wilkinsonlab.sadi.jena;

import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.syntax.ElementDataset;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementNamedGraph;
import com.hp.hpl.jena.sparql.syntax.ElementOptional;
import com.hp.hpl.jena.sparql.syntax.ElementService;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.ElementUnion;
import com.hp.hpl.jena.sparql.syntax.ElementUnsaid;
import com.hp.hpl.jena.sparql.syntax.ElementVisitor;

/**
 * Build a list of predicate URIs used in a Jena query.
 * Pellet's ARQParser class has a parse() method for converting Jena 
 * queries to Pellet queries, which would be handy here.  Unfortunately, 
 * that method assumes that all predicates have already been defined in 
 * the KnowledgeBase. 
 */
public class PredicateVisitor implements ElementVisitor {

	Set<String> predicates;
	public PredicateVisitor(Set<String> predicateURIs) 
	{
		predicates = predicateURIs;
	}
	
	public void visit(ElementTriplesBlock el) 
	{
		BasicPattern triples = el.getTriples();
		for(int i = 0; i < triples.size(); i++) {
			Triple triple = triples.get(i);
			Node predicate = triple.getPredicate();
			if(predicate.isConcrete())
				predicates.add(predicate.toString());
		}
	}

	public void visit(ElementFilter el) {}
	public void visit(ElementUnion el) {}
	public void visit(ElementOptional el) {}
	public void visit(ElementGroup el) {}
	public void visit(ElementDataset el) {}
	public void visit(ElementNamedGraph el) {}
	public void visit(ElementUnsaid el) {}
	public void visit(ElementService el) {}

}
