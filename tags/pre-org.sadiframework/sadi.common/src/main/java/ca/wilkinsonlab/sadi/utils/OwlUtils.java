package ca.wilkinsonlab.sadi.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.Config;
import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.decompose.VisitingDecomposer;
import ca.wilkinsonlab.sadi.owl2sparql.QueryGeneratingDecomposer;
import ca.wilkinsonlab.sadi.rdfpath.RDFPath;
import ca.wilkinsonlab.sadi.utils.graph.BreadthFirstIterator;
import ca.wilkinsonlab.sadi.utils.graph.OpenGraphIterator;

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
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OwlUtils 
{
	private static final Logger log = Logger.getLogger( OwlUtils.class );
	
	private static final OntModel owlModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
	
	/**
	 * An OntClass view of owl:nothing.
	 */
	public static final OntClass OWL_Nothing = owlModel.getOntClass(OWL.Nothing.getURI());
	
	/**
	 * Returns an OntModel view of the base OWL ontology.  This is useful
	 * for things like getting an OntClass view of OWL.Thing, etc.
	 * @return
	 */
	public static OntModel getOWLModel()
	{
		return owlModel;
	}
	
	/**
	 * Returns the configured default OntModelSpec.
	 * @return the configured default OntModelSpec
	 * @throws SADIException if there is a problem with the configuration
	 */
	public static OntModelSpec getDefaultReasonerSpec() throws SADIException
	{
		return getReasonerSpec(Config.getConfiguration().getString("sadi.defaultReasoner"));
	}
	
	/**
	 * Returns an in-memory OntModel with the configured default OntModelSpec
	 * @return an in-memory OntModel with the configured default OntModelSpec
	 * @throws SADIException if there is a problem with the configuration
	 */
	public static OntModel createDefaultReasoningModel() throws SADIException
	{
		return ModelFactory.createOntologyModel(getDefaultReasonerSpec());
	}
	
	/**
	 * Returns the OntModelSpec corresponding to the specified string.
	 * This is a convenience method to easily allow reasoners to be 
	 * instantiated based on strings in configuration files.
	 * @param specString
	 * @return the OntModelSpec
	 * @throws SADIException if there is an error loading the OntModelSpec
	 */
	public static OntModelSpec getReasonerSpec(String specString) throws SADIException
	{
		String specClassName = "";
		String specFieldName = "";
		try {
			int lastDot = specString.lastIndexOf('.');
			specClassName = specString.substring(0, lastDot);
			specFieldName = specString.substring(lastDot+1);
			Class<?> specClass = Class.forName(specClassName);
			Field specField = specClass.getField(specFieldName);
			return (OntModelSpec)specField.get(null);
		} catch (IndexOutOfBoundsException e) {
			throw new SADIException(String.format("reasoner spec must be a fully qualified class.field", specString), e);
		} catch (ClassNotFoundException e) {
			throw new SADIException(String.format("no such class '%s'", specClassName));
		} catch (NoSuchFieldException e) {
			throw new SADIException(String.format("no such field '%s'", specFieldName));
		} catch (SecurityException e) {
			throw new SADIException(String.format("%s.%s is not visible", specClassName, specFieldName));
		} catch (IllegalAccessException e) {
			throw new SADIException(String.format("%s.%s is not visible", specClassName, specFieldName));
		} catch (IllegalArgumentException e) {
			// this shouldn't happen...
			throw new SADIException("", e);
		} catch (ClassCastException e) {
			throw new SADIException(String.format("%s.%s is not an OntModelSpec", specClassName, specFieldName));
		}
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
	
	/**
	 * Returns the valuesFrom of the specified restriction, 
	 * or null if there is none.
	 * @param restriction the restriction
	 * @return the valuesFrom of the specified restriction
	 */
	public static OntResource getValuesFrom(Restriction restriction)
	{
		return getValuesFrom(restriction, false);
	}
	
	/**
	 * Returns the valuesFrom of the specified restriction. If the restriction
	 * is not an all/someValuesFrom restriction, optionally return the range
	 * of the restricted property.
	 * @param restriction the restriction
	 * @param fallbackToRange if true and the restriction has no valuesFrom,
	 *                        return the range of the restricted property instead
	 * @return the valuesFrom of the specified restriction
	 */
	public static OntResource getValuesFrom(Restriction restriction, boolean fallbackToRange)
	{
		OntResource valuesFrom = null;
		if (restriction.isAllValuesFromRestriction()) {
			valuesFrom = restriction.getOntModel().getOntResource(restriction.asAllValuesFromRestriction().getAllValuesFrom());
		} else if (restriction.isSomeValuesFromRestriction()) {
			valuesFrom = restriction.getOntModel().getOntResource(restriction.asSomeValuesFromRestriction().getSomeValuesFrom());
		}
		if (valuesFrom == null && fallbackToRange) {
			valuesFrom = OwlUtils.getUsefulRange(restriction.getOnProperty());
		}
		return valuesFrom;
	}
	
	/**
	 * Returns the valuesFrom of the specified restriction as an OntClass,
	 * or null if this isn't possible.
	 * @param restriction the restriction
	 * @return the valuesFrom of the specified restriction as an OntClass
	 */
	public static OntClass getValuesFromAsClass(Restriction restriction)
	{
		return getValuesFromAsClass(restriction, false);
	}
	
	/**
	 * Returns the valuesFrom of the specified restriction as an OntClass. 
	 * If the restriction is not an all/someValuesFrom restriction, optionally 
	 * return the range of the restricted property as an OntClass.
	 * @param restriction the restriction
	 * @param fallbackToRange if true and the restriction has no valuesFrom,
	 *                        return the range of the restricted property instead
	 * @return the valuesFrom of the specified restriction
	 * @return
	 */
	public static OntClass getValuesFromAsClass(Restriction restriction, boolean fallbackToRange)
	{
		OntResource valuesFrom = getValuesFrom(restriction, fallbackToRange);
		if (valuesFrom != null && valuesFrom.isClass())
			return valuesFrom.asClass();
		else
			return null;
	}

	/**
	 * Resolve the specified URI and load the resulting statements into the
	 * specified OntModel. If so configured, import isDefinedBy URIs and/or 
	 * load only the minimal ontology that defines the URI.
	 * @param model the OntModel
	 * @param uri the URI
	 * @throws SADIException if there is a problem reading the model or any of its imports
	 */
	public static void loadOntologyForUri(OntModel model, String uri) throws SADIException
	{
		loadOntologyForUri(model, uri, 
				Config.getConfiguration().getBoolean("sadi.loadOntologyForURI.importIsDefinedBy"),
				Config.getConfiguration().getBoolean("sadi.loadOntologyForURI.loadMinimalByDefault"));
	}
	
	/**
	 * Resolve the specified URI and load the resulting statements into the
	 * specified OntModel. Optionally import isDefinedBy URIs and/or load only
	 * the minimal ontology that defines the URI. 
	 * @param model the OntModel
	 * @param uri the URI
	 * @param importIsDefinedBy if true, import isDefinedBy URIs
	 * @param loadMinimalOntology if true, load only the minimal ontology that defines the URI
	 * @throws SADIException if there is a problem reading the model or any of its imports
	 */
	public static void loadOntologyForUri(OntModel model, String uri, 
			boolean importIsDefinedBy, boolean loadMinimalOntology) throws SADIException
	{
		String ontologyURI = StringUtils.substringBefore(uri, "#");
		if (log.isDebugEnabled())
			log.debug(String.format("loading ontology for %s from %s", uri, ontologyURI));
		
		if (model.hasLoadedImport(ontologyURI)) {
			log.trace(String.format("skipping previously loaded ontology %s", ontologyURI));
			return;
		}
		
		/* if we're loading a minimal ontology, what we're actually doing is
		 * loading everything and pruning it before adding to the actual model;
		 * this is the model that will hold the whole before it's pruned (note
		 * that it shouldn't need reasoning...)
		 */
		OntModel localModel = loadMinimalOntology ?
				ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM) : model;
		
		try {
			localModel.read(ontologyURI);
		} catch (JenaException e) {
			if (e instanceof DoesNotExistException) {
				throw new SADIException(String.format("no such ontology %s", uri));
			} else if (e.getMessage().endsWith("Connection refused")) {
				throw new SADIException(String.format("connection refused to %s", uri));
			}
		} catch (Exception e) {
			throw new SADIException(e.toString(), e);
		}

		if (importIsDefinedBy) {
			/* extract to a list here to prevent concurrent modification exceptions...
			 */
			for (Statement statement: localModel.getResource(uri).listProperties(RDFS.isDefinedBy).toList()) {
				if (statement.getObject().isURIResource()) {
					loadOntologyForUri(localModel, statement.getResource().getURI(), false, importIsDefinedBy);
				}
			}
		}
		
		if (loadMinimalOntology) {
			extractMinimalOntology(model, localModel, uri);
		}
	}
	
	/**
	 * Resolve the specified URI and load only the statements about that URI
	 * into the specified OntModel.
	 * @param model the OntModel
	 * @param uri the URI
	 * @throws SADIException if there is a problem reading the model or any of its imports
	 */
	public static void loadMinimalOntologyForUri(OntModel model, String uri) throws SADIException 
	{
		loadOntologyForUri(model, uri, Config.getConfiguration().getBoolean("sadi.loadOntologyForURI.importIsDefinedBy"), true);
	}
	
	/**
	 * Extract only those stations that describe a specified URI from a source
	 * ontology and add them to a target ontology. 
	 * @param target the target model
	 * @param source the source model
	 * @param uri the URI
	 */
	public static void extractMinimalOntology(Model target, Model source, String uri) 
	{
		Resource root = source.getResource(uri);
		OpenGraphIterator<Resource> i = new BreadthFirstIterator<Resource>(new MinimalOntologySearchNode(source, target, root));
		// we are only interested in the side effect of the iteration, 
		// which is to load statements about each of the visited resources
		// into the target model
		i.iterate();
	}
	
//	public static void loadMinimalOntologyForUri(Model model, String ontologyUri, String uri) throws SADIException
//	{
//		loadMinimalOntologyForUri(model, ontologyUri, uri, new HashSet<OntologyUriPair>());
//	}
//
//	private static void loadMinimalOntologyForUri(Model model, String ontologyUri, String uri, Set<OntologyUriPair> visitedUris) throws SADIException
//	{
//		OntologyUriPair ontologyUriPair = new OntologyUriPair(ontologyUri, uri);
//
////		if (deadOntology(ontologyUri)) {
////			log.debug(String.format("skipping dead ontology %s", ontologyUri));
////			return;
////		}
//		if(visitedUris.contains(ontologyUriPair)) {
//			log.debug(String.format("skipping previously loaded uri %s from %s", uri, ontologyUri));
//			return;
//		}
//
//		log.debug(String.format("loading minimal ontology for %s from %s", uri, ontologyUri));
//		visitedUris.add(ontologyUriPair);
//		
//		try {
//			OntModel wholeOntology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
//			wholeOntology.read(ontologyUri);
//
//			Model minimalOntology = getMinimalOntologyFromModel(wholeOntology, uri);
//			
//			/* Michel Dumontier's predicates resolve to a minimal definition that
//			 * doesn't include the inverse relationship, so we need to resolve
//			 * the ontology that contains the complete definition...
//			 * We extract to a list here to prevent concurrent modification exceptions...
//			 */
//			for (Statement statement: minimalOntology.getResource(uri).listProperties(RDFS.isDefinedBy).toList()) {
//				Resource s = statement.getSubject();
//				RDFNode o = statement.getObject();
//				if (s.isURIResource() && o.isURIResource()) {
//					log.debug(String.format("following (%s, rdfs:isDefinedBy, %s)", s, o));
//					loadMinimalOntologyForUri(minimalOntology, ((Resource)o).getURI(), s.getURI(), visitedUris);
//				}
//			}			
//			model.add(minimalOntology);
//		} catch(Exception e) {
//			if (e instanceof SADIException)
//				throw (SADIException)e;
//			else
//				throw new SADIException(String.format("error loading ontology for %s from %s", uri, ontologyUri), e);			
//		}
//	}
//	private static class OntologyUriPair 
//	{
//		public String ontologyUri;
//		public String uri;
//		
//		public OntologyUriPair(String ontologyUri, String uri)
//		{
//			this.ontologyUri = ontologyUri;
//			this.uri = uri;
//		}
//		
//		public boolean equals(Object o) 
//		{
//			if (o instanceof OntologyUriPair) {
//				OntologyUriPair that = (OntologyUriPair)o;
//				return (this.ontologyUri.equals(that.ontologyUri) && this.uri.equals(that.uri));
//			}
//			return false;
//		}
//		
//		public int hashCode() 
//		{
//			return (ontologyUri + uri).hashCode();
//		}
//	}
	
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
		
		loadOntologyForUri(model, uri);
		return model.getOntProperty(uri);
	}
	
	/**
	 * Return the OntClass with the specified URI, resolving it and loading
	 * the resolved ontology into the model if necessary.
	 * @param model the OntModel
	 * @param uri the URI
	 * @return the OntClass
	 */
	public static OntClass getOntClassWithLoad(OntModel model, String uri) throws SADIException
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
	public static OntResource getOntResourceWithLoad(OntModel model, String uri) throws SADIException
	{
		OntResource r = model.getOntResource(uri);
		if (r != null)
			return r;
		
		loadOntologyForUri(model, uri);
		return model.getOntResource(uri);
	}
	
	/**
	 * Return all properties equivalent to the specified property, including
	 * the property itself.
	 * This method is necessary to standardize behaviour between different
	 * reasoners (some reasoners don't include the property itself, some
	 * reasoners return an immutable list, etc.)
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
	 * This method is necessary to standardize behaviour between different
	 * reasoners (some reasoners don't include the property itself, some
	 * reasoners return an immutable list, etc.)
	 * @param p the OWL property
	 * @param withSubProperties include sub-properties
	 * @return
	 */
	public static Set<OntProperty> getEquivalentProperties(OntProperty p, boolean withSubProperties)
	{
		if (log.isDebugEnabled()) {
			log.debug(String.format("finding all properties equivalent to %s%s", p, 
					withSubProperties ? " (and their sub-properties)" : ""));
		}
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
		if (!equivalentProperties.contains(p)) {
			log.trace(String.format("adding original property %s", p));
			equivalentProperties.add(p);
			if (withSubProperties) {
				for (OntProperty subproperty: p.listSubProperties().toList()) {
					log.trace(String.format("found sub-property %s", subproperty));
					equivalentProperties.add(subproperty);
				}
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
	
	/**
	 * Return the set of properties the OWL class identified by a URI has restrictions on.
	 * The ontology containing the OWL class (and any referenced imports) will be fetched
	 * and processed.
	 * @param classUri the URI of the OWL class
	 * @return the set of properties the OWL class has restrictions on
	 * @deprecated use {@link OwlUtils.listRestrictedPropertes(OntClass)} instead
	 */
	public static Set<OntProperty> listRestrictedProperties(String classUri) throws SADIException
	{
		OntModel model = createDefaultReasoningModel();
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
	
	/**
	 * Returns an iterator over the rdf:types of an individual.
	 * @param individual the individual
	 * @return an iterator over the rdf:types of an individual
	 */
	public static ExtendedIterator<? extends Resource> listTypes(Resource individual)
	{
		/* TODO this is causing a problem with Maven; to wit:
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:2.3.2:compile (default-compile) on project sadi-common: Compilation failure
[ERROR] /Users/luke/Code/eclipse-cocoa-64/sadi.common/src/main/java/ca/wilkinsonlab/sadi/utils/OwlUtils.java:[812,49] incompatible types; inferred type argument(s) com.hp.hpl.jena.rdf.model.Resource do not conform to bounds of type variable(s) T
[ERROR] found   : <T>com.hp.hpl.jena.util.iterator.ExtendedIterator<T>
[ERROR] required: com.hp.hpl.jena.util.iterator.ExtendedIterator<? extends com.hp.hpl.jena.rdf.model.Resource>
		 * this change shouldn't affect functionality, but it's annoying...
		 */
//		if (individual instanceof Individual)
//			return ((Individual)individual).listOntClasses(false);
//		else 
		if (individual instanceof OntResource)
			return ((OntResource)individual).listRDFTypes(false);
		else
			return RdfUtils.getPropertyValues(individual, RDF.type, null);
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
		MinimalModelDecomposer decomposer = new MinimalModelDecomposer(individual, asClass);
		decomposer.decompose();
		return decomposer.getModel();
//		final Model model = ModelFactory.createDefaultModel();
//		MinimalModelVisitor visitor = new MinimalModelVisitor(model, individual);
//		new VisitingDecomposer(visitor, visitor, visitor).decompose(asClass);
//		return model;
	}
	
//	private static class MinimalModelVisitor extends RestrictionAdapter implements ClassTracker, ClassVisitor
//	{
//		private Model model;
//		private Resource subject;
//		private Set<String> visited;
//		
//		public MinimalModelVisitor(Model model, Resource subject)
//		{
//			this(model, subject, new HashSet<String>());
//		}
//		
//		private MinimalModelVisitor(Model model, Resource subject, Set<String> visited)
//		{
//			this.model = model;
//			this.subject = subject;
//			this.visited = visited;
//		}
//
//		/* (non-Javadoc)
//		 * @see ca.wilkinsonlab.sadi.decompose.ClassTracker#seen(com.hp.hpl.jena.ontology.OntClass)
//		 */
//		@Override
//		public boolean seen(OntClass c)
//		{
//			/* remember that we've visited this individual as this class
//			 * in order to prevent cycles where the object of one of our
//			 * triples has us as the object of one of theirs...
//			 */
//			String hashKey = getHashKey(subject, c);
//			if (visited.contains(hashKey)) {
//				return true;
//			} else {
//				visited.add(hashKey);
//				return false;
//			}
//		}
//		private static String getHashKey(Resource individual, Resource asClass)
//		{
//			return String.format("%s %s", 
//					individual.isURIResource() ? individual.getURI() : individual.getId(),
//					asClass.isURIResource() ? asClass.getURI() : asClass.getId()
//			);
//		}
//
//		/* (non-Javadoc)
//		 * @see ca.wilkinsonlab.sadi.decompose.ClassVisitor#ignore(com.hp.hpl.jena.ontology.OntClass)
//		 */
//		@Override
//		public boolean ignore(OntClass c)
//		{
//			/* bottom out explicitly at owl:Thing, or we'll have problems when
//			 * we enumerate equivalent classes...
//			 */
//			return c.equals( OWL.Thing );
//		}
//
//		/* (non-Javadoc)
//		 * @see ca.wilkinsonlab.sadi.decompose.ClassVisitor#visitPreDecompose(com.hp.hpl.jena.ontology.OntClass)
//		 */
//		@Override
//		public void visitPreDecompose(OntClass c)
//		{
//			log.trace(String.format("visiting %s as %s", subject, c));
//			
//			/* if the individual is explicitly declared as a member of the 
//			 * target class, add that type statement to the model...
//			 */
//			if (c.isURIResource() && subject.hasProperty(RDF.type, c))
//				model.add(subject, RDF.type, c);
//		}
//		
//		/* (non-Javadoc)
//		 * @see ca.wilkinsonlab.sadi.decompose.ClassVisitor#visitPostDecompose(com.hp.hpl.jena.ontology.OntClass)
//		 */
//		public void visitPostDecompose(OntClass c)
//		{
//		}
//
//		/* (non-Javadoc)
//		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionAdapter#onProperty(com.hp.hpl.jena.ontology.OntProperty)
//		 */
//		public void onProperty(OntProperty onProperty)
//		{
//			/* TODO there may be some cases where we don't have to add all
//			 * values of the restricted property, but this shouldn't be too
//			 * bad...
//			 */
//			model.add(subject.listProperties(onProperty));
//		}
//
//		/* (non-Javadoc)
//		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionAdapter#hasValue(com.hp.hpl.jena.ontology.OntProperty, com.hp.hpl.jena.rdf.model.RDFNode)
//		 */
//		public void hasValue(OntProperty onProperty, RDFNode hasValue)
//		{
//			if (subject.hasProperty(onProperty, hasValue)) {
//				model.add(subject, onProperty, hasValue);
//			}
//		}
//
//		/* (non-Javadoc)
//		 * @see ca.wilkinsonlab.sadi.decompose.RestrictionAdapter#valuesFrom(com.hp.hpl.jena.ontology.OntProperty, com.hp.hpl.jena.ontology.OntResource)
//		 */
//		public void valuesFrom(OntProperty onProperty, OntResource valuesFrom)
//		{
//			/* for (all/some)ValuesFrom restrictions, we need to add enough
//			 * information to determine the class membership of the objects of
//			 * the statements as well...
//			 * (extract to list to avoid ConcurrentModificationException)
//			 */
//			for (Statement statement: subject.listProperties(onProperty).toList()) {
//				/* always add the statement itself; this covers the case where
//				 * valuesFrom is a datatype or data range...
//				 */
//				model.add(statement);
//				
//				/* if valuesFrom is a class and the object of the statement
//				 * isn't a literal, recurse...
//				 */
//				if (valuesFrom.isClass() && statement.getObject().isResource()) {
//					Resource object = statement.getResource();
//					OntClass clazz = valuesFrom.asClass();
//					if (!visited.contains(getHashKey(object, clazz))) {
//						MinimalModelVisitor visitor = new MinimalModelVisitor(model, object, visited);
//						new VisitingDecomposer(visitor, visitor).decompose(clazz);
//					}
//				}
//			}
//		}
//	}
	
	/**
	 * Create an anonymous chain of property restrictions in the specified 
	 * OntModel that conform to the specified RDFPath.
	 * @param model the OntModel
	 * @param path the RDFPath
	 */
	public static Restriction createRestrictions(OntModel model, RDFPath path)
	{
		return createRestrictions(model, path, true);
	}
	
	/**
	 * Create a chain of property restrictions in the specified OntModel
	 * that conform to the specified RDFPath. These restrictions will be
	 * anonymous or have RFC 4122 UUID URNs as specified.
	 * @param model the OntModel
	 * @param path the RDFPath
	 * @param anonymous if true, create anonymous restrictions;
	 *                  if false, use UUID URNs
	 */
	public static Restriction createRestrictions(OntModel model, RDFPath path, boolean anonymous)
	{
		Restriction r = null;
		for (int i=path.size()-1; i>=0; i-=1) {
			Property p = path.get(i).getProperty();
			Resource type = path.get(i).getType();
			if (r != null) {
				if (type != null) {
					OntClass valuesFrom = model.createIntersectionClass(anonymous ? null : RdfUtils.createUniqueURI(), model.createList(new RDFNode[]{type, r}));
					r = model.createSomeValuesFromRestriction(anonymous ? null : RdfUtils.createUniqueURI(), p, valuesFrom);
				} else {
					r = model.createSomeValuesFromRestriction(anonymous ? null : RdfUtils.createUniqueURI(), p, r);
				}
			} else {
				if (type != null) {
					r = model.createSomeValuesFromRestriction(anonymous ? null : RdfUtils.createUniqueURI(), p, type);
				} else {
					r = model.createMinCardinalityRestriction(anonymous ? null : RdfUtils.createUniqueURI(), p, 1);
				}
			}
		}
		return r;
	}
	
	/**
	 * Returns a SPARQL CONSTRUCT query that will fetch instances of the
	 * specified OWL class.
	 * @param clazz the OWL class
	 * @return the SPARQL CONSTRUCT query
	 */
	public static final String getConstructQuery(OntClass clazz)
	{
		QueryGeneratingDecomposer decomposer = new QueryGeneratingDecomposer();
		decomposer.decompose(clazz);
		return decomposer.getQuery();
	}
}
