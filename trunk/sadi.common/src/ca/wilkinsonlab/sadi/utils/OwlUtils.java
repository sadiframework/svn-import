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
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class OwlUtils 
{
	private static final Log log = LogFactory.getLog( OwlUtils.class );
	
	public static String getLabel(OntResource resource)
	{
		if (resource == null)
			return "null";
		
		String label = resource.getLabel(null);
		if (label != null)
			return label;
		
		if (resource.isURIResource()) {
			/* there seems some kind of bug in getLocalName() in that it loses
			 * some of the text after a #...
			 */
			if (resource.isURIResource()) {
				if (resource.getURI().contains("#"))
					return StringUtils.substringAfterLast(resource.getURI(), "#");
				else
					return resource.getLocalName();
			} else {
				return resource.getLocalName();
			}
		}
		
		if (resource.isClass()) {
			OntClass clazz = resource.asClass();
			if (clazz.isRestriction()) {
				Restriction restriction = clazz.asRestriction();
				try {
					return String.format("restriction on %s", restriction.getOnProperty());
				} catch (Exception e) {
					log.warn(e.getMessage());
				}
			}
		}
		
		return resource.toString();
	}
	
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
			try {
				model.read(source);
			}
			catch(Exception e) {
				log.warn("Unable to load " + source, e);
			}
			visited.add(source);
		}
	} 

	private static String getOWLFileForPredicate(String predicateURI)
	{
		return StringUtils.substringBeforeLast(predicateURI, "#");
	}
	
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
