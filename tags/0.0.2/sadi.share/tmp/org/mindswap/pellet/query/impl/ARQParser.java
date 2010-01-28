/*
 * Created on Jul 27, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.mindswap.pellet.query.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.exceptions.UnsupportedQueryException;
import org.mindswap.pellet.jena.JenaUtils;
import org.mindswap.pellet.jena.OWLLoader;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryEngine;
import org.mindswap.pellet.query.QueryParser;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.URIUtils;

import aterm.ATermAppl;
import aterm.ATermList;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.ARQConstants;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author Evren Sirin
 */
public class ARQParser implements QueryParser {
    public static Log log = LogFactory.getLog( QueryEngine.class );
    
    private Syntax syntax = QueryEngine.DEFAULT_SYNTAX;
    
    private Set triples;
    
    private Map terms;
    
    private KnowledgeBase kb;

    
    public ARQParser() {        
    }
    
    public Syntax getSyntax() {
        return syntax;
    }
    
    public void setSyntax( Syntax syntax ) {
        this.syntax = syntax;
    }
    
	public Query parse(InputStream in, KnowledgeBase kb) throws IOException {
		return parse( new InputStreamReader(in), kb );
	}
	
	public Query parse(Reader in, KnowledgeBase kb) throws IOException {
		StringBuffer queryString = new StringBuffer();
		BufferedReader r = new BufferedReader( in );
		
		String line = r.readLine();
		while ( line != null ) {
			queryString.append( line ).append("\n");
			line = r.readLine();
		}
		
		return parse( queryString.toString(), kb );
	}

	public Query parse( String queryStr, KnowledgeBase kb ) {
	    com.hp.hpl.jena.query.Query sparql = QueryFactory.create( queryStr, syntax );
	    
	    return parse( sparql, kb );
	}
    
    private void collectResultVars( com.hp.hpl.jena.query.Query sparql, Query query ) {
        // very important to call this function so that getResultVars() will work 
        // fine for SELECT * queries 
        sparql.setResultVars();
        for ( Iterator i = sparql.getResultVars().iterator(); i.hasNext(); ) {
            String var = (String) i.next();
    
            query.addResultVar( ATermUtils.makeVar( var ) );
        }        
    }
	
	public Query parse( com.hp.hpl.jena.query.Query sparql, KnowledgeBase kb ) {
        this.kb = kb;

        if( sparql.isDescribeType() )
	        throw new UnsupportedQueryException("DESCRIBE queries cannot be answered with PelletQueryEngine");	    
        
	    Element pattern = sparql.getQueryPattern();

	    if( !(pattern instanceof ElementGroup) )
	        throw new UnsupportedQueryException("ElementGroup was expected");
        
        terms = new HashMap();
        
	    ElementGroup elementGroup = (ElementGroup) pattern;
	    
	    List elements = elementGroup.getElements();
	    Element first = (Element) elements.get( 0 );
	    if( elements.size() != 1 || !(first instanceof ElementTriplesBlock) )
            throw new UnsupportedQueryException("Complex query patterns are not supported");
	    
	    triples = new HashSet( ((ElementTriplesBlock) first).getTriples().getList() );
            
        List iterableList = new ArrayList( triples );
	    
	    QueryImpl query = new QueryImpl( kb );
        
        collectResultVars( sparql, query );
        
        for( Iterator i = iterableList.iterator(); i.hasNext(); ) {
            Triple t = (Triple) i.next();

            if( !triples.contains( t ) )
                continue;
            
            Node subj = t.getSubject();
            Node pred = t.getPredicate();
            Node obj = t.getObject();
            
            ATermAppl s1 = node2term( subj );
            ATermAppl p1 = node2term( pred );
            ATermAppl o1 = node2term( obj ) ;
            
            terms.put( subj, s1 );
            terms.put( pred, p1 );
            terms.put( obj, o1 );
        }
        
		for( Iterator i = triples.iterator(); i.hasNext(); ) {
			Triple t = (Triple) i.next();
			
            Node subj = t.getSubject();
            Node pred = t.getPredicate();
            Node obj = t.getObject();
            
            ATermAppl s = (ATermAppl) terms.get( subj );
            ATermAppl p = (ATermAppl) terms.get( pred );
            ATermAppl o = (ATermAppl) terms.get( obj ) ; 
             
            if( subj.isVariable() && !subj.getName().startsWith(ARQConstants.anonVarMarker) )
                query.addDistVar( s );
            if( obj.isVariable() && !obj.getName().startsWith(ARQConstants.anonVarMarker) )
                query.addDistVar( o );
            
            /*
             * PATCH: Original Pellet code in comment below.  There seems to be 
             * no need for this restriction for our purposes. --BV
             */

            /*
            if( obj.toString().startsWith( RDF.getURI() )
                || obj.toString().startsWith( OWL.getURI() )
                || obj.toString().startsWith( RDFS.getURI() ) ) {

                // this is to make sure no TBox, RBox queries are encoded in RDQL
                throw new UnsupportedQueryException(
                    "Terms that belong to [RDF, RDFS, OWL] namespaces cannot be used as objects in ABoxQuery: "
                        + obj );
            }
            */
            
			if( pred.equals( RDF.Nodes.type ) ) {

				/*
				 * PATCH: Original Pellet code in comment below.  If the object of an rdf:type is not a URI which represents 
				 * a class in the KB, we want it to act like an ordinary predicate. -- BV
                 */

                /*
                if( ATermUtils.isVar( o ) )
                    throw new UnsupportedQueryException(
                        "Variables cannot be used as objects of rdf:type triples in ABoxQuery" );

                if( !kb.isClass( o ) ) {
                    query.setHasUndefinedPredicate( true );
                    log.warn( "Class " + o + " used in the query is not defined in the KB." );
                }

                query.addTypePattern( s, o );
                */
                
                if( kb.isClass(o) )
                	query.addTypePattern( s, o );
                else 
                	query.addEdgePattern(s, ATermUtils.makeTermAppl(RDF.Nodes.type.getURI()), o);
                
            }
            else if( pred.isVariable() ) {
                throw new UnsupportedQueryException(
                    "Variables cannot be used in predicate position in AboxQuery" );
            }
            /*
             * PATCH: Original Pellet code in comment below.  There seems to be 
             * no need for this restriction for our purposes. --BV
             */
			
			/*
            else if( pred.toString().startsWith( RDF.getURI() )
                || pred.toString().startsWith( OWL.getURI() )
                || pred.toString().startsWith( RDFS.getURI() ) ) {

                // this is to make sure no TBox, RBox queries are encoded in RDQL
                throw new UnsupportedQueryException(
                    "Predicates that belong to [RDF, RDFS, OWL] namespaces cannot be used in ABoxQuery: "
                        + pred );
            }
            */
            else {
                if( !kb.isProperty( p ) ) {
                    query.setHasUndefinedPredicate( true );
                    log.warn( "Property " + pred + " used in the query is not defined in the KB." );
                }
                
                if( kb.isDatatypeProperty( p ) && query.getDistVars().contains( o )
                    && subj.isVariable()  && !query.getDistVars().contains( s ) ) {
                    log.warn( "Warning: Forcing variable " + subj
                        + " to be distinguished (Subject of datatype triple)" );

                    query.addDistVar( s );
                }

                query.addEdgePattern( s, p, o );
            }
		}
        
		return query;
	}	
    
    private Node getObject(Node subj, Node pred) {
        for ( Iterator i = triples.iterator(); i.hasNext(); ) {
            Triple t = (Triple) i.next();
            if( subj.equals( t.getSubject() ) && pred.equals( t.getPredicate() ) ) {
                i.remove();
                return t.getObject();
            }
        }
                    
        return null;
    }

    private boolean hasObject(Node subj, Node pred) {
        for ( Iterator i = triples.iterator(); i.hasNext(); ) {
            Triple t = (Triple) i.next();
            if( subj.equals( t.getSubject() ) && pred.equals( t.getPredicate() ) )
                return true;
        }
                    
        return false;
    }
    
    private boolean hasObject(Node subj, Node pred, Node obj) {
        for ( Iterator i = triples.iterator(); i.hasNext(); ) {
            Triple t = (Triple) i.next();
            if( subj.equals( t.getSubject() ) && pred.equals( t.getPredicate() ) ) {
                i.remove();
                if( obj.equals( t.getObject() ) )
                    return true;
                else
                    throw new UnsupportedQueryException( "Expecting rdf:type " + obj
                        + " but found rdf:type " + t.getObject() );                
            }
        }
                    
        return false;
    }

    private ATermList createList( Node node ) {
        if( node.equals( RDF.nil.asNode() ) )
            return ATermUtils.EMPTY_LIST;
        else if( terms.containsKey( node ) ) 
            return (ATermList) terms.get( node );
        
        hasObject( node, RDF.type.asNode(), RDF.List.asNode() );

        Node first = getObject( node, RDF.first.asNode() );
        Node rest = getObject( node, RDF.rest.asNode() );

        if( first == null || rest == null ) {
            throw new UnsupportedQueryException( "Invalid list structure: List " + node + " does not have a "
                + (first == null ? "rdf:first" : "rdf:rest")
                + " property." );
        }

        ATermList list = ATermUtils.makeList( node2term( first ), createList( rest ) );

        terms.put( node, list );

        return list;
    }

    private ATermAppl createRestriction( Node node ) throws UnsupportedFeatureException {
        ATermAppl aTerm = ATermUtils.TOP;

        hasObject( node, RDF.type.asNode(), OWL.Restriction.asNode() );
        
        Node p = getObject(node, OWL.onProperty.asNode());

        // TODO warning message: no owl:onProperty
        if(p == null) return aTerm;

        ATermAppl pt = node2term(p);
        if ( !kb.isProperty( pt ) ) 
            throw new UnsupportedQueryException( "Property " + pt + " is not present in KB." );

        // TODO warning message: multiple owl:onProperty
        Node o = null;
        if((o = getObject(node, OWL.hasValue.asNode())) != null) {
            if( PelletOptions.USE_PSEUDO_NOMINALS ) {
                if( o.isLiteral() ) {                       
                    aTerm = ATermUtils.makeMin(pt, 1, ATermUtils.TOP_LIT);
                }
                else {
                    ATermAppl ind = ATermUtils.makeTermAppl( o.getURI() );
                    if( !kb.isIndividual( ind ) )
                        throw new UnsupportedQueryException( "Individual " + ind + " is not present in KB." );
                    
                    ATermAppl nom = ATermUtils.makeTermAppl( o.getURI() + "_nom" );

                    aTerm = ATermUtils.makeSomeValues( pt, nom );
                }
            }
            else {
                ATermAppl ot = node2term(o);

                aTerm = ATermUtils.makeHasValue(pt, ot);
            }
        }
        else if((o = getObject(node, OWL.allValuesFrom.asNode())) != null) {
            ATermAppl ot = node2term(o);

            aTerm = ATermUtils.makeAllValues(pt, ot);
        }
        else if((o = getObject(node, OWL.someValuesFrom.asNode())) != null) {
            ATermAppl ot = node2term(o);

            aTerm = ATermUtils.makeSomeValues(pt, ot);
        }
        else if((o = getObject(node, OWL.minCardinality.asNode())) != null) {
            try {
                ATermAppl top = null;
                if( kb.isDatatypeProperty( pt ) )
                    top = ATermUtils.TOP_LIT;
                else                
                    top = ATermUtils.TOP;                
                
                int cardinality = Integer.parseInt(o.getLiteral().getLexicalForm());
                aTerm = ATermUtils.makeMin(pt, cardinality, top);
            } catch(Exception ex) {
                // TODO print warning message (invalid number)
            }
        }
        else if((o = getObject(node, OWL.maxCardinality.asNode())) != null) {
            try {
                ATermAppl top = null;
                if( kb.isDatatypeProperty( pt ) )
                    top = ATermUtils.TOP_LIT;
                else                
                    top = ATermUtils.TOP; 
                
                int cardinality = Integer.parseInt(o.getLiteral().getLexicalForm());
                aTerm = ATermUtils.makeMax(pt, cardinality, top);
            } catch(Exception ex) {
                // TODO print warning message (invalid number)
            }
        }
        else if((o = getObject(node, OWL.cardinality.asNode())) != null) {
            try {
                ATermAppl top = null;
                if( kb.isDatatypeProperty( pt ) )
                    top = ATermUtils.TOP_LIT;
                else                
                    top = ATermUtils.TOP; 
                
                int cardinality = Integer.parseInt(o.getLiteral().getLexicalForm());
                aTerm = ATermUtils.makeCard(pt, cardinality, top);
            } catch(Exception ex) {
                // TODO print warning message (invalid number)
            }
        }
        else {
            // TODO print warning message (invalid restriction type)
        }

        return aTerm;
    }

    private ATermAppl node2term(Node node) {
        ATermAppl aTerm = (ATermAppl) terms.get(node);

        if(aTerm == null) {
            if( node.equals( OWL.Thing.asNode() ))
                return ATermUtils.TOP;
            else if( node.equals( OWL.Nothing.asNode() ))
                return ATermUtils.BOTTOM;           
            else if( node.equals( RDF.type.asNode() ))
                return null;           
            else if( node.isLiteral() ) 
                return JenaUtils.makeLiteral( node.getLiteral() );
            else if(hasObject(node, OWL.onProperty.asNode())) {
                aTerm = createRestriction(node);
                terms.put(node, aTerm);
            }
            else if( node.isBlank() || node.isVariable() ) {
                Node o = null;
                if((o = getObject(node, OWL.intersectionOf.asNode())) != null ) {
                    ATermList list = createList( o );
                    hasObject( node, RDF.type.asNode(), OWL.Class.asNode() );

                    aTerm = ATermUtils.makeAnd( list );
                }
                else if((o = getObject(node, OWL.unionOf.asNode())) != null ) {
                    ATermList list = createList( o );
                    hasObject( node, RDF.type.asNode(), OWL.Class.asNode() );

                    aTerm = ATermUtils.makeOr( list );
                }
                else if((o = getObject(node, OWL.oneOf.asNode())) != null) {
                    ATermList list = createList( o );
                    hasObject( node, RDF.type.asNode(), OWL.Class.asNode() );

                    ATermList result = ATermUtils.EMPTY_LIST;
                    for( ATermList l = list; !l.isEmpty(); l = l.getNext() ) {
                        ATermAppl c = (ATermAppl) l.getFirst();
                        if( PelletOptions.USE_PSEUDO_NOMINALS ) {
                            ATermAppl nominal = ATermUtils.makeTermAppl( c.getName() + "_nominal" );
                            result = result.insert( nominal );
                        }
                        else {
                            ATermAppl nominal = ATermUtils.makeValue( c );
                            result = result.insert( nominal );
                        }
                    }

                    aTerm = ATermUtils.makeOr( result );
                }
                else if((o = getObject(node, OWL.complementOf.asNode())) != null) {
                    ATermAppl complement = node2term( o );
                    hasObject( node, RDF.type.asNode(), OWL.Class.asNode() );

                    aTerm = ATermUtils.makeNot( complement );
                }
                else if( node.isVariable() )
                    return ATermUtils.makeVar( node.getName() );
                else {
                    String bNode = PelletOptions.BNODE + node.getBlankNodeId();
                    aTerm = ATermUtils.makeTermAppl( bNode );
                }
            }
            else {
                String uri = node.getURI();

                if( PelletOptions.USE_LOCAL_NAME ) {
                    if( uri.startsWith( Namespaces.XSD ) )
                        aTerm = ATermUtils.makeTermAppl( uri );
                    else
                        aTerm = ATermUtils.makeTermAppl( URIUtils.getLocalName( uri ) );
                }
                else if( PelletOptions.USE_QNAME ) {
                    if( uri.startsWith( Namespaces.XSD ) )
                        aTerm = ATermUtils.makeTermAppl( uri );
                    else
                        aTerm = ATermUtils.makeTermAppl( OWLLoader.qnames.shortForm( uri ) );
                }
                else
                    aTerm = ATermUtils.makeTermAppl( uri );
            }
    
            terms.put(node, aTerm);
        }

        return aTerm;
    }
}
