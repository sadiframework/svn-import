package org.sadiframework.decompose;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sadiframework.Config;
import org.sadiframework.SADIException;
import org.sadiframework.utils.LabelUtils;
import org.sadiframework.utils.OwlUtils;


import com.hp.hpl.jena.ontology.ComplementClass;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.IntersectionClass;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

public class VisitingDecomposer
{
	private static final Logger log = Logger.getLogger(VisitingDecomposer.class);
	
	protected ClassTracker tracker;
	protected ClassVisitor classVisitor;
	protected RestrictionVisitor restrictionVisitor;
	
	private static final int IGNORE = 0x0;
	private static final int CREATE = 0x1;
	private static final int RESOLVE = 0x2;
	private int undefinedPropertiesPolicy;
	
	public VisitingDecomposer()
	{
		this(new RestrictionAdapter());
	}
	
	public VisitingDecomposer(RestrictionVisitor restrictionVisitor)
	{
		this(new DefaultClassTracker(), new DefaultClassVisitor(), restrictionVisitor);
	}
	
	public VisitingDecomposer(ClassVisitor classVisitor, RestrictionVisitor restrictionVisitor)
	{
		this(new DefaultClassTracker(), classVisitor, restrictionVisitor);
	}
	
	public VisitingDecomposer(ClassTracker tracker, ClassVisitor classVisitor, RestrictionVisitor restrictionVisitor)
	{
		this.tracker = tracker;
		this.classVisitor = classVisitor;
		this.restrictionVisitor = restrictionVisitor;
		
		String s = Config.getConfiguration().getString("sadi.decompose.undefinedPropertiesPolicy", "create");
		undefinedPropertiesPolicy = IGNORE;
		if (s.equalsIgnoreCase("create"))
			undefinedPropertiesPolicy = CREATE;
		else if (s.equalsIgnoreCase("resolve"))
			undefinedPropertiesPolicy = RESOLVE;
		else if (s.equalsIgnoreCase("resolveThenCreate"))
			undefinedPropertiesPolicy = RESOLVE | CREATE;
	}

	/**
	 * @return the tracker
	 */
	public ClassTracker getTracker()
	{
		return tracker;
	}

	/**
	 * @param tracker the tracker to set
	 */
	public void setTracker(ClassTracker tracker)
	{
		this.tracker = tracker;
	}

	/**
	 * @return the classVisitor
	 */
	public ClassVisitor getClassVisitor()
	{
		return classVisitor;
	}

	/**
	 * @param classVisitor the classVisitor to set
	 */
	public void setClassVisitor(ClassVisitor classVisitor)
	{
		this.classVisitor = classVisitor;
	}

	/**
	 * @return the restrictionVisitor
	 */
	public RestrictionVisitor getRestrictionVisitor()
	{
		return restrictionVisitor;
	}

	/**
	 * @param restrictionVisitor the restrictionVisitor to set
	 */
	public void setRestrictionVisitor(RestrictionVisitor restrictionVisitor)
	{
		this.restrictionVisitor = restrictionVisitor;
	}
	
	/**
	 * @param clazz
	 */
	public void decompose(OntClass clazz)
	{
		if ( tracker.seen(clazz) )
			return;
		
		if ( classVisitor.ignore(clazz) )
			return;
		
		classVisitor.visitPreDecompose(clazz);
		
		if (log.isTraceEnabled())
			log.trace(String.format("decomposing %s", LabelUtils.getLabel(clazz)));
		
		/* base case: is this a property restriction?
		 */
		if ( clazz.isRestriction() ) {
			Restriction restriction = clazz.asRestriction();
			
			/* Restriction.onProperty throws an exception if the property
			 * isn't defined in the ontology; this is technically correct,
			 * but it's often better for us to just add the property...
			 */
			try {
				restriction.getOnProperty();
			} catch (ConversionException e) {
				RDFNode p = restriction.getPropertyValue(OWL.onProperty);
				log.debug(String.format("unknown property %s in class %s", p, clazz));
				if (p.isURIResource()) {
					String uri = ((Resource)p.as(Resource.class)).getURI();
					resolveUndefinedPropery(clazz.getOntModel(), uri, undefinedPropertiesPolicy);
				} else {
					// TODO call a new method on PropertyRestrictionVisitor?
					log.warn(String.format("found non-URI property %s in class %s", p, clazz));
				}
			}
			
			restrictionVisitor.visit(restriction);
		}

		/* extended case: is this a composition of several classes? if
		 * so, visit all of them...
		 */
		if ( clazz.isUnionClass() ) {
			log.trace("decomposing union classes");
			decomposeUnionClass(clazz.asUnionClass());
		} else if ( clazz.isIntersectionClass() ) {
			log.trace("decomposing intersection classes");
			decomposeIntersectionClass(clazz.asIntersectionClass());
		} else if ( clazz.isComplementClass() ) {
			log.trace("decomposing complement classes");
			decomposeComplementClass(clazz.asComplementClass());
		}
		
		/* recursive case: visit equivalent and super classes...
		 * note that we can't use an iterator because we might add
		 * properties to the ontology if they're undefined, which can
		 * trigger a ConcurrentModificationException.
		 */
		log.trace("decomposing equivalent classes");
		decomposeEquivalentClasses(clazz.listEquivalentClasses().toSet());
		log.trace("decomposing super classes");
		decomposeSuperClasses(clazz.listSuperClasses(true).toSet());
		
		classVisitor.visitPostDecompose(clazz);
	}

	protected void decomposeUnionClass(UnionClass clazz)
	{
		for (Iterator<?> i = clazz.listOperands(); i.hasNext(); )
			decompose((OntClass)i.next());
	}
	
	protected void decomposeIntersectionClass(IntersectionClass clazz)
	{
		for (Iterator<?> i = clazz.listOperands(); i.hasNext(); )
			decompose((OntClass)i.next());
	}

	protected void decomposeComplementClass(ComplementClass clazz)
	{
		for (Iterator<?> i = clazz.listOperands(); i.hasNext(); )
			decompose((OntClass)i.next());
	}

	protected void decomposeEquivalentClasses(Set<OntClass> clazz)
	{
		for (Object equivalentClass: clazz)
			decompose((OntClass)equivalentClass);
	}

	protected void decomposeSuperClasses(Set<OntClass> set)
	{
		for (Object superClass: set)
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