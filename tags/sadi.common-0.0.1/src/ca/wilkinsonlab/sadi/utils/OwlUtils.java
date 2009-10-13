package ca.wilkinsonlab.sadi.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.common.Config;

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
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

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
	public static Set<OntProperty> listRestrictedProperties(String classUri)
	{
		// TODO do we need more reasoning here?
		OntModel inf = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		inf.read( StringUtils.substringBefore( classUri, "#" ) );
		return listRestrictedProperties( inf.getOntClass( classUri ) );
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
	
	public static Model getMinimalModel(Resource individual, OntClass asClass)
	{
		Model model = ModelFactory.createDefaultModel();
		MinimalModelVisitor visitor = new MinimalModelVisitor(model, individual, asClass);
		decompose(asClass, visitor);
		return model;
	}
	
	public static void decompose(OntClass clazz, PropertyRestrictionVisitor visitor)
	{
		if ( clazz == null )
			throw new IllegalArgumentException("class to decompose cannot be null");
		
		new VisitingDecomposer(visitor).decompose(clazz);
	}
	
	private static class VisitingDecomposer
	{
		private PropertyRestrictionVisitor visitor;
		private Set<OntClass> visited;
		
		boolean addUnknownProperties;
		
		public VisitingDecomposer(PropertyRestrictionVisitor visitor)
		{
			this.visitor = visitor;
			this.visited = new HashSet<OntClass>();
			
			addUnknownProperties = Config.getConfiguration().getBoolean("sadi.decompose.addUnknownProperties", true);
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
				} catch (Exception e) {
					/* TODO I can't remember which Exception (if any) is
					 * actually thrown; figure it out and fix this code...
					 */
					log.warn(String.format("exception getting property on %s", restriction), e);
				} finally {
					if (onProperty == null) {
						RDFNode p = restriction.getPropertyValue(OWL.onProperty);
						log.info(String.format("unknown property %s in ontology", p));
						if (p.isURIResource()) {
							String uri = ((Resource)p.as(Resource.class)).getURI();
							if (addUnknownProperties) {
								log.info(String.format("adding unknown property %s", p));
								onProperty = clazz.getOntModel().createOntProperty(uri);
							}
						} else {
							/* TODO call a new method on PropertyRestrictionVisitor?
							 */
							log.warn(String.format("found non-URI property %s in restriction", p));
						}
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
	}
	
	public static interface PropertyRestrictionVisitor
	{
		void onProperty(OntProperty onProperty);
		void valuesFrom(OntProperty onProperty, OntResource valuesFrom);
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

		public void valuesFrom(OntProperty onProperty, OntResource valuesFrom)
		{
			/* for (all/some)ValuesFrom restrictions, we need to add enough
			 * information to determine the class membership of the objects of
			 * the statments as well...
			 */
			for (StmtIterator statements = subject.listProperties(onProperty); statements.hasNext(); ) {
				Statement statement = statements.nextStatement();
				
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
					asClass.isURIResource() ? individual.getURI() : individual.getId()
			);
		}
	}
}
