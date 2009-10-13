// The MIT License
//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package org.mindswap.pellet.jena;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.exceptions.UnsupportedQueryException;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryEngine;
import org.mindswap.pellet.query.impl.ARQParser;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Timer;

import aterm.ATermAppl;

import ca.wilkinsonlab.sadi.pellet.DynamicKnowledgeBase;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.compose.Union;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * The main reasoner class designed to support Jena applications. It basically
 * wraps a {@link org.mindswap.pellet.KnowledgeBase KnowledgeBase} object and 
 * does the conversion between Jena structures and ATerms.  
 * 
 * @author Evren Sirin
 */
public class OWLReasoner {
    public static Log log = LogFactory.getLog( OWLReasoner.class );

    /**
     * @deprecated Edit log4j.properties to turn on debugging
     */    
	public static boolean DEBUG  = false;

	private KnowledgeBase kb;
	private OWLLoader loader;
	/**
	 * @deprecated
	 */
	private boolean econnEnabled;
	private ModelExtractor modelExtractor;
	private OWLSpecies species;

	private DisjointMultiUnion graph;
	private Model model;	
	
	private boolean discardJenaGraph;

	public OWLReasoner() {
		log.info("Using patched OWLReasoner");
		kb = new DynamicKnowledgeBase();
		graph = new DisjointMultiUnion( true, kb, null );
		model = ModelFactory.createModelForGraph( graph );
		((DynamicKnowledgeBase)kb).setModel(model);
		loader = new OWLLoader();
		loader.setGraph( graph );
		modelExtractor = new ModelExtractor( this );
		graph.setLoader(loader);

		
		econnEnabled = false;
		discardJenaGraph = false;
	}
	
	/**
	 * Clear the loaded ontologies from reasoner's memory.
	 *
	 */
	public void clear() {
	    graph.releaseListeners();
		graph = new DisjointMultiUnion( true, kb, null );
		model = ModelFactory.createModelForGraph( graph );
		kb.clear();
		loader.clear();
		loader.setGraph( graph );
		graph.setLoader(loader);
	}


	/**
	 * Load the ontology from the given URI (with its imports)
	 * 
	 * @param uri URI of the ontology
	 */
	public void load(String uri) {
		load( uri, true );
	}
	
	/**
	 * Load the ontology from the given URI, with or without its imports.
	 * 
	 * @param uri URI of the ontology
	 * @param withImports If true load imports too
	 */
	public void load(String uri, boolean withImports) {
		ModelReader reader = new ModelReader();
		Model model = reader.read(uri, withImports);
		load( model.getGraph() );		
	}
	
	/**
	 * Load the Jena model to the reasoner.
	 * 
	 * @param model
	 */
	public void load(Model newModel) {
	    DisjointMultiUnion newGraph = new DisjointMultiUnion( newModel.getGraph(), kb,this.getLoader() );
	    
	    load( newGraph );
	}
	
	/**
	 * Load the Jena graph to the reasoner.
	 * 
	 * @param graph
	 */
	public void load(Graph graph) {
	    DisjointMultiUnion newGraph = null;
	    
	    if(graph instanceof DisjointMultiUnion)
	    	newGraph = (DisjointMultiUnion) graph;
	    else {	
	        newGraph = new DisjointMultiUnion( graph, kb, this.getLoader() );
	    }
	    
	    load( newGraph );
	}
	
	@SuppressWarnings("unchecked")
	public void load(DisjointMultiUnion newGraph) {
	    if( newGraph.isEmpty() ) return;
	    
		Timer t = kb.timers.startTimer("Loading");
		
		if( !discardJenaGraph ) {
		    graph.addGraph( newGraph );
			model = ModelFactory.createModelForGraph(graph);
		}
		
		List allGraphs = newGraph.getSubGraphs();
		allGraphs.add( newGraph.getBaseGraph() );		
		for(Iterator i = allGraphs.iterator(); i.hasNext();) {
            Graph subG = (Graph) i.next();
            loader.load( subG, kb );
        }
		
//		loader.load( newGraph, kb );
		if( discardJenaGraph )
		    loader.clear();
		else
		    loader.setGraph( graph );
		
		t.stop();
	}

	
	/**
	 * Return the KnowledgeBase that backs this reasoner.
	 * 
	 * @return
	 */
	public KnowledgeBase getKB() {
		return kb;
	}
	
	/**
	 * Return the Jena model where all the loaded models are accumulated.
	 * 
	 * @return
	 */
	public Model getModel() {
		return model;
	}
	
	/**
	 * Return the species of the model loaded to this reasoner.
	 * 
	 * @return
	 */
	public OWLSpecies getSpecies() throws UnsupportedFeatureException {	    
	    if(model == null)
	        throw new UnsupportedFeatureException("No ontology loaded, reasoner cannot find the species!");

	    if(species == null) {
	        OWLSpeciesValidator validator = new OWLSpeciesValidator();
	        species = validator.validate( model );
	    }
	    
	    return species;
	}
	
	public String getLevel() {
	    return getSpecies().toString();
	}

	/**
	 * Check if the loaded ontologies is consistent. A KB is inconsistent when an individual
	 * belongs to an unsatisfiable class. Unsatisfiability may be due to multiple different 
	 * assertions. For example, an ontology is inconsistent when an individual is stated to 
	 * belong to two disjoint classes because the intersection of two disjoint classes is 
	 * empty.
	 * 
	 * @return
	 */
	public boolean isConsistent() {
		return kb.isConsistent();
	}

	/**
	 * Classify the loaded ontologies by computing all the direct/indirect subclass relations
	 * between each named class. This operation will take considerable amount of time on large
	 * ontologies. After classification all subclass information is cached so subsequent calls
	 * to functinos like getSubclasses() will be much faster. 
	 * 
	 */
	public void classify() {
		kb.classify();
	}

	/**
	 * For each named individual compute all the classes they belong to. Note that, calling 
	 * this function will automatically cause the KB to be classified. Therefore, realizing 
	 * large ontologies will take considerable amount of time.
	 */
	public void realize() {
		kb.realize();
	}
	
	/**
	 * Return the associated model extractor
	 * 
	 * @return
	 */
	public ModelExtractor getModelExtractor() {
	    return modelExtractor;
	}
	
	/**
	 * Returns a Jena model that contains the obvious and inferred subclass, 
	 * subproperty and type assertions. This model can be created once to
	 * provide efficient reasoning without the cost of conversion at each function
	 * call. However, the returned model will not be complete in the sense that
	 * property assertions between indviduals will not be included. 
	 * 
	 * This function simply combines the results of extractClassModel,
	 * extractPropertyModel and extractIndividualModel.
	 * 
	 * @param verbose If this parameter is false only direct subclass, subproperty
	 * and type assertions are included in the result (rest can easily be found 
	 * with transitivity). If the parameter is true all the subclass, subproperty
	 * and type assertions are put into the result.
	 * @return
	 */
	public Model extractModel(boolean verbose) {
	    modelExtractor.setVerbose(verbose);	    
		return modelExtractor.extractModel();
	}
	
	/**
	 * Returns a Jena model that contains the subclass and equivalence relations
	 * between named classes.
	 * 
	 * @param verbose If false only direct subclass relations are returned,
	 * otherwise all the subclass relations are put into the result.
	 * @return
	 */
	public Model extractClassModel(boolean verbose) {
	    modelExtractor.setVerbose(verbose);
		return modelExtractor.extractClassModel();
	}

	/**
	 * Returns a Jena model that contains the information about properties.
	 * Namely subproperty, equivalent property relations, and attributes
	 * such as Functional, Symmetric, Transitive, etc. are included in the
	 * model 
	 * 
	 * @param verbose If false only direct subproperty relations are returned,
	 * otherwise all the subproperty relations are put into the result.
	 * @return
	 */
	public Model extractPropertyModel(boolean verbose) {
	    modelExtractor.setVerbose(verbose);
		return modelExtractor.extractPropertyModel();
	}
	
	/**
	 * Returns a Jena model that contains the type assertions about individuals.
	 * 
	 * @param verbose If false only direct type assertions are returned,
	 * otherwise all the type relations are put into the result.
	 * @return
	 */
	public Model extractIndividualModel(boolean verbose) {
	    modelExtractor.setVerbose(verbose);
		return modelExtractor.extractIndividualModel();
	}
	
	/**
	 * Return the set of all named classes.
	 * 
	 * @return Set of Jena resources
	 */
	public Set getClasses() {
		return toJenaSet(kb.getClasses());
	}

	/**
	 * Return the set of all named individuals.
	 * 
	 * @return Set of Jena resources
	 */
	public Set getIndividuals() {
		return toJenaSet(kb.getIndividuals());
	}
	
	/**
	 * Convert the RDFNode to ATerm representation.
	 * 
	 * @param r
	 * @return
	 */
	public ATermAppl node2term(RDFNode r) {
		return loader.node2term(r.asNode());
	}
	
	/**
	 * Check id the given resource is defined as a class.
	 * 
	 * @param c
	 * @return
	 */
	public boolean isClass(Resource c) {
		return kb.isClass(node2term(c));
	}

	/**
	 * Check if the given class is satisfiable. 
	 * 
	 * @param r
	 * @return
	 */
	public boolean isSatisfiable(Resource r) {
		return kb.isSatisfiable(node2term(r));
	}

	public boolean isSubTypeOf(String d1, String d2) {
		return kb.isSubTypeOf(ATermUtils.makeTermAppl(d1), ATermUtils.makeTermAppl(d2));
	}
	
	public boolean isSubTypeOf(RDFDatatype d1, RDFDatatype d2) {
		return kb.isSubTypeOf(ATermUtils.makeTermAppl(d1.getURI()), ATermUtils.makeTermAppl(d2.getURI()));
	}
	
	/**
	 * @deprecated  As of Pellet 1.1.1, replaced by {@link #isSubClassOf(Resource,Resource)}
	 */
	public boolean isSubclassOf(Resource r1, Resource r2) {
	    return isSubClassOf(r1, r2);
	}
	
	public boolean isSubClassOf(Resource r1, Resource r2) {
		return kb.isSubClassOf(node2term(r1), node2term(r2));
	}

	public boolean isEquivalentClass(Resource r1, Resource r2) {
		return kb.isEquivalentClass(node2term(r1), node2term(r2));
	}

	public boolean isDisjoint(Resource c1, Resource c2) {
		return kb.isDisjoint(node2term(c1), node2term(c2));
	}

	public boolean isComplement(Resource c1, Resource c2) {
		return kb.isComplement(node2term(c1), node2term(c2));

	}

	public boolean isProperty(Resource p) {
		return kb.isProperty(node2term(p));
	}
		
	public boolean isDatatypeProperty(Resource p) {
		return kb.isDatatypeProperty(node2term(p));
	}
	
	public boolean isObjectProperty(Resource p) {
		return kb.isObjectProperty(node2term(p));
	}
	
	public boolean isAnnotationProperty(Resource p) {
		return kb.isAnnotationProperty(node2term(p));
	}
	public boolean isTransitiveProperty(Resource p) {
		return kb.isTransitiveProperty(node2term(p));
	}

	public boolean isSymmetricProperty(Resource p) {
		return kb.isInverse(node2term(p), node2term(p));
	}

	public boolean isFunctionalProperty(Resource p) {
		return kb.isFunctionalProperty(node2term(p));
	}

	public boolean isInverseFunctionalProperty(Resource p) {
		return kb.isInverseFunctionalProperty(node2term(p));
	}

	public boolean isSubPropertyOf(Resource p1, Resource p2) {
		return kb.isSubPropertyOf(node2term(p1), node2term(p2));
	}

	public boolean isEquivalentProperty(Resource p1, Resource p2) {
		return kb.isEquivalentProperty(node2term(p1), node2term(p2));
	}

	public boolean isInverse(Resource p1, Resource p2) {
		return kb.isInverse(node2term(p1), node2term(p2));
	}

	public boolean hasDomain(Resource p, Resource c) {
		return kb.hasDomain(node2term(p), node2term(c));
	}

	public boolean hasRange(Resource p, Resource c) {
		return kb.hasRange(node2term(p), node2term(c));
	}

	public boolean isIndividual(Resource ind) {
		return kb.isIndividual(node2term(ind));
	}

	public boolean isSameAs(Resource i1, Resource i2) {
		return kb.isSameAs(node2term(i1), node2term(i2));
	}

	public boolean isDifferentFrom(Resource i1, Resource i2) {
		return kb.isDifferentFrom(node2term(i1), node2term(i2));
	}

	public boolean isType(Resource ind, Resource c) {
		if(ind.isAnon()) {
			return kb.isSatisfiable(node2term(c));
		}
		
		return kb.isType(node2term(ind), node2term(c));
	}

	/**
	 * Checks is a triple (s, p, o) exists in the knowledge base. Using
	 * the strategy from "Reducing OWL Entailment to DL Satisfiability"
	 * paper.
	 *
	 * boolean
	 * @param s subject of the triple
	 * @param p predicate of the triple
	 * @param o object of the triple
	 * @return
	 */
	public boolean hasPropertyValue(Resource s, Resource p, RDFNode o) {
		if(s.isAnon() || (o instanceof Resource && ((Resource)o).isAnon())) {
			// TODO create a query object from ABox
		    // each bnode in the abox becomes an (undistinguished) var in the
		    // query and a boolean query with no dist, vars is created
		    return false;
		}
		
		return kb.hasPropertyValue(node2term(s), node2term(p), node2term(o));
	}

	/**
	 * Check if all the statements in a given model is entailed by the current KB.
	 * 
	 * @param model
	 * @return
	 * @throws UnsupportedFeatureException
	 */
	public boolean isEntailed(Model model) throws UnsupportedFeatureException {
	    loader.setGraph( new Union( graph, model.getGraph() ) );

		StmtIterator i = model.listStatements();
		if( !i.hasNext() ) {
			log.warn( "Empty ontologies are entailed by any premise document!" );
		}
		else { 
			while( i.hasNext() ) {
				Statement stmt = (Statement) i.next();
				if( !isEntailed( stmt ) ) 
					return false;
			}
		}
	    
		loader.setGraph( graph );
		
		return true;
	}

	/**
	 * Check if the given statement is entailed by the KB. 
	 * 
	 * @param stmt
	 * @return
	 * @throws UnsupportedFeatureException
	 */
	public boolean isEntailed(Statement stmt) throws UnsupportedFeatureException {
		if( log.isDebugEnabled() )
			log.debug("Check entailment " + stmt);

		boolean isEntailed = isEntailed(stmt.getSubject(), stmt.getPredicate(), stmt.getObject());


		if(!isEntailed &&  log.isDebugEnabled() ) {
			log.debug("Does not entail: (" + stmt + ")");
		}

		return isEntailed;
	}

    final public static List SKIP = Arrays.asList(new Property[] {
      RDF.first, RDF.rest,
      OWL.hasValue, 
      OWL.onProperty,
      OWL.allValuesFrom, OWL.someValuesFrom, 
      OWL.minCardinality, OWL.maxCardinality,
      OWL.cardinality, });
    
	/**
	 * Chekc if the given subject, property, object triple is entailed by the KB.
	 * 
	 * @param s Subject
	 * @param p Predicate
	 * @param ox Object
	 * @return
	 */
	public boolean isEntailed(Resource s, Property p, RDFNode ox) {
		if( SKIP.contains(p) ) {
			if( log.isDebugEnabled() ) log.debug("Skip syntax related triple");
			return true;
		}

		Resource o = (ox instanceof Resource) ? (Resource) ox : null;
		if(p.equals(RDF.type)) {
			if(o.equals(OWL.Class) || o.equals(RDFS.Class))
				return isClass(s);
//			else if(o.equals(OWL.Thing))
//				return true;
			else if(o.equals(RDF.List)) {
				if( log.isDebugEnabled() ) log.debug("Skip type triple for rdf:List");
				return true;
			}
			else if(o.equals(OWL.Restriction)) {
				if( log.isDebugEnabled() ) log.debug("Skip type triple for owl:Different");
				return true;
			}
			else if(o.equals(OWL.AllDifferent))
				throw new UnsupportedFeatureException("owl:AllDifferent is not supported");
			else if(o.equals(RDF.Property)) {
				return isProperty(s);
			}
			else if(o.equals(OWL.ObjectProperty)) {
				return isObjectProperty(s);
			}
			else if(o.equals(OWL.DatatypeProperty)) {
				return isDatatypeProperty(s);
			}
			else if(o.equals(OWL.AnnotationProperty)) {
				return isAnnotationProperty(s);
			}
			else if(o.equals(OWL.Ontology)) {
				return true;
//				return isOntology(s);
			}
			else if(o.equals(OWL.TransitiveProperty)) {
				return isTransitiveProperty(s);
			}
			else if(o.equals(OWL.SymmetricProperty)) {
				return isSymmetricProperty(s);
			}
			else if(o.equals(OWL.FunctionalProperty)) {
				return isFunctionalProperty(s);
			}
			else if(o.equals(OWL.InverseFunctionalProperty)) {
				return isInverseFunctionalProperty(s);
			}
			else {
				return isType(s, o);
			}
		}
		else if(p.equals(OWL.intersectionOf)) {
			if( log.isDebugEnabled() ) log.debug("Skip intersection of triple");
			return true;
		}
		else if(p.equals(OWL.unionOf)) {
			if( log.isDebugEnabled() ) log.debug("Skip union of triple");
			return true;
		}
		else if(p.equals(OWL.complementOf)) {
			return isComplement(s, o);
		}
		else if(p.equals(RDFS.subClassOf)) {
			return isSubClassOf(s, o);
		}
		else if(p.equals(OWL.equivalentClass)) {
			return isEquivalentClass(s, o);
		}
		else if(p.equals(OWL.disjointWith)) {
			return isDisjoint(s, o);
		}
		else if(p.equals(OWL.equivalentProperty)) {
			return isEquivalentProperty(s, o);
		}
		else if(p.equals(RDFS.subPropertyOf)) {
			return isSubPropertyOf(s, o);
		}
		else if(p.equals(OWL.inverseOf)) {
			return isInverse(s, o);
		}
		else if(p.equals(OWL.sameAs)) // || p.equals(OWL.sameIndividualAs)) {
		{
			return isSameAs(s, o);
		}
		else if(p.equals(OWL.differentFrom)) {
			return isDifferentFrom(s, o);
		}
		else if(p.equals(RDFS.domain))
			return hasDomain(s, o);
		else if(p.equals(RDFS.range))
			return hasRange(s, o);
		else if(p.equals(OWL.distinctMembers))
			throw new UnsupportedFeatureException("owl:distinctMembers is not supported yet");
		else if(p.equals(OWL.oneOf)) {
			if( log.isDebugEnabled() ) log.debug("Skip oneOf triple");
			return true;
		}
		else if(p.equals(OWL.imports))
			throw new UnsupportedFeatureException("owl:imports is not supported yet");
		else if(p.getURI().startsWith(OWL.getURI())) {
			System.err.println("Warning: " + p + " does not belong to OWL namespace");
			if(p.getLocalName().equals("differentIndividualFrom")) {
				System.err.println("Warning: Renaming the predicate to owl:differentFrom");
				return isDifferentFrom(s, o);
			}
			else if(p.getLocalName().equals("samePropertyAs")) {
				System.err.println("Warning: Renaming the predicate to owl:equivalentProperty");
				return isEquivalentProperty(s, o);
			}
			else if(p.getLocalName().equals("sameClassAs")) {
				System.err.println("Warning: Renaming the predicate to owl:equivalentClass");
				return isEquivalentClass(s, o);
			}
			else
				throw new UnsupportedFeatureException(p + " does not belong to OWL namespace");
		} else {
			return hasPropertyValue(s, p, ox);
		}

	}

	/**
	 * Returns the (named) superclasses of class c. Depending on the second parameter the resulting
	 * list will include etiher all or only the direct superclasses.
	 * 
	 * A class d is a direct superclass of c iff
	 * <ol>
	 *   <li> d is superclass of c </li> 
	 *   <li> there is no other class x such that x is superclass of c and d is superclass of x </li>
	 * </ol>
	 * The class c itself is not included in the list but all the other classes that
	 * are sameAs c are put into the list. Also note that the returned
	 * list will always have at least one element. The list will either include one other
	 * concept from the hierachy or the owl:Thing if no other class subsumes c. 
	 * By definition owl:Thing is superclass of every concept. 
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are Jena resources. 
	 */
	public Set getSuperClasses(Resource c, boolean direct) {
		return toJenaSetOfSet(kb.getSuperClasses(node2term(c), direct));
	}

	/**
	 * Return all the (named) super classes of the given class.
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are Jena resources. 
	 */
	public Set getSuperClasses(Resource c) {
		return toJenaSetOfSet(kb.getSuperClasses(node2term(c)));
	}

	/**
	 * Return all the (named) subclasses of the given class.
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are Jena resources. 
	 */
	public Set getSubClasses(Resource c) {
		return toJenaSetOfSet(kb.getSubClasses(node2term(c)));
	}
	
	/**
	 * Returns the (named) subclasses of class c. According to the second parameter the resulting
	 * list will include either all subclasses or only the direct subclasses.
	 * 
	 * A class d is a direct subclass of c iff
	 * <ol>
	 *   <li>d is subclass of c</li> 
	 *   <li>there is no other class x different from c and d such that x is subclass 
	 *   of c and d is subclass of x</li>
	 * </ol> 
	 * The class c itself is not included in the result but all the other classes that
	 * are equivalent to c are put in the result as one set. Also note that the returned
	 * list will always have at least one element. The list will either include one other
	 * concept from the hierachy or the owl:Nothing concept if no other class is subsumed by c. 
	 * By definition owl:Nothing concept is subclass of every concept. 
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are Jena resources. 
	 */
	public Set getSubClasses(Resource c, boolean direct) {
		return toJenaSetOfSet(kb.getSubClasses(node2term(c), direct));
	}	

	/**
	 * Return all the (named) equivalent classes.
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c
	 * @return A set of Jena resources
	 */
	public Set getEquivalentClasses(Resource c) {
		return toJenaSet(kb.getEquivalentClasses(node2term(c)));
		
	}

	/**
	 * Check if the given individual is an instance of the given class.
	 * 
	 * @param c
	 * @return
	 */
	public boolean isInstanceOf(Resource ind, Resource c) {
		return kb.isType(node2term(ind), node2term(c));
	}

	/**
	 * Get all the instances of the given class.
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c
	 * @return A set of Jena resource
	 */
	public Set getInstances(Resource c) {
		return toJenaSet(kb.getInstances(node2term(c)));
	}
	
	/**
	 * Returns the instances of class c. Depending on the second parameter the resulting
	 * list will include all or only the direct instances. An individual x is a direct
	 * instance of c iff x is of type c and there is no subclass d of c such that x is of
	 * type d. 
	 * 
	 * <p>*** This function will first realize the whole ontology ***</p>
	 * 
	 * @param c Class whose instances are returned
	 * @param direct if true return only the direct instances, otherwise return all the instances
	 * @return A set of Jena resources
	 */
	public Set getInstances(Resource c, boolean direct) {
		return toJenaSet(kb.getInstances(node2term(c), direct));
	}	
	
	/**
	 * Get all the (named) classes individual belongs to.
	 * 
	 * <p>*** This function will first realize the whole ontology ***</p>
	 * 
	 * @param ind An individual name
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are Jena resources. 
	 */
	public Set getTypes(Resource ind) {
		return toJenaSetOfSet(kb.getTypes(node2term(ind)));
	}

	/**
	 * Returns the (named) classes individual belongs to. Depending on the second parameter the 
	 * result will include either all types or only the direct types.
	 * 
	 * @param ind An individual name
	 * @param direct If true return only the direct types, otherwise return all types
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are Jena resources 
	 */
	public Set getTypes(Resource ind, boolean direct) {
		return toJenaSetOfSet(kb.getTypes(node2term(ind), direct));

	}

	public Resource getType(Resource ind) {
		return toJenaResource(kb.getType(node2term(ind)));
	}

	public Resource getType(Resource ind, boolean direct) {
		return toJenaResource(kb.getType(node2term(ind), direct));
	}
	
	/**
	 * List all subjects with a given property and property value.
	 * 
	 * @return Set of Jena resources.
	 */
	public Set getIndividualsWithProperty(Property p, Resource r) {
        return toJenaSet(kb.getIndividualsWithProperty( node2term(p), node2term(r) ));
	}
	
	/**
	 * Return all property values for a given property and subject value. 
	 * 
	 * @param r
	 * @param x
	 * @return List of ATermAppl objects.
	 */
	public Set getPropertyValues(Property p, RDFNode r) {
	    return toJenaSet(kb.getPropertyValues(node2term(p), node2term(r)));
	}
	
	public RDFNode getPropertyValue(Property p, RDFNode r) {
	    List values = kb.getPropertyValues(node2term(p), node2term(r));
	    
	    if(values.isEmpty())
	        return null;
	    
	    return toJenaNode( (ATermAppl) values.iterator().next() );
	}
	
	/**
	 * Convert an ATerm (or a Literal) to a Jena RDFNode.
	 * 
	 * @param term
	 * @return
	 */
	public RDFNode toJenaNode(ATermAppl term) {
	    RDFNode r = JenaUtils.makeRDFNode(term, model);
		if(r == null) 
			throw new RuntimeException("Cannot find Jena resource for term " + term);
		return r;
	}
	
	/**
	 * Convert an ATerm to a Jena resource. Use this function only if you are sure
	 * that givne term is not a literal value.
	 * 
	 * @param term
	 * @return
	 */
	public Resource toJenaResource(ATermAppl term) {
	    Resource r = JenaUtils.makeResource(term, model);
		if(r == null) 
			throw new RuntimeException("Cannot find Jena resource for term " + term);
		return r;
	}
	
	
	public Resource toJenaProperty(ATermAppl term) {
	    Resource r = JenaUtils.makeProperty(term, model);
		if(r == null) 
			throw new RuntimeException("Cannot find Jena resource for term " + term);
		return r;
	}
	
	@SuppressWarnings("unchecked")
	protected Set toJenaSetOfSet(Set set) {
		Set results = new HashSet();
		Iterator i = set.iterator();
		while(i.hasNext()) 
			results.add(toJenaSet((Set) i.next()));		

		return results;		
	}	

	/**
	 * Convert a set of ATerms (or Literals) to a set of Jena resources
	 * 
	 * @param set
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected Set toJenaSet(Collection set) {
		Set results = new HashSet();
		Iterator i = set.iterator();
		while(i.hasNext()) {
			RDFNode r = toJenaNode((ATermAppl) i.next());
			
			results.add(r);
		}

		return results;		
	}	
	
	public ResultSet execQuery( String queryStr ) throws UnsupportedQueryException {	    
	    ResultSet results = new PelletResultSet( QueryEngine.exec( queryStr, kb ), model );	    
	    
	    return results;
	}
	
	public ResultSet execQuery( com.hp.hpl.jena.query.Query sparql ) throws UnsupportedQueryException {
        ARQParser parser = (ARQParser) QueryEngine.createParser();
	    Query query = parser.parse( sparql, kb );

	    ResultSet results = new PelletResultSet( QueryEngine.exec( query ), model );	    
	    
	    return results;
	}
		
	/**
	 * @return Returns the econnEnabled.
	 */
	public boolean isEconnEnabled() {
		return econnEnabled;
	}
	
	/**
	 * @deprecated
	 */
	public void setEconnEnabled(boolean econnEnabled) {
		this.econnEnabled = econnEnabled;		
	}
	
    public OWLLoader getLoader() {
        return loader;
    }
    
    public boolean isDiscardJenaGraph() {
        return discardJenaGraph;
    }
    
    public void setDiscardJenaGraph( boolean discardJenaGraph ) {
        this.discardJenaGraph = discardJenaGraph;
    }

	public boolean isComplete(Model coModel) {
		boolean complete = true;
		//type assertions, role and class hierarchy
		
		kb.realize();
		
		Model m = this.extractModel(true);
		
		/* check whether a role is transitive and (functional or inv. fcn.) in the inferred model */
		
		
		
		for(StmtIterator i = m.listStatements(); i.hasNext(); ) {
			Statement stmt = (Statement) i.next();
			//check whether coModel contains statement stmt
			// pruner some of the stmt's
			Resource subj = stmt.getSubject();
			RDFNode obj = stmt.getObject();
			Property prop = stmt.getPredicate();
			if (subj.equals(obj) && prop.equals(OWL.equivalentClass)) continue;
			if (subj.equals(obj) && prop.equals(OWL.equivalentProperty)) continue;
			if (subj.equals(obj) && prop.equals(RDFS.subClassOf)) continue;
			if (subj.equals(obj) && prop.equals(RDFS.subPropertyOf)) continue;
			if (obj.equals(OWL.ObjectProperty)  || obj.equals(RDF.Property)) continue;
			if (subj.equals(OWL.Thing)) continue;
			if (obj.equals(OWL.Thing)) continue;			
			if (subj.equals(OWL.Nothing)) continue;
			
			if (prop.equals(RDF.type) && obj.equals(OWL.TransitiveProperty)) {
				//this property cannot be functional or inversfunctional (OWL-DL restriction)
				if (coModel.listStatements(subj, prop, OWL.InverseFunctionalProperty).hasNext()) {
					System.out.println("Property error: " + subj + " cannot be both trans and ifp!" );
				}
				if (coModel.listStatements(subj, prop, OWL.FunctionalProperty).hasNext()) {
					System.out.println("Property error: " + subj +  " cannot be both trans and fp!");
				}				
				
				
				
			}
			
			//we want to catch the unsatisfiable classes
			if (obj.equals(OWL.Nothing) && !prop.equals(RDFS.subClassOf)) continue;
			if (obj.equals(OWL.Class)) continue;						
			
			if (!coModel.listStatements(stmt.getSubject(), stmt.getPredicate(), stmt.getObject()).hasNext()) {
				System.out.println("Incomplete:" + stmt);
				complete =  false;				
			}			
		}
		
		// go through the sameAs and differentFrom assertions
		// check for every individual		
		kb.realize();
		for(Iterator i =kb.getIndividuals().iterator(); i.hasNext(); ) {
			ATermAppl ind1 = (ATermAppl) i.next();
			for(Iterator k =kb.getIndividuals().iterator(); k.hasNext(); ) {
				
				
				ATermAppl ind2 = (ATermAppl) k.next();
				
				if (ind1.equals(ind2)) continue;
				
				if (kb.isSameAs(ind1, ind2)) {
					if (!coModel.listStatements( toJenaResource(ind1), OWL.sameAs, toJenaNode(ind2)).hasNext()) {
						System.out.println("Incomplete:" + ind1 + " sameAs " + ind2);
						complete =  false;		
					} 
				} 
				if (kb.isDifferentFrom(ind1, ind2)) {
					if (!coModel.listStatements( toJenaResource(ind1), OWL.differentFrom, toJenaNode(ind2)).hasNext()) {
						System.out.println("Incomplete:" + ind1 + " differentfrom " + ind2);
						complete =  false;		
					} 
				}	
			}		
		}
				
		return complete;
	}
}
