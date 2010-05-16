package ca.wilkinsonlab.sadi.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.common.Config;
import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.utils.graph.BreadthFirstIterator;
import ca.wilkinsonlab.sadi.utils.graph.OpenGraphIterator;
import ca.wilkinsonlab.sadi.utils.graph.SearchNode;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OwlUtils 
{
	private static final Logger log = Logger.getLogger( OwlUtils.class );
	private static final OntModel owlModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
	
	/**
	 * Return an OntModel view of the base OWL ontology.  This is useful
	 * for things like getting an OntClass view of OWL.Thing, etc.
	 * @return
	 */
	public static OntModel getOWLModel()
	{
		return owlModel;
	}
	
	/**
	 * Returns a human-readable label for the specified resource. 
	 * The value returned will be the first of the following that is
	 * present:
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
	 * Returns the range of the specified property.  Jena seems to include
	 * all of the ancestor classes of any specified range (all the way back
	 * to rdfs:resource) in the iterator it returns, so it's not actually
	 * useful for asking if a given resource is in the property's range.
	 * This method constructs a tree of the classes in the range returned
	 * by Jena and returns an OWL union class of the leaf nodes.
	 * @param p the property
	 * @return the range of the specified property as a single OWL class
	 */
	public static OntClass getUsefulRange(OntProperty p)
	{
		if (p.getRange() == null)
			return getDefaultRange(p);
		
		Set<OntClass> ancestors = new HashSet<OntClass>();
		List<OntClass> range = new ArrayList<OntClass>();
		for (Iterator<? extends OntResource> i = p.listRange(); i.hasNext(); ) {
			OntResource r = (OntResource)i.next();
			if (r.isClass()) {
				OntClass c = r.asClass();
				range.add(c);
				
				/* TODO does listSuperClasses() return all ancestors in all reasoners,
				 * or just in Jena? also, do any reasoners return the class itself?
				 * Either of those will be a problem...
				 */
				for (Iterator<? extends OntClass> j = c.listSuperClasses(); j.hasNext(); ) {
					ancestors.add(j.next());
				}
			}
		}
		
		log.debug("range before pruning is " + range);
		log.debug("ancestors to prune are " + ancestors);
		for (Iterator<OntClass> i = range.iterator(); i.hasNext(); ) {
			OntClass c = i.next();
			if (ancestors.contains(c))
				i.remove();
		}
		
		if (range.size() > 1) {
			String unionUri = p.getURI().concat("-range");
			log.debug(String.format("creating union class %s from %s", unionUri, range));
			OntModel model = p.getOntModel();
			RDFList unionList = model.createList();
			for (OntClass c: range) {
				unionList = unionList.with(c);
			}
			return model.createUnionClass(unionUri, unionList);
		} else if (range.size() == 1) {
			log.debug("pruned range to single class " + range);
			return range.get(0);
		} else {
			return getDefaultRange(p);
		}
	}

	private static OntClass getDefaultRange(OntProperty p)
	{
		if (p.isDatatypeProperty())
			return p.getOntModel().getOntClass(RDFS.Literal.getURI());
		else if (p.isObjectProperty())
			return p.getOntModel().getOntClass(OWL.Thing.getURI());
		else
			return p.getOntModel().getOntClass(RDFS.Resource.getURI());
	}
	
	/**
	 * Resolve the specified URI and load the resulting statements into the
	 * specified OntModel. Resolve imports and relevant isDefinedBy URIs as
	 * well.
	 * TODO we should probably make the import/isDefinedBy behaviour
	 * configurable, including an option to load only the relevant
	 * parts of each ontology (using ResourceUtils.reachableClosure)
	 * @param model the OntModel
	 * @param uri the URI
	 */
	public static void loadOntologyForUri(OntModel model, String uri) throws SADIException
	{
		log.debug(String.format("loading ontology for %s", uri));
		
		String ontologyUri = StringUtils.substringBefore( uri, "#" );
		if (model.hasLoadedImport(ontologyUri)) {
			log.trace(String.format("skipping previously loaded ontology %s", ontologyUri));
			return;
		}
		
		log.trace(String.format("reading ontology from %s", ontologyUri));
		try {
			model.read( ontologyUri );
		} catch (Exception e) {
			if (e instanceof SADIException)
				throw (SADIException)e;
			else
				throw new SADIException(e.toString(), e);
		}
		
		/* Michel Dumontier's predicates resolve to a minimal definition that
		 * doesn't include the inverse relationship, so we need to resolve
		 * the ontology that contains the complete definition...
		 * We extract to a list here to prevent concurrent modification exceptions...
		 */
		for (Statement statement: model.getResource(uri).listProperties(RDFS.isDefinedBy).toList()) {
			if (statement.getObject().isURIResource()) {
				loadOntologyForUri(model, statement.getResource().getURI());
			}
		}
	}
	
	public static void loadMinimalOntologyForUri(Model model, String uri) throws SADIException 
	{
		String ontologyUri = StringUtils.substringBefore(uri, "#");
		loadMinimalOntologyForUri(model, ontologyUri, uri);
	}
	
	public static void loadMinimalOntologyForUri(Model model, String ontologyUri, String uri) throws SADIException
	{
		loadMinimalOntologyForUri(model, ontologyUri, uri, new HashSet<OntologyUriPair>());
	}

	protected static void loadMinimalOntologyForUri(Model model, String ontologyUri, String uri, Set<OntologyUriPair> visitedUris) throws SADIException
	{
		OntologyUriPair ontologyUriPair = new OntologyUriPair(ontologyUri, uri);
		if(visitedUris.contains(ontologyUriPair)) {
			log.debug(String.format("skipping previously loaded uri %s from %s", uri, ontologyUri));
			return;
		}

		log.debug(String.format("loading minimal ontology for %s from %s", uri, ontologyUri));
		visitedUris.add(ontologyUriPair);
		
		try {
			OntModel wholeOntology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
			wholeOntology.read(ontologyUri);

			Model minimalOntology = getMinimalOntologyFromModel(wholeOntology, uri);
			
			/* Michel Dumontier's predicates resolve to a minimal definition that
			 * doesn't include the inverse relationship, so we need to resolve
			 * the ontology that contains the complete definition...
			 * We extract to a list here to prevent concurrent modification exceptions...
			 */
			for (Statement statement: minimalOntology.getResource(uri).listProperties(RDFS.isDefinedBy).toList()) {
				Resource s = statement.getSubject();
				RDFNode o = statement.getObject();
				if (s.isURIResource() && o.isURIResource()) {
					log.debug(String.format("following (%s, rdfs:isDefinedBy, %s)", s, o));
					loadMinimalOntologyForUri(minimalOntology, ((Resource)o).getURI(), s.getURI(), visitedUris);
				}
			}			
			model.add(minimalOntology);
		} catch(Exception e) {
			if (e instanceof SADIException)
				throw (SADIException)e;
			else
				throw new SADIException(e.toString(), e);			
		}
	}
	
	protected static Model getMinimalOntologyFromModel(Model sourceModel, String uriInSourceModel) 
	{
		Model minimalOntology = ModelFactory.createMemModelMaker().createFreshModel();
		Resource root = sourceModel.getResource(uriInSourceModel);
		OpenGraphIterator<Resource> i = new BreadthFirstIterator<Resource>(new MinimalOntologySearchNode(sourceModel, minimalOntology, root));
		// we are only interested in the side effect of the iteration, 
		// which is to load statements about each of the visited resources
		// into the target model (minimalOntology)
		i.iterate();
		return minimalOntology;
	}
	
	/**
	 * Return the OntProperty with the specified URI, resolving it and
	 * loading the resulting ontology into the model if necessary.
	 * @param model the OntModel
	 * @param uri the URI
	 * @return the OntProperty
	 */
	public static OntProperty getOntPropertyWithLoad(OntModel model, String uri) throws SADIException
	{
		OntProperty p = model.getOntProperty(uri);
		if (p != null)
			return p;
		
		loadMinimalOntologyForUri(model, uri);
		return model.getOntProperty(uri);
	}
	
	/**
	 * Return the OntClass with the specified URI, resolving it and loading
	 * the resolved ontology into the model if it is not already there.
	 * @param model the OntModel
	 * @param uri the URI
	 * @return the OntClass
	 */
	public static OntClass getOntClassWithLoad(OntModel model, String uri) throws SADIException
	{
		OntClass c = model.getOntClass(uri);
		if (c != null)
			return c;
		
		loadMinimalOntologyForUri(model, uri);
		return model.getOntClass(uri);
	}
	
	/**
	 * Return the OntResource with the specified URI, resolving it and
	 * loading the resulting ontology into the model if necessary.
	 * @param model the OntModel
	 * @param uri the URI
	 * @return the OntResource
	 */
	public static OntResource getOntResourceWithLoad(OntModel model, String uri) throws SADIException
	{
		OntResource r = model.getOntResource(uri);
		if (r != null)
			return r;
		
		loadOntologyForUri(model, uri);
		return model.getOntResource(uri);
	}
	
	/**
	 * Returns a description of the specified OWL restriction
	 * @param r the OWL restriction
	 * @return a description of the OWL restriction
	 */
	public static String toString(Restriction r)
	{
		String type;
		if (r.isAllValuesFromRestriction())
			type = "allValuesFrom " + r.asAllValuesFromRestriction().getAllValuesFrom();
		else if (r.isSomeValuesFromRestriction())
			type = "someValuesFrom " + r.asSomeValuesFromRestriction().getSomeValuesFrom();
		else if (r.isCardinalityRestriction())
			type = (r.asCardinalityRestriction().isMinCardinalityRestriction() ?
					"minCardinality" : "maxCardinality") + r.asCardinalityRestriction().getCardinality();
		else if (r.isHasValueRestriction())
			type = "hasValue " + r.asHasValueRestriction().getHasValue();
		else
			type = r.getClass().getSimpleName();
		
		return String.format("%s on %s", type, r.getOnProperty());
	}
	
	/**
	 * Return the set of properties the OWL class identified by a URI has restrictions on.
	 * The ontology containing the OWL class (and any referenced imports) will be fetched
	 * and processed.
	 * @param classUri the URI of the OWL class
	 * @return the set of properties the OWL class has restrictions on
	 */
	public static Set<OntProperty> listRestrictedProperties(String classUri) throws SADIException
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
		return visitor.properties;
	}
	
	public static Set<Restriction> listRestrictions(OntClass clazz)
	{
		RestrictionEnumerationVisitor visitor = new RestrictionEnumerationVisitor();
		decompose(clazz, visitor);
		return visitor.restrictions;
	}
	
	public static Set<Restriction> listRestrictions(OntClass clazz, OntClass relativeTo)
	{
		Set<Restriction> base = listRestrictions(relativeTo);
		Set<Restriction> relative = listRestrictions(clazz);
		relative.removeAll(base);
		return relative;
	}
	
	public static OntClass getValuesFromAsClass(Restriction restriction)
	{
		OntResource valuesFrom = getValuesFrom(restriction);
		if (valuesFrom.isClass())
			return valuesFrom.asClass();
		else
			return null;
	}
	
	public static OntResource getValuesFrom(Restriction restriction)
	{
		if (restriction.isAllValuesFromRestriction()) {
			return restriction.getOntModel().getOntResource(restriction.asAllValuesFromRestriction().getAllValuesFrom());
		} else if (restriction.isSomeValuesFromRestriction()) {
			return restriction.getOntModel().getOntResource(restriction.asSomeValuesFromRestriction().getSomeValuesFrom());
		} else {
			return OwlUtils.getUsefulRange(restriction.getOnProperty());
		}
	}
	
	public static Set<OntProperty> getEquivalentProperties(OntProperty p)
	{
		/* in some reasoners, listEquivalentProperties doesn't include the
		 * property itself; also, some reasoners return an immutable list here,
		 * so we need to create our own copy (incidentally solving an issue
		 * with generics...)
		 */
		log.trace(String.format("finding all properties equivalent to %s", p));
		Set<OntProperty> equivalentProperties = new HashSet<OntProperty>();
		for (OntProperty q: p.listEquivalentProperties().toList()) {
			log.trace(String.format("found equivalent property %s", q));
			equivalentProperties.add(q);
		}
		log.trace(String.format("adding original property %s", p));
		equivalentProperties.add(p);
		
		return equivalentProperties;
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
		void hasRestriction(Restriction restriction);
	}
	
	public static abstract class PropertyRestrictionAdapter implements PropertyRestrictionVisitor
	{
		public void hasRestriction(Restriction restriction)
		{
			OntProperty onProperty = restriction.getOnProperty();
			if (onProperty != null) {
				if ( restriction.isAllValuesFromRestriction() ) {
					Resource valuesFrom = restriction.asAllValuesFromRestriction().getAllValuesFrom();
					OntResource ontValuesFrom = onProperty.getOntModel().getOntResource(valuesFrom);
					valuesFrom(onProperty, ontValuesFrom);
				} else if ( restriction.isSomeValuesFromRestriction() ) {
					Resource valuesFrom = restriction.asSomeValuesFromRestriction().getSomeValuesFrom();
					OntResource ontValuesFrom = onProperty.getOntModel().getOntResource(valuesFrom);
					valuesFrom(onProperty, ontValuesFrom);			
				} else if ( restriction.isHasValueRestriction() ) {
					RDFNode hasValue = restriction.asHasValueRestriction().getHasValue();
					hasValue(onProperty, hasValue);
				} else {
					onProperty(onProperty);
				}
			}
		}
		
		public abstract void onProperty(OntProperty onProperty);
		public abstract void hasValue(OntProperty onProperty, RDFNode hasValue);
		public abstract void valuesFrom(OntProperty onProperty, OntResource valuesFrom);
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
			
			/* bottom out explicitly at owl:Thing, or we'll have problems when
			 * we enumerate equivalent classes...
			 */
			if ( clazz.equals( OWL.Thing ) )
				return;
			
			/* base case: is this a property restriction?
			 */
			if ( clazz.isRestriction() ) {
				Restriction restriction = clazz.asRestriction();
				
				/* Restriction.onProperty throws an exception if the property
				 * isn't defined in the ontology; this is technically correct,
				 * but it's often better for us to just add the property...
				 */
				@SuppressWarnings("unused")
				OntProperty onProperty = null;
				try {
					onProperty = restriction.getOnProperty();
				} catch (ConversionException e) {
					RDFNode p = restriction.getPropertyValue(OWL.onProperty);
					log.debug(String.format("unknown property %s in class %s", p, clazz));
					if (p.isURIResource()) {
						String uri = ((Resource)p.as(Resource.class)).getURI();
						onProperty = resolveUndefinedPropery(clazz.getOntModel(), uri, undefinedPropertiesPolicy);
					} else {
						// TODO call a new method on PropertyRestrictionVisitor?
						log.warn(String.format("found non-URI property %s in class %s", p, clazz));
					}
				}
				
				visitor.hasRestriction(restriction);
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
			for (Object equivalentClass: clazz.listEquivalentClasses().toSet())
				decompose((OntClass)equivalentClass);
			for (Object superClass: clazz.listSuperClasses().toSet())
				decompose((OntClass)superClass);
		}

		private static OntProperty resolveUndefinedPropery(OntModel model, String uri, int undefinedPropertiesPolicy)
		{
			// first, try to resolve the property (if we're allowed to...)
			if ((undefinedPropertiesPolicy & RESOLVE) != 0) {
				log.debug(String.format("resolving property %s during decomposition", uri));
				try {
					OntProperty p = OwlUtils.getOntPropertyWithLoad(model, uri);
					if (p != null)
						return p;
					// fall-through to create...
				} catch (SADIException e) {
					log.error(String.format("error loading property %s: %s", uri, e.getMessage()));
					// fall-through to create...
				}
			}
			
			// if we're here, we failed to resolve or weren't allowed to...
			if ((undefinedPropertiesPolicy & CREATE) != 0) {
				log.debug(String.format("creating property %s during decomposition", uri));
				return model.createOntProperty(uri);
			}
			
			// if we're here, we can't do anything...
			return null;
		}
	}
	
	private static class RestrictionEnumerationVisitor implements PropertyRestrictionVisitor
	{
		Set<Restriction> restrictions;
		
		/* if an OntClass comes from a model with reasoning, we may find
		 * several copies of the same restriction from artifact equivalent
		 * classes; we don't want to store these, so maintain our own table
		 * of restrictions we've seen...
		 */
		Set<String> seen;
		
		public RestrictionEnumerationVisitor()
		{
			restrictions = new HashSet<Restriction>();
			seen = new HashSet<String>();
		}
		
		public void hasRestriction(Restriction restriction)
		{

			log.trace(String.format("found restriction %s", OwlUtils.toString(restriction)));
			String key = getHashKey(restriction);
			if (!seen.contains(key)) {
				restrictions.add(restriction);
				seen.add(key);
			}
		}
		
		String getHashKey(Restriction restriction)
		{
			return OwlUtils.toString(restriction);
		}
	}
	
	private static class PropertyEnumerationVisitor implements PropertyRestrictionVisitor
	{
		Set<OntProperty> properties;
		
		public PropertyEnumerationVisitor()
		{
			properties = new HashSet<OntProperty>();
		}
		
		public void hasRestriction(Restriction restriction)
		{
			try {
				OntProperty p = restriction.getOnProperty();
				if (p != null)
					properties.add(p);
			} catch (ConversionException e) {
				// we should already have warned about this above, but just in case...
				log.warn(String.format("undefined restricted property %s"), e);
			}
		}
	}
	
	private static class MinimalModelVisitor extends PropertyRestrictionAdapter
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
	
	protected static class MinimalOntologySearchNode extends SearchNode<Resource>
	{
		Model sourceModel;
		Model targetModel;
		
		public MinimalOntologySearchNode(Model sourceModel, Model targetModel, Resource node)
		{
			super(node);
			this.sourceModel = sourceModel;
			this.targetModel = targetModel;
		}
		
		@Override
		public Set<SearchNode<Resource>> getSuccessors() 
		{
			Set<SearchNode<Resource>> successors = new HashSet<SearchNode<Resource>>();
			Resource r = getNode();
			
			for(Statement stmt : r.listProperties().toList()) { 
				targetModel.add(stmt);
				RDFNode o = stmt.getObject();
				if(o.isResource()) {
					successors.add(new MinimalOntologySearchNode(sourceModel, targetModel, o.as(Resource.class)));
				}
			}
			
			// If we just compute a normal directed closure starting from the root URI, we may miss  
			// equivalent/inverse/disjoint properties, and equivalent/complementary classes.  
			// (Since these relationships might be asserted in only one direction.)  
			// 
			// We also need to know all subproperties of an included property, or service discovery 
			// will not work correctly.
			
			Property reverseProperties[] = {
					OWL.equivalentProperty,
					OWL.inverseOf,
					OWL.disjointWith,
					OWL.complementOf,
					OWL.equivalentClass,
					OWL.sameAs,
					OWL.differentFrom,
					RDFS.subPropertyOf, 
				};
			
			for(Property reverseProperty : reverseProperties) {
				for(Statement stmt : sourceModel.listStatements(null, reverseProperty, r).toList()) { 
					targetModel.add(stmt);
					successors.add(new MinimalOntologySearchNode(sourceModel, targetModel, stmt.getSubject()));
				}
			}

			return successors;
		}
		
	}
	
	protected static class OntologyUriPair 
	{
		public String ontologyUri;
		public String uri;
		
		public OntologyUriPair(String ontologyUri, String uri)
		{
			this.ontologyUri = ontologyUri;
			this.uri = uri;
		}
		
		public boolean equals(Object o) 
		{
			if(o instanceof OntologyUriPair) {
				OntologyUriPair other = (OntologyUriPair)o;
				return (ontologyUri.equals(other.ontologyUri) && uri.equals(other.uri));
			}
			return false;
		}
		
		public int hashCode() 
		{
			return (ontologyUri + uri).hashCode();
		}
	}	
}
