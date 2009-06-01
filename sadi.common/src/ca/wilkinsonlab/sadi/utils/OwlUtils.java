package ca.wilkinsonlab.sadi.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class OwlUtils 
{
	private static final Log log = LogFactory.getLog( OwlUtils.class );
	
	/**
	 * Load the definitions for each predicate in 'predicates', by loading the
	 * corresponding OWL files into the Model.  (By 'definition', I mean
	 * whether it is a data property or an object property, what the equivalent
	 * properties are, and what the inverse properties are).
	 * 
	 * @param model The model to load the definitions into.
	 * @param predicates The predicates we need definitions for.
	 */
	public static void loadOWLFilesForPredicates(Model model, Collection<String> predicateURIs)
	{
		Set<String> visited = new HashSet<String>();
		for (String predicate : predicateURIs) {
			log.trace("checking predicate " + predicate);
			String source = getOWLFileForPredicate(predicate);
			if (visited.contains(source))
				continue;
			
			log.trace("reading ontology from " + source);
			model.read(source);
			visited.add(source);
		}
	} 

	private static String getOWLFileForPredicate(String predicateURI)
	{
		return StringUtils.substringBeforeLast(predicateURI, "#");
	}

	/* I've never used or tested this method, but I may need it in the future.
	 * This method is like "loadOWLFilesForPredicates", but it doesn't 
	 * assume that all properties referenced in an OWL file are necessarily defined
	 * in that same file.  
	 * 
	 * For example, in "predicates.owl" (the OWL file where I define my ad-hoc
	 * predicates like "hasProteinSequence"), I might assert that 
	 * "http://someontology.com/ontology#hasSeq" is an equivalent property
	 * to "http://cardioshare.biordf.net/cardioSHARE/predicates.owl#hasProteinSequence",
	 * but not import "http://someontology.com/ontology".   This method
	 * would detect that "hasSeq" isn't defined and load "http://someontology.com/ontology"
	 * into the knowledgebase automatically.  It would then look in 
	 * "http://someontology.com/ontology" to see if any referenced 
	 * properties there were not defined, load the requisite OWL files, and so on.
	 * 
	 * However: Referencing properties (or classes) without importing the ontology
	 * that defines them is incorrect usage of OWL.  In fact, Protege won't
	 * let you do this.
	 *
	static boolean loadOWLFilesForPredicatesSmart(Set<String> predicates, DynamicKnowledgeBase kb)
	{
		Set<String> loadedOWLURIs = new HashSet<String>();
		Set<String> failedOWLURIs = new HashSet<String>();
		Set<String> failedPredURIs = new HashSet<String>();
		
		//------------------------------------------------------
		// Hack: Force loading of 'predicates.owl' until
		// I put it up at http://cardioshare.biordf.net/cardioSHARE/predicates.owl
		//------------------------------------------------------
		
		if(!loadOWLFile("file:///home/ben/Programming/workspace/cardioSHARE/predicates.owl", kb))
			return false;
		
		loadedOWLURIs.add("http://cardioshare.biordf.net/cardioSHARE/predicates.owl");
		
		Set<String> unvisitedPredicates = new HashSet<String>();
		
		boolean bUnvisitedPredicates = true;
		
		while(bUnvisitedPredicates) 
		{

			bUnvisitedPredicates = false;

			//----------------------------------------------------
			// Determine which predicates in the KnowledgeBase
			// have undefined types.  (Every predicate is either
			// a data property or an object property).
			//----------------------------------------------------
			
			unvisitedPredicates.clear();
			
			for(ATermAppl prop : kb.getProperties())
			{
				if(kb.getPropertyType(prop) == Role.UNTYPED && 
						!failedPredURIs.contains(prop.toString()))
				{
					unvisitedPredicates.add(prop.toString());
					bUnvisitedPredicates = true;
				}
			}
			
			for(Iterator<String> it = unvisitedPredicates.iterator(); it.hasNext();)
			{

				String pred = it.next();
				String OWLfile = getOWLFileForPredicate(pred);
				
				boolean bPredFailed = false; 
				
				if(failedOWLURIs.contains(OWLfile))
				{
					// We've already tried to load this OWL file, so skip it.
					bPredFailed = true;
				}
				else if(loadedOWLURIs.contains(OWLfile))
				{
					log.warn(OWLfile + " does not define predicate " + pred);
					bPredFailed = true;
				}
				else if(!loadOWLFile(OWLfile, kb))
				{
					log.error("Failed to load OWL file " + OWLfile + " into knowledgebase.");
					failedOWLURIs.add(OWLfile);
					bPredFailed = true;
				}
				
				if(bPredFailed)
				{
					
					log.warn("Failed to determine type for predicate " + pred);
					
					// At this point we've failed to find a definition
					// for the predicate, and there's no other OWL files
					// we can look in.
					//
					// Ideally, what I'd like to do here is remove the  
					// predicate from the knowledgebase entirely.  But neither
					// the KnowledgeBase nor the RBox class have a method for 
					// removing a property.  
					//
					// I am hesitant to patch Pellet unless absolutely
					// necessary, so instead I'm using a temporary Set to keep 
					// track of URIs we've failed to load a definition for.

					failedPredURIs.add(pred);
					
				}
				
			}
			
		} 
		
		return true;
	} 
	*/
	
	/**
	 * Resolve the specified URI and load the resulting statements
	 * into the specified OntModel.
	 * @param model the OntModel
	 * @param uri the URI
	 */
	public static void loadOntologyForUri(OntModel model, String uri)
	{
		log.debug(String.format("loading ontology for %s", uri));
		
		/* TODO check to see if the document manager is actually preventing
		 * duplicate URIs from being loaded...
		 */
		model.read( StringUtils.substringBefore( uri, "#" ) );
	}
	
	/**
	 * Return the set of properties the OWL class identified by a URI has restrictions on.
	 * The ontology containing the OWL class (and any referenced imports) will be fetched
	 * and processed.
	 * @param classUri the URI of the OWL class
	 * @return the set of properties the OWL class has restrictions on
	 */
	public static Set<OntProperty> decompose(String classUri)
	{
		// do we need more reasoning here?
		OntModel inf = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		inf.read( StringUtils.substringBefore( classUri, "#" ) );
		return decompose( inf.getOntClass( classUri ) );
	}
	
	/**
	 * Return the set of properties an OWL class has restrictions on.
	 * @param clazz the OWL class
	 * @return the set of properties the OWL class has restrictions on
	 */
	public static Set<OntProperty> decompose(OntClass clazz)
	{
		if ( clazz == null )
			throw new IllegalArgumentException("argument is null");
		
		return new Decomposer(clazz).getProperties();
	}
	
	/**
	 * A class used by the decompose methods above.
	 * @author Luke McCarthy
	 */
	private static class Decomposer
	{
		Set<OntProperty> accumulator;
		Set<OntClass> seen;
		
		public Decomposer(OntClass clazz)
		{
			accumulator = new HashSet<OntProperty>();
			seen = new HashSet<OntClass>();
			decompose(clazz);
		}
		
		public Set<OntProperty> getProperties()
		{
			return accumulator;
		}
		
		private void decompose(OntClass clazz)
		{
			if ( seen.contains(clazz) )
				return;
			else
				seen.add(clazz);
			
			/* base case: is this a property restriction? if so, just grab
			 * the property and return; the shortcut return could be an error
			 * if property restrictions can be subclasses of other things...
			 */
			if ( clazz.isRestriction() ) {
				accumulator.add( clazz.asRestriction().getOnProperty() );
				return;
			}

			/* extended case: is this a composition of several classes? if
			 * so, visit all of them...
			 */
			if ( clazz.isUnionClass() ) {
				for (Iterator i = clazz.asUnionClass().listOperands(); i.hasNext(); )
					decompose((OntClass)i.next());
			} else if ( clazz.isIntersectionClass() ) {
				for (Iterator i = clazz.asIntersectionClass().listOperands(); i.hasNext(); )
					decompose((OntClass)i.next());
			} else if ( clazz.isComplementClass() ) {
				for (Iterator i = clazz.asComplementClass().listOperands(); i.hasNext(); )
					decompose((OntClass)i.next());
			}
			
			/* recursive case: visit equivalent and super classes...
			 */
			for (Iterator i = clazz.listEquivalentClasses(); i.hasNext(); )
				decompose((OntClass)i.next());
			for (Iterator i = clazz.listSuperClasses(); i.hasNext(); )
				decompose((OntClass)i.next());
		}
	}
}
