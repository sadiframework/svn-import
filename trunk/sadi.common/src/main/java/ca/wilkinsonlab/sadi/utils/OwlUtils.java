package ca.wilkinsonlab.sadi.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.decompose.ClassTracker;
import ca.wilkinsonlab.sadi.decompose.ClassVisitor;
import ca.wilkinsonlab.sadi.decompose.RestrictionAdapter;
import ca.wilkinsonlab.sadi.decompose.RestrictionVisitor;
import ca.wilkinsonlab.sadi.decompose.VisitingDecomposer;
import ca.wilkinsonlab.sadi.rdfpath.RDFPath;
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
import com.hp.hpl.jena.shared.DoesNotExistException;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Map1;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OwlUtils 
{
	private static final Logger log = Logger.getLogger( OwlUtils.class );
	private static final boolean loadMinimalOntologyByDefault = true;
	private static final OntModel owlModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
	public static final OntClass OWL_Nothing = owlModel.getOntClass(OWL.Nothing.getURI());

//	private static List<Pattern> deadOntologyPatterns;
//	static {
//		deadOntologyPatterns = new ArrayList<Pattern>();
//		String patterns[] = Config.getConfiguration().getStringArray("sadi.deadOntologyPattern");
//		for(String pattern : patterns) {
//			deadOntologyPatterns.add(Pattern.compile(pattern));
//		}
//	}
	
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
	 *  a human-readable description of an anonymous restriction
	 * 	toString()
	 * @param resource
	 * @return a human-readable label for the specified resource
	 * @deprecated use {@link LabelUtils.getLabel(Resource)} instead
	 */
	public static String getLabel(Resource resource)
	{
		if (resource == null)
			return "null";
		
		if (resource instanceof OntResource) {
			String label = ((OntResource)resource).getLabel(null);
			if (label != null)
				return label;
		} else {
			if (resource.hasProperty(RDFS.label))
				return resource.getProperty(RDFS.label).getString();
		}
		
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
		
		if (resource instanceof OntResource) {
			OntResource ontResource = (OntResource)resource;
			if (ontResource.isClass()) {
				OntClass clazz = ontResource.asClass();
				if (clazz.isRestriction()) {
					Restriction restriction = clazz.asRestriction();
					try {
						return LabelUtils.getRestrictionString(restriction);
					} catch (Exception e) {
						log.warn(e.getMessage());
					}
				} else if (clazz.isIntersectionClass()) {
					StringBuffer buf = new StringBuffer("intersection{ ");
					for (Iterator<? extends OntClass> i = clazz.asIntersectionClass().listOperands(); i.hasNext(); ) {
						buf.append(getLabel(i.next()));
						if (i.hasNext())
							buf.append(", ");
					}
					buf.append(" }");
					return buf.toString();
				} else if (clazz.isIntersectionClass()) {
					StringBuffer buf = new StringBuffer("union{ ");
					for (Iterator<? extends OntClass> i = clazz.asIntersectionClass().listOperands(); i.hasNext(); ) {
						buf.append(getLabel(i.next()));
						if (i.hasNext())
							buf.append(", ");
					}
					buf.append(" }");
					return buf.toString();
				}
			}
		}
		
		return resource.toString();
	}
	
	/**
	 * Returns the concatentation of the human-readable labels for the 
	 * specified resources. 
	 * The component labels will be those returned by
	 * <code>OwlUtils.getLabel(com.hp.hpl.jena.rdf.model.Resource)</code>.
	 * @param resources
	 * @return the concatentation of the human-readable labels for the specified resources
	 * @deprecated use {@link LabelUtils.getLabel(Resource)} and a mapping function
	 */
	public static String getLabels(Iterator<? extends Resource> resources)
	{
		StringBuilder buf = new StringBuilder("[");
		while (resources.hasNext()) {
			buf.append(getLabel(resources.next()));
			if (resources.hasNext())
				buf.append(", ");
		}
		buf.append("]");
		return buf.toString();
	}
	
	/**
	 * Returns a description of the specified OWL restriction
	 * @param r the OWL restriction
	 * @return a description of the OWL restriction
	 * @deprecated use {@link LabelUtils.getRestrictionString(Restriction)} instead
	 */
	public static String getRestrictionString(Restriction r)
	{
		return LabelUtils.getRestrictionString(r);
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

	/**
	 * Returns the default range of the specified property as if it had
	 * no explicit range.
	 * @param p
	 * @return the default range of the specified property
	 */
	public static OntClass getDefaultRange(OntProperty p)
	{
		if (p.isDatatypeProperty())
			return p.getOntModel().createClass(RDFS.Literal.getURI());
		else if (p.isObjectProperty())
			return p.getOntModel().createClass(OWL.Thing.getURI());
		else
			return p.getOntModel().createClass(RDFS.Resource.getURI());
	}
	
//	/**
//	 * Return true if the SADI configuration (sadi.common.properties / sadi.properties) 
//	 * indicates that we should bypass the loading of this ontology.
//	 * 
//	 * @param uri The URI of the ontology
//	 * @return true if the configuration indicates the ontology is dead,
//	 * false otherwise
//	 * @deprecated use the Jena document/location manager instead...
//	 */
//	@Deprecated
//	private static boolean deadOntology(String uri) 
//	{
//		for(Pattern pattern : deadOntologyPatterns) {
//			if(pattern.matcher(uri).find()) {
//				return true;
//			}
//		}
//		return false;
//	}
	
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
		
//		if (deadOntology(ontologyUri)) {
//			log.trace(String.format("skipping dead ontology %s", ontologyUri));
//			return;
//		}
		if (model.hasLoadedImport(ontologyUri)) {
			log.trace(String.format("skipping previously loaded ontology %s", ontologyUri));
			return;
		}
		
		log.trace(String.format("reading ontology from %s", ontologyUri));
		try {
			model.read(ontologyUri);
			// model.hasLoadedImport(ontologyUri) is now true...
			// model.hasLoadedImport(all ontologies imported by ontologyUri) is now true...
		} catch (JenaException e) {
			if (e instanceof DoesNotExistException) {
				throw new SADIException(String.format("no such ontology %s", uri));
			} else if (e.getMessage().endsWith("Connection refused")) {
				throw new SADIException(String.format("connection refused to %s", uri));
			}
		} catch (Exception e) {
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

	private static void loadMinimalOntologyForUri(Model model, String ontologyUri, String uri, Set<OntologyUriPair> visitedUris) throws SADIException
	{
		OntologyUriPair ontologyUriPair = new OntologyUriPair(ontologyUri, uri);

//		if (deadOntology(ontologyUri)) {
//			log.debug(String.format("skipping dead ontology %s", ontologyUri));
//			return;
//		}
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
	private static class OntologyUriPair 
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
			if (o instanceof OntologyUriPair) {
				OntologyUriPair that = (OntologyUriPair)o;
				return (this.ontologyUri.equals(that.ontologyUri) && this.uri.equals(that.uri));
			}
			return false;
		}
		
		public int hashCode() 
		{
			return (ontologyUri + uri).hashCode();
		}
	}	
	
	private static Model getMinimalOntologyFromModel(Model sourceModel, String uriInSourceModel) 
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
	private static class MinimalOntologySearchNode extends SearchNode<Resource>
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
		return getOntClassWithLoad(model, uri, loadMinimalOntologyByDefault);
	}
	
	/**
	 * Return the OntClass with the specified URI, resolving it and loading
	 * the resolved ontology into the model if it is not already there.
	 * @param model the OntModel
	 * @param uri the URI
	 * @param loadMinimalOntology if true, load only the minimal ontology
	 * @return the OntClass
	 */
	public static OntClass getOntClassWithLoad(OntModel model, String uri, boolean loadMinimalOntology) throws SADIException
	{
		OntClass c = model.getOntClass(uri);
		if (c != null)
			return c;
		
		if (loadMinimalOntology)
			loadMinimalOntologyForUri(model, uri);
		else
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
	public static OntResource getOntResourceWithLoad(OntModel model, String uri) throws SADIException
	{
		OntResource r = model.getOntResource(uri);
		if (r != null)
			return r;
		
		loadMinimalOntologyForUri(model, uri);
		return model.getOntResource(uri);
	}
	
	public static OntClass getValuesFromAsClass(Restriction restriction)
	{
		return getValuesFromAsClass(restriction, false);
	}
	
	public static OntClass getValuesFromAsClass(Restriction restriction, boolean fallbackToRange)
	{
		OntResource valuesFrom = getValuesFrom(restriction, fallbackToRange);
		if (valuesFrom != null && valuesFrom.isClass())
			return valuesFrom.asClass();
		else
			return null;
	}
	
	public static OntResource getValuesFrom(Restriction restriction)
	{
		return getValuesFrom(restriction, false);
	}
	
	public static OntResource getValuesFrom(Restriction restriction, boolean fallbackToRange)
	{
		if (restriction.isAllValuesFromRestriction()) {
			return restriction.getOntModel().getOntResource(restriction.asAllValuesFromRestriction().getAllValuesFrom());
		} else if (restriction.isSomeValuesFromRestriction()) {
			return restriction.getOntModel().getOntResource(restriction.asSomeValuesFromRestriction().getSomeValuesFrom());
		} else if (fallbackToRange) {
			return OwlUtils.getUsefulRange(restriction.getOnProperty());
		} else {
			return null;
		}
	}
	
	/**
	 * Return all properties equivalent to the specified property, including
	 * the property itself.
	 * This method is necessary to standardize behaviour between different
	 * reasoners.
	 * @param p the OWL property
	 * @return all properties equivalent to the specified property
	 */
	public static Set<OntProperty> getEquivalentProperties(OntProperty p)
	{
		return getEquivalentProperties(p, false);
	}
	
	/**
	 * Return all properties equivalent to the specified property, including
	 * the property itself and optionally all sub-properties.
	 * @param p the OWL property
	 * @param withSubProperties include sub-properties
	 * @return
	 */
	public static Set<OntProperty> getEquivalentProperties(OntProperty p, boolean withSubProperties)
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
			if (withSubProperties) {
				for (OntProperty subproperty: q.listSubProperties().toList()) {
					log.trace(String.format("found sub-property %s", subproperty));
					equivalentProperties.add(subproperty);
				}
			}
		}
		log.trace(String.format("adding original property %s", p));
		equivalentProperties.add(p);
		if (withSubProperties) {
			for (OntProperty subproperty: p.listSubProperties().toList()) {
				log.trace(String.format("found sub-property %s", subproperty));
				equivalentProperties.add(subproperty);
			}
		}
		
		return equivalentProperties;
	}
	
	/**
	 * Returns the nearest named super-class of the specified OWL class.
	 * @param c the OWL class
	 * @return the nearest named super-class of the specified OWL class
	 */
	public static OntClass getFirstNamedSuperClass(OntClass c)
	{
		for (BreadthFirstIterator<OntClass> i = new BreadthFirstIterator<OntClass>(new SuperClassSearchNode(c)); i.hasNext(); ) {
			OntClass superClass = i.next();
			if (superClass.isURIResource() && !superClass.equals(c))
				return superClass;
		}
		return c.getOntModel().getOntClass(OWL.Thing.getURI());
	}
	private static class SuperClassSearchNode extends SearchNode<OntClass>
	{
		public SuperClassSearchNode(OntClass c)
		{
			super(c);
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.utils.graph.SearchNode#getSuccessors()
		 */
		@Override
		public Set<SearchNode<OntClass>> getSuccessors()
		{
			Set<SearchNode<OntClass>> superClasses = new HashSet<SearchNode<OntClass>>();
			for (Iterator<OntClass> i = getNode().listSuperClasses(true); i.hasNext(); ) {
				OntClass superClass = i.next();
				superClasses.add(new SuperClassSearchNode(superClass));
			}
			return superClasses;
		}
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
		RestrictedPropertyCollector collector = new RestrictedPropertyCollector();
		new VisitingDecomposer(collector).decompose(clazz);
		return collector.getProperties();
	}
	private static class RestrictedPropertyCollector implements RestrictionVisitor
	{
		private Set<OntProperty> properties;
		
		public RestrictedPropertyCollector()
		{
			properties = new HashSet<OntProperty>();
		}
		
		public Set<OntProperty> getProperties()
		{
			return properties;
		}
		
		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionVisitor#visit(com.hp.hpl.jena.ontology.Restriction)
		 */
		public void visit(Restriction restriction)
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
	
	/**
	 * Return the set of property restrictions an OWL class decomposes into.
	 * @param clazz the OWL class
	 * @return the set of property restrictions the OWL class decomposes into
	 */
	public static Set<Restriction> listRestrictions(OntClass clazz)
	{
		RestrictionCollector collector = new RestrictionCollector();
		new VisitingDecomposer(collector).decompose(clazz);
		return collector.getRestrictions();
	}
	private static class RestrictionCollector implements RestrictionVisitor
	{
		private Set<Restriction> restrictions;
		
		/* if an OntClass comes from a model with reasoning, we can find
		 * several "copies" of the same restriction from artifact equivalent
		 * classes; we don't want to store these, so maintain our own table
		 * of restrictions we've seen...
		 */
		private Set<String> seen;
		
		public RestrictionCollector()
		{
			restrictions = new HashSet<Restriction>();
			seen = new HashSet<String>();
		}
		
		public Set<Restriction> getRestrictions()
		{
			return restrictions;
		}
		
		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionVisitor#visit(com.hp.hpl.jena.ontology.Restriction)
		 */
		public void visit(Restriction restriction)
		{
			log.trace(String.format("found restriction %s", LabelUtils.getRestrictionString(restriction)));
			String key = getHashKey(restriction);
			if (!seen.contains(key)) {
				restrictions.add(restriction);
				seen.add(key);
			}
		}
		
		private String getHashKey(Restriction restriction)
		{
			/* TODO this is a pretty costly way of doing this; 
			 * could be a performance issue...
			 */
			return LabelUtils.getRestrictionString(restriction);
		}
	}

	/**
	 * Return the set of property restrictions an OWL class adds relative to
	 * another OWL class. Usually the second class will be a superclass of
	 * the first.
	 * @param clazz the OWL class to decompose
	 * @param relativeTo the OWL class to decompose relative to
	 * @return the set of property restrictions the OWL class adds
	 */
	public static Set<Restriction> listRestrictions(OntClass clazz, OntClass relativeTo)
	{
		Set<Restriction> base = listRestrictions(relativeTo);
		Set<Restriction> restrictions = listRestrictions(clazz);
		restrictions.removeAll(base);
		return restrictions;
	}
	
	public static ExtendedIterator<? extends Resource> listTypes(Resource individual)
	{
		/* TODO this is causing a problem with Maven; to wit:
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:2.3.2:compile (default-compile) on project sadi-common: Compilation failure
[ERROR] /Users/luke/Code/eclipse-cocoa-64/sadi.common/src/main/java/ca/wilkinsonlab/sadi/utils/OwlUtils.java:[812,49] incompatible types; inferred type argument(s) com.hp.hpl.jena.rdf.model.Resource do not conform to bounds of type variable(s) T
[ERROR] found   : <T>com.hp.hpl.jena.util.iterator.ExtendedIterator<T>
[ERROR] required: com.hp.hpl.jena.util.iterator.ExtendedIterator<? extends com.hp.hpl.jena.rdf.model.Resource>
[ERROR] -> [Help 1]
		 * this change shouldn't affect functionality, btu it's annoying...
		 */
//		if (individual instanceof Individual)
//			return ((Individual)individual).listOntClasses(false);
//		else 
		if (individual instanceof OntResource)
			return ((OntResource)individual).listRDFTypes(false);
		else
			return individual.listProperties(RDF.type).mapWith(new Map1<Statement, Resource>() {
				public Resource map1(Statement statement) {
					return statement.getResource();
				}
			});
	}
	
	/**
	 * Returns an individual that conforms to the restrictions of the specified
	 * class.
	 * This instance will be created in a new memory model.
	 * NB: this is dangerous; it's quite easy to create an infinitely-looping
	 * construct.
	 * @param clazz the OntClass
	 * @return an individual that conforms to the restrictions of the specified class
	 */
	public static Resource createDummyInstance(OntClass clazz)
	{
		return createDummyInstance(ModelFactory.createDefaultModel(), clazz);
	}
	
	/**
	 * Returns an individual in the specified model that conforms to the
	 * restrictions of the specified class.
	 * NB: this is dangerous; it's quite easy to create an infinitely-looping
	 * construct.
	 * @param model the model in which to create the new individual
	 * @param clazz the OntClass
	 * @return an individual that conforms to the restrictions of the specified class
	 */
	public static Resource createDummyInstance(Model model, OntClass clazz)
	{
		DummyInstanceCreator creator = new DummyInstanceCreator(model);
		new VisitingDecomposer(creator, creator).decompose(clazz);
		return creator.getInstance();
	}
	private static class DummyInstanceCreator extends RestrictionAdapter implements ClassVisitor
	{
		private Model model;
		private Resource individual;
		
		public DummyInstanceCreator(Model model)
		{
			this(model, model.createResource());
		}
		
		public DummyInstanceCreator(Model model, Resource individual)
		{
			this.model = model;
			this.individual = individual;
		}
		
		public Resource getInstance()
		{
			return individual;
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.ClassVisitor#ignore(com.hp.hpl.jena.ontology.OntClass)
		 */
		@Override
		public boolean ignore(OntClass c)
		{
			/* bottom out explicitly at owl:Thing, or we'll have problems when
			 * we enumerate equivalent classes...
			 */
			return c.equals( OWL.Thing );
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.ClassVisitor#visit(com.hp.hpl.jena.ontology.OntClass)
		 */
		@Override
		public void visit(OntClass c)
		{
			if (c.isURIResource())
				model.add(individual, RDF.type, c);
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionAdapter#onProperty(com.hp.hpl.jena.ontology.OntProperty)
		 */
		@Override
		public void onProperty(OntProperty onProperty)
		{
			if (individual.hasProperty(onProperty))
				return;
			else if (onProperty.isDatatypeProperty())
				model.add(individual, onProperty, "");
			else
				model.add(individual, onProperty, model.createResource());
		}
		
		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionAdapter#minCardinality(com.hp.hpl.jena.ontology.OntProperty, int)
		 */
		@Override
		public void minCardinality(OntProperty onProperty, int minCardinality)
		{
			cardinality(onProperty, minCardinality);
		}
		
		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionAdapter#cardinality(com.hp.hpl.jena.ontology.OntProperty, int)
		 */
		@Override
		public void cardinality(OntProperty onProperty, int cardinality)
		{
			for (int currentCardinality = individual.listProperties(onProperty).toList().size(); currentCardinality<cardinality; ++currentCardinality)
				onProperty(onProperty);
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionAdapter#hasValue(com.hp.hpl.jena.ontology.OntProperty, com.hp.hpl.jena.rdf.model.RDFNode)
		 */
		@Override
		public void hasValue(OntProperty onProperty, RDFNode hasValue)
		{
			if (individual.hasProperty(onProperty, hasValue))
				return;
			else
				model.add(individual, onProperty, hasValue);
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionAdapter#valuesFrom(com.hp.hpl.jena.ontology.OntProperty, com.hp.hpl.jena.ontology.OntResource)
		 */
		public void valuesFrom(OntProperty onProperty, OntResource valuesFrom)
		{
			if (valuesFrom.isClass()) {
				/* check if there are any values of onProperty that are already
				 * instances of valuesFrom; this prevents us from recursing
				 * infinitely in most cases...
				 */
				for (Iterator<Statement> i = individual.listProperties(onProperty); i.hasNext(); ) {
					Statement statement = i.next();
					RDFNode oNode = statement.getObject();
					if (oNode.isURIResource()) {
						Resource o = oNode.asResource();
						if (o.hasProperty(RDF.type, valuesFrom))
							return;
					}
				}
				Resource object = model.createResource();
				model.add(individual, onProperty, object);
				OntClass clazz = valuesFrom.asClass();
				DummyInstanceCreator creator = new DummyInstanceCreator(model, object);
				new VisitingDecomposer(creator).decompose(clazz);
			}
		}
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
	public static Model getMinimalModel(final Resource individual, final OntClass asClass)
	{
		final Model model = ModelFactory.createDefaultModel();
		MinimalModelVisitor visitor = new MinimalModelVisitor(model, individual);
		new VisitingDecomposer(visitor, visitor, visitor).decompose(asClass);
		return model;
	}
	private static class MinimalModelVisitor extends RestrictionAdapter implements ClassTracker, ClassVisitor
	{
		private Model model;
		private Resource subject;
		private Set<String> visited;
		
		public MinimalModelVisitor(Model model, Resource subject)
		{
			this(model, subject, new HashSet<String>());
		}
		
		private MinimalModelVisitor(Model model, Resource subject, Set<String> visited)
		{
			this.model = model;
			this.subject = subject;
			this.visited = visited;
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.ClassTracker#seen(com.hp.hpl.jena.ontology.OntClass)
		 */
		@Override
		public boolean seen(OntClass c)
		{
			/* remember that we've visited this individual as this class
			 * in order to prevent cycles where the object of one of our
			 * triples has us as the object of one of theirs...
			 */
			String hashKey = getHashKey(subject, c);
			if (visited.contains(hashKey)) {
				return true;
			} else {
				visited.add(hashKey);
				return false;
			}
		}
		private static String getHashKey(Resource individual, Resource asClass)
		{
			return String.format("%s %s", 
					individual.isURIResource() ? individual.getURI() : individual.getId(),
					asClass.isURIResource() ? asClass.getURI() : asClass.getId()
			);
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.ClassVisitor#ignore(com.hp.hpl.jena.ontology.OntClass)
		 */
		@Override
		public boolean ignore(OntClass c)
		{
			/* bottom out explicitly at owl:Thing, or we'll have problems when
			 * we enumerate equivalent classes...
			 */
			return c.equals( OWL.Thing );
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.ClassVisitor#visit(com.hp.hpl.jena.ontology.OntClass)
		 */
		@Override
		public void visit(OntClass c)
		{
			log.trace(String.format("visiting %s as %s", subject, c));
			
			/* if the individual is explicitly declared as a member of the 
			 * target class, add that type statement to the model...
			 */
			if (c.isURIResource() && subject.hasProperty(RDF.type, c))
				model.add(subject, RDF.type, c);
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionAdapter#onProperty(com.hp.hpl.jena.ontology.OntProperty)
		 */
		public void onProperty(OntProperty onProperty)
		{
			/* TODO there may be some cases where we don't have to add all
			 * values of the restricted property, but this shouldn't be too
			 * bad...
			 */
			model.add(subject.listProperties(onProperty));
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionAdapter#hasValue(com.hp.hpl.jena.ontology.OntProperty, com.hp.hpl.jena.rdf.model.RDFNode)
		 */
		public void hasValue(OntProperty onProperty, RDFNode hasValue)
		{
			if (subject.hasProperty(onProperty, hasValue)) {
				model.add(subject, onProperty, hasValue);
			}
		}

		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionAdapter#valuesFrom(com.hp.hpl.jena.ontology.OntProperty, com.hp.hpl.jena.ontology.OntResource)
		 */
		public void valuesFrom(OntProperty onProperty, OntResource valuesFrom)
		{
			/* for (all/some)ValuesFrom restrictions, we need to add enough
			 * information to determine the class membership of the objects of
			 * the statements as well...
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
					Resource object = statement.getResource();
					OntClass clazz = valuesFrom.asClass();
					if (!visited.contains(getHashKey(object, clazz))) {
						MinimalModelVisitor visitor = new MinimalModelVisitor(model, object, visited);
						new VisitingDecomposer(visitor, visitor).decompose(clazz);
					}
				}
			}
		}
	}
	
	public static Restriction createRestrictions(OntModel model, RDFPath path)
	{
		return createRestrictions(model, path, true);
	}
	
	public static Restriction createRestrictions(OntModel model, RDFPath path, boolean anonymous)
	{
		Restriction r = null;
		for (int i=path.size()-1; i>=0; i-=1) {
			Property p = path.get(i).getProperty();
			Resource type = path.get(i).getType();
			if (r != null) {
				if (type != null) {
					OntClass valuesFrom = model.createIntersectionClass(anonymous ? null : getUURI(), model.createList(new RDFNode[]{type, r}));
					r = model.createSomeValuesFromRestriction(anonymous ? null : getUURI(), p, valuesFrom);
				} else {
					r = model.createSomeValuesFromRestriction(anonymous ? null : getUURI(), p, r);
				}
			} else {
				if (type != null) {
					r = model.createSomeValuesFromRestriction(anonymous ? null : getUURI(), p, type);
				} else {
					r = model.createMinCardinalityRestriction(anonymous ? null : getUURI(), p, 1);
				}
			}
		}
		return r;
	}
	
	public static String getUURI()
	{
		return String.format("urn:uuid:%s", UUID.randomUUID());
	}
	
	/**
	 * Visit each property restriction of the specified OWL class with the
	 * specified RestrictionVisitor.
	 * @param clazz the class
	 * @param visitor the visitor
	 */

	/*
	public static void decompose(OntClass clazz, RestrictionVisitor visitor)
	{
		if ( clazz == null )
			throw new IllegalArgumentException("class to decompose cannot be null");
		
		new VisitingDecomposer(visitor).decompose(clazz);
	}
	
	public static void decompose(OntClass clazz, PropertyRestrictionVisitor visitor, Tracker tracker, ClassFilter filter)
	{
		if ( clazz == null )
			throw new IllegalArgumentException("class to decompose cannot be null");
		
		new VisitingDecomposer(visitor, tracker, filter).decompose(clazz);
	}
	
	public static interface PropertyRestrictionVisitor
	{
		void hasRestriction(Restriction restriction);
	}
	
	public static interface Tracker
	{
		public boolean beenThere(OntClass c);
	}
	
	public static class DefaultTracker implements Tracker
	{
		private Set<OntClass> visited;
		
		public DefaultTracker()
		{
			this.visited = new HashSet<OntClass>();
		}
		
		public boolean beenThere(OntClass c)
		{
			if ( visited.contains(c) ) {
				return true;
			} else {
				visited.add(c);
				return false;
			}
		}
	}
	
	public static interface ClassFilter
	{
		public boolean ignoreClass(OntClass c);
	}
	
	public static class DefaultClassFilter implements ClassFilter
	{
		public boolean ignoreClass(OntClass c)
		{
			return c.equals( OWL.Thing );
		}
	}
	*/
}
