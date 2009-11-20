package ca.wilkinsonlab.sadi.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.common.Config;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OwlUtils 
{
	private static final Log log = LogFactory.getLog( OwlUtils.class );
	
	/**
	 * Returns a human-readable label for the specified resource.  The value
	 * returned will be the first of the following that is present:
	 * 	getLabel() [usually the value of the rdfs:label property]
	 * 	getLocalName() [usually the last path element of the URI]
	 * 	toString()
	 * @param resource
	 * @return a human-readable label for the specified resource
	 */
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
	 * Resolve the specified URI and load the resulting statements
	 * into the specified OntModel. Resolve imports and relevant
	 * isDefinedBy URIs as well.
	 * TODO we should probably make the import/isDefinedBy behaviour
	 * configurable, including an option to load only the relevant
	 * parts of each ontology (using ResourceUtils.reachableClosure)
	 * @param model the OntModel
	 * @param uri the URI
	 */
	public static void loadOntologyForUri(OntModel model, String uri)
	{
		log.debug(String.format("loading ontology for %s", uri));
		
		/* TODO check to see if the document manager is actually preventing
		 * duplicate URIs from being loaded...
		 */
		String ontologyUri = StringUtils.substringBefore( uri, "#" );
		log.trace(String.format("reading ontology from %s", ontologyUri));
		try {
			model.read( ontologyUri );
		} catch (Exception e) {
			log.error(String.format("error reading ontology from %s", uri), e);
		}
		
		/* Michel Dumontier's predicates resolve to a minimal definition that
		 * doesn't include the inverse relationship, so we need to resolve
		 * the ontology that contains the complete definition...
		 * We extract to a list here to prevent concurrent modification exceptions...
		 */
		for (Statement statement: model.getResource(uri).listProperties(RDFS.isDefinedBy).toList()) {
			if (statement.getObject().isURIResource()) {
				ontologyUri = statement.getResource().getURI();
				log.trace(String.format("reading isDefinedBy ontology from %s", ontologyUri));
				model.read(ontologyUri);
			}
		}
	}
	
	/**
	 * Return the OntProperty with the specified URI, resolving it and
	 * loading the resulting ontology into the model if necessary.
	 * @param model the OntModel
	 * @param uri the URI
	 * @return the OntProperty
	 */
	public static OntProperty getOntPropertyWithLoad(OntModel model, String uri)
	{
		OntProperty p = model.getOntProperty(uri);
		if (p != null)
			return p;
		
		loadOntologyForUri(model, uri);
		return model.getOntProperty(uri);
	}
	
	/**
	 * Return the OntClass with the specified URI, resolving it and
	 * loading the resulting ontology into the model if necessary.
	 * @param model the OntModel
	 * @param uri the URI
	 * @return the OntClass
	 */
	public static OntClass getOntClassWithLoad(OntModel model, String uri)
	{
		OntClass c = model.getOntClass(uri);
		if (c != null)
			return c;
		
		loadOntologyForUri(model, uri);
		return model.getOntClass(uri);
	}
	
	/**
	 * Return the OntResource with the specified URI, resolving it and
	 * loading the resulting ontology into the model if necessary.
	 * @param model the OntModel
	 * @param uri the URI
	 * @return the OntResource
	 */
	public static OntResource getOntResourceWithLoad(OntModel model, String uri)
	{
		OntResource r = model.getOntResource(uri);
		if (r != null)
			return r;
		
		loadOntologyForUri(model, uri);
		return model.getOntResource(uri);
	}
	
	/**
	 * Return the set of properties the OWL class identified by a URI has restrictions on.
	 * The ontology containing the OWL class (and any referenced imports) will be fetched
	 * and processed.
	 * @param classUri the URI of the OWL class
	 * @return the set of properties the OWL class has restrictions on
	 */
	public static Set<OntProperty> listRestrictedProperties(String classUri)
	{
		// TODO do we need more reasoning here?
		OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		OntClass clazz = getOntClassWithLoad( model, classUri );
		return listRestrictedProperties( clazz );
	}
	
	/**
	 * Return the set of properties an OWL class has restrictions on.
	 * @param clazz the OWL class
	 * @return the set of properties the OWL class has restrictions on
	 */
	public static Set<OntProperty> listRestrictedProperties(OntClass clazz)
	{
		PropertyEnumerationVisitor visitor = new PropertyEnumerationVisitor();
		decompose(clazz, visitor);
		return visitor.listProperties();
	}
	
	/**
	 * Return the minimal RDF required for the specified individual to satisfy
	 * the specified class description. Note that the RDF will not be complete
	 * if the individual is not in the same OntModel as the OntClass or does
	 * not satisfy the class requirements. 
	 * @param individual the individual
	 * @param asClass the class
	 * @return a new in-memory model containing the minimal RDF
	 */
	public static Model getMinimalModel(Resource individual, OntClass asClass)
	{
		Model model = ModelFactory.createDefaultModel();
		MinimalModelVisitor visitor = new MinimalModelVisitor(model, individual, asClass);
		decompose(asClass, visitor);
		return model;
	}
	
	/**
	 * Visit each property restriction of the specified OWL class with the
	 * specified PropertyRestrictionVisitor.
	 * @param clazz the class
	 * @param visitor the visitor
	 */
	public static void decompose(OntClass clazz, PropertyRestrictionVisitor visitor)
	{
		if ( clazz == null )
			throw new IllegalArgumentException("class to decompose cannot be null");
		
		new VisitingDecomposer(visitor).decompose(clazz);
	}
	
	/**
	 * PropertyRestrictionVisitor is used by OwlUtils.decompose.
	 * @author Luke McCarthy
	 */
	public static interface PropertyRestrictionVisitor
	{
		void onProperty(OntProperty onProperty);
		void hasValue(OntProperty onProperty, RDFNode hasValue);
		void valuesFrom(OntProperty onProperty, OntResource valuesFrom);
	}
	
	private static class VisitingDecomposer
	{
		private PropertyRestrictionVisitor visitor;
		private Set<OntClass> visited;
		
		private static final int IGNORE = 0x0;
		private static final int CREATE = 0x1;
		private static final int RESOLVE = 0x2;
		private int undefinedPropertiesPolicy;
		
		public VisitingDecomposer(PropertyRestrictionVisitor visitor)
		{
			this.visitor = visitor;
			this.visited = new HashSet<OntClass>();
			
			String s = Config.getConfiguration().getString("sadi.decompose.undefinedPropertiesPolicy", "create");
			undefinedPropertiesPolicy = IGNORE;
			if (s.equalsIgnoreCase("create"))
				undefinedPropertiesPolicy = CREATE;
			else if (s.equalsIgnoreCase("resolve"))
				undefinedPropertiesPolicy = RESOLVE;
			else if (s.equalsIgnoreCase("resolveThenCreate"))
				undefinedPropertiesPolicy = RESOLVE | CREATE;
		}
		
		public void decompose(OntClass clazz)
		{
			if ( visited.contains(clazz) )
				return;
			else
				visited.add(clazz);
			
			/* base case: is this a property restriction?
			 */
			if ( clazz.isRestriction() ) {
				Restriction restriction = clazz.asRestriction();
				
				/* Restriction.onProperty throws an exception if the property
				 * isn't defined in the ontolog; this is technically correct,
				 * but it's often better for us to just add the property...
				 */
				OntProperty onProperty = null;
				try {
					onProperty = restriction.getOnProperty();
				} catch (ConversionException e) {
					RDFNode p = restriction.getPropertyValue(OWL.onProperty);
					log.info(String.format("unknown property %s in class %s", p, clazz));
					if (p.isURIResource()) {
						String uri = ((Resource)p.as(Resource.class)).getURI();
						onProperty = resolveUndefinedPropery(clazz.getOntModel(), uri, undefinedPropertiesPolicy);
					} else {
						// TODO call a new method on PropertyRestrictionVisitor?
						log.warn(String.format("found non-URI property %s in clsas %s", p, clazz));
					}
				}
				
				if (onProperty != null) {
					if ( restriction.isAllValuesFromRestriction() ) {
						Resource valuesFrom = restriction.asAllValuesFromRestriction().getAllValuesFrom();
						OntResource ontValuesFrom = onProperty.getOntModel().getOntResource(valuesFrom);
						visitor.valuesFrom(onProperty, ontValuesFrom);
					} else if ( restriction.isSomeValuesFromRestriction() ) {
						Resource valuesFrom = restriction.asSomeValuesFromRestriction().getSomeValuesFrom();
						OntResource ontValuesFrom = onProperty.getOntModel().getOntResource(valuesFrom);
						visitor.valuesFrom(onProperty, ontValuesFrom);			
					} else if ( restriction.isHasValueRestriction() ) {
						RDFNode hasValue = restriction.asHasValueRestriction().getHasValue();
						visitor.hasValue(onProperty, hasValue);
					} else {
						visitor.onProperty(onProperty);
					}
				}
			}

			/* extended case: is this a composition of several classes? if
			 * so, visit all of them...
			 */
			if ( clazz.isUnionClass() ) {
				for (Iterator<?> i = clazz.asUnionClass().listOperands(); i.hasNext(); )
					decompose((OntClass)i.next());
			} else if ( clazz.isIntersectionClass() ) {
				for (Iterator<?> i = clazz.asIntersectionClass().listOperands(); i.hasNext(); )
					decompose((OntClass)i.next());
			} else if ( clazz.isComplementClass() ) {
				for (Iterator<?> i = clazz.asComplementClass().listOperands(); i.hasNext(); )
					decompose((OntClass)i.next());
			}
			
			/* recursive case: visit equivalent and super classes...
			 * note that we can't use an iterator because we might add
			 * properties to the ontology if they're undefined, which can
			 * trigger a ConcurrentModificationException.
			 */
			for (Object subclass: clazz.listEquivalentClasses().toSet())
				decompose((OntClass)subclass);
			for (Object superclass: clazz.listSuperClasses().toSet())
				decompose((OntClass)superclass);
		}

		private static OntProperty resolveUndefinedPropery(OntModel model, String uri, int undefinedPropertiesPolicy)
		{
			if ((undefinedPropertiesPolicy & RESOLVE) != 0) {
				log.debug(String.format("resolving property %s during decomposition", uri));
				return OwlUtils.getOntPropertyWithLoad(model, uri);
			}
			if ((undefinedPropertiesPolicy & CREATE) != 0 && (model.getOntProperty(uri) == null)) {
				log.debug(String.format("creating property %s during decomposition", uri));
				return model.createOntProperty(uri);
			}
			return model.getOntProperty(uri);
		}
	}
	
	private static class PropertyEnumerationVisitor implements PropertyRestrictionVisitor
	{
		private Set<OntProperty> properties;
		
		public PropertyEnumerationVisitor()
		{
			properties = new HashSet<OntProperty>();
		}
		
		public void onProperty(OntProperty onProperty)
		{
			properties.add(onProperty);
		}

		public void hasValue(OntProperty onProperty, RDFNode hasValue)
		{
			onProperty(onProperty);
		}

		public void valuesFrom(OntProperty onProperty, OntResource valuesFrom)
		{
			properties.add(onProperty);
		}

		public Set<OntProperty> listProperties()
		{
			return properties;
		}
	}
	
	private static class MinimalModelVisitor implements PropertyRestrictionVisitor
	{
		private Model model;
		private Resource subject;
		private Set<String> visited;
		
		public MinimalModelVisitor(Model model, Resource subject, OntClass asClass)
		{
			this(model, subject, asClass, new HashSet<String>());
		}
		
		public MinimalModelVisitor(Model model, Resource subject, OntClass asClass, Set<String> visited)
		{
			log.trace(String.format("visiting %s as %s", subject, asClass));
			
			this.model = model;
			this.subject = subject;
			this.visited = visited;
			
			/* if the individual is explicitly declared as a member of the 
			 * target class, add that type statement to the model...
			 */
			if (subject.hasProperty(RDF.type, asClass))
				model.add(subject, RDF.type, asClass);
			
			/* remember that we've visited this individual as this class
			 * in order to prevent cycles where the object of one of our
			 * triples has us as the object of one of theirs...
			 */
			visited.add(getHashKey(subject, asClass));
		}

		public void onProperty(OntProperty onProperty)
		{
			/* TODO there may be some cases where we don't have to add all
			 * values of the restricted property, but this shouldn't be too
			 * bad...
			 */
			model.add(subject.listProperties(onProperty));
		}

		public void hasValue(OntProperty onProperty, RDFNode hasValue)
		{
			if (subject.hasProperty(onProperty, hasValue)) {
				model.add(subject, onProperty, hasValue);
			}
		}

		public void valuesFrom(OntProperty onProperty, OntResource valuesFrom)
		{
			/* for (all/some)ValuesFrom restrictions, we need to add enough
			 * information to determine the class membership of the objects of
			 * the statments as well...
			 * (extract to list to avoid ConcurrentModificationException)
			 */
			for (Statement statement: subject.listProperties(onProperty).toList()) {
				/* always add the statement itself; this covers the case where
				 * valuesFrom is a datatype or data range...
				 */
				model.add(statement);
				
				/* if valuesFrom is a class and the object of the statement
				 * isn't a literal, recurse...
				 */
				if (valuesFrom.isClass() && statement.getObject().isResource()) {
					Resource resource = statement.getResource();
					OntClass clazz = valuesFrom.asClass();
					if (!visited.contains(getHashKey(resource, clazz)))
						decompose(valuesFrom.asClass(), new MinimalModelVisitor(model, resource, clazz, visited));
				}
			}
		}
		
		private static String getHashKey(Resource individual, Resource asClass)
		{
			return String.format("%s %s", 
					individual.isURIResource() ? individual.getURI() : individual.getId(),
					asClass.isURIResource() ? asClass.getURI() : asClass.getId()
			);
		}
	}
}
