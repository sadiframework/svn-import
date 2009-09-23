package ca.wilkinsonlab.sadi.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryPattern;
import org.mindswap.pellet.query.impl.QueryPatternImpl;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

import ca.wilkinsonlab.sadi.vocab.W3C;
import ca.wilkinsonlab.sadi.utils.PredicateUtils;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class Pellet2JenaUtils 
{

	public static List<Triple> getTriples(Query query) 
	{
		List<Triple> triples = new ArrayList<Triple>();
		List<QueryPattern> queryPatterns = query.getQueryPatterns();
		for(QueryPattern pattern : queryPatterns)
			triples.add(Pellet2JenaUtils.getTriple(pattern));
		return triples;
	}
	
	public static List<QueryPattern> getQueryPatterns(List<Triple> triples)
	{
		List<QueryPattern> queryPatterns = new ArrayList<QueryPattern>();
		for(Triple triple : triples) 
			queryPatterns.add(getQueryPattern(triple));
		return queryPatterns;
	}
	
	public static QueryPattern getQueryPattern(Triple triple)
	{
		Node s = triple.getSubject();
		Node p = triple.getPredicate();
		Node o = triple.getObject();
		
		/*
		// Triple patterns with 'rdf:type' as the predicate are treated as a special case in Pellet.
		if(triple.getPredicate().toString().equals(W3C.PREDICATE_RDF_TYPE))
			return new QueryPatternImpl(getATerm(s), getATerm(o));
		else
		*/

		/* Note that we do not want to use the special construct form of QueryPatternImpl
		 * for rdf:type patterns: QueryPatternImpl(subj, obj), because it disallows variables
		 * in the object position. 
		 */
		return new QueryPatternImpl(getATerm(s), getATerm(p), getATerm(o));
	}
	
	public static Triple getTriple(QueryPattern pattern) 
	{
		ATermAppl s = pattern.getSubject();
		ATermAppl p;
		ATermAppl o = pattern.getObject();
		
		// Internally, Pellet represents rdf:type by setting the predicate to null.
		if(pattern.isTypePattern())
			p = ATermUtils.makeTermAppl(W3C.PREDICATE_RDF_TYPE);
		else 
			p = pattern.getPredicate();
		
		return getTriple(s, p, o);
	}
	
	public static Triple getTriple(ATermAppl s, ATermAppl p, ATermAppl o) 
	{   
		// Reverse the triple, if inv() has been applied to the predicate.
		if(ATermUtils.isInv(p)) {
			ATermAppl temp = s;
			s = o;
			o = temp;
			p = ATermUtils.makeTermAppl(p.getArguments().getFirst().toString());
		}
		
		return new Triple( getNode(s), getNode(p),getNode(o));
	}
	
	public static Node getNode(ATermAppl aterm) 
	{
		Node node;
		
		if(ATermUtils.isInv(aterm)) {
			throw new IllegalArgumentException("cannot convert an ATerm representing an inverted predicate '" 
					+ aterm.toString() + "' to a Jena Node");
		}
			
		if(ATermUtils.isVar(aterm))
			node = NodeCreateUtils.create("?" + aterm.getArgument(0).toString());
		else if(ATermUtils.isLiteral(aterm)) { 
			//node = NodeCreateUtils.create("'" + aterm.getArgument(0).toString() + "'");
			
			String datatypeURI = ATermUtils.getLiteralDatatype(aterm).trim();
			RDFDatatype datatype = datatypeURI.isEmpty() ? null : (new TypeMapper()).getSafeTypeByName(ATermUtils.getLiteralDatatype(aterm)); 
			node = Node.createLiteral(ATermUtils.getLiteralValue(aterm), ATermUtils.getLiteralLang(aterm), datatype);
		}
		else if(ATermUtils.isBnode(aterm)) {

			// Blank nodes are identified in Pellet by a special prefix (PelletOptions.BNODE).
			// We need to remove this prefix before we create the Jena node.
			String label = aterm.toString();
			
			if(!label.startsWith(PelletOptions.BNODE))
				throw new RuntimeException("the blank node " + label + " was expected to start with " + PelletOptions.BNODE);
			
			label = StringUtils.substringAfter(label, PelletOptions.BNODE);
			node = NodeCreateUtils.create("_" + label);

		}
		else // URI
			node = NodeCreateUtils.create(aterm.toString());
		
		return node;
	}
	
	public static ATermAppl getATerm(Node node) 
	{
		ATermAppl aterm;
		
		if(node.isVariable())
			aterm = ATermUtils.makeVar(node.getName());
		else if(node.isURI()) {
			String nodeStr = node.toString();
			if(PredicateUtils.isInverted(nodeStr)) 
				aterm = ATermUtils.makeInv(ATermUtils.makeTermAppl(PredicateUtils.invert(nodeStr)));
			else
				aterm = ATermUtils.makeTermAppl(node.getURI());
		}
		else if(node.isBlank()) {
			aterm = ATermUtils.makeTermAppl(PelletOptions.BNODE + node.getBlankNodeLabel());
		}
		else if(node.isLiteral()) {

			LiteralLabel literal = node.getLiteral();
			RDFDatatype datatype = literal.getDatatype();
			
			if (datatype == null) {
				//aterm = ATermUtils.makePlainLiteral(String.valueOf(literal.getValue()));
				aterm = ATermUtils.makePlainLiteral(literal.getLexicalForm());
			}
			else {
				//aterm = ATermUtils.makeTypedLiteral(String.valueOf(literal.getValue()), datatype.getURI());
				aterm = ATermUtils.makeTypedLiteral(literal.getLexicalForm(), datatype.getURI());
			}
		}
		else 	
			throw new IllegalArgumentException("attempt to add non-URI, non-blank, non-literal node");
			
		return aterm;
	}

	/**
	 * Return an ATermAppl corresponding to the given property.  This
	 * method accepts properties of the form "inv(property)".
	 * 
	 * @param property
	 * @return an ATermAppl corresponding to property
	 */
	public static ATermAppl getProperty(String property)
	{
		ATermAppl aterm;
		if(PredicateUtils.isInverted(property)) 
			aterm = ATermUtils.makeInv(ATermUtils.makeTermAppl(PredicateUtils.invert(property)));
		else
			aterm = ATermUtils.makeTermAppl(property);
		return aterm;
	}	
	
	public static Literal getLiteral(ATermAppl literal)
	{
		if(!ATermUtils.isLiteral(literal))
			throw new IllegalArgumentException(literal.toString() + " is not a literal");
		
		String datatype = ATermUtils.getLiteralDatatype(literal);
		RDFDatatype jenaDatatype = (new TypeMapper()).getSafeTypeByName(datatype);
		Literal jenaLiteral;

		if(jenaDatatype.getURI().equals(""))
			jenaLiteral = ResourceFactory.createPlainLiteral(ATermUtils.getLiteralValue(literal));
		else
			jenaLiteral = ResourceFactory.createTypedLiteral(ATermUtils.getLiteralValue(literal), jenaDatatype);
		
		return jenaLiteral;
	}
}
