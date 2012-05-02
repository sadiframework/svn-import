package org.sadiframework.utils;

import java.util.Iterator;

import org.sadiframework.decompose.ClassVisitor;
import org.sadiframework.decompose.RestrictionAdapter;
import org.sadiframework.decompose.VisitingDecomposer;


import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class DummyInstanceCreator extends RestrictionAdapter implements ClassVisitor
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
	 * @see org.sadiframework.decompose.ClassVisitor#ignore(com.hp.hpl.jena.ontology.OntClass)
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
	 * @see org.sadiframework.decompose.ClassVisitor#visitPreDecompose(com.hp.hpl.jena.ontology.OntClass)
	 */
	@Override
	public void visitPreDecompose(OntClass c)
	{
		if (c.isURIResource())
			model.add(individual, RDF.type, c);
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.decompose.ClassVisitor#visitPostDecompose(com.hp.hpl.jena.ontology.OntClass)
	 */
	public void visitPostDecompose(OntClass c)
	{
	}

	/* (non-Javadoc)
	 * @see org.sadiframework.decompose.RestrictionAdapter#onProperty(com.hp.hpl.jena.ontology.OntProperty)
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
	 * @see org.sadiframework.decompose.RestrictionAdapter#minCardinality(com.hp.hpl.jena.ontology.OntProperty, int)
	 */
	@Override
	public void minCardinality(OntProperty onProperty, int minCardinality)
	{
		cardinality(onProperty, minCardinality);
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.decompose.RestrictionAdapter#cardinality(com.hp.hpl.jena.ontology.OntProperty, int)
	 */
	@Override
	public void cardinality(OntProperty onProperty, int cardinality)
	{
		for (int currentCardinality = individual.listProperties(onProperty).toList().size(); currentCardinality<cardinality; ++currentCardinality)
			onProperty(onProperty);
	}

	/* (non-Javadoc)
	 * @see org.sadiframework.decompose.RestrictionAdapter#hasValue(com.hp.hpl.jena.ontology.OntProperty, com.hp.hpl.jena.rdf.model.RDFNode)
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
	 * @see org.sadiframework.decompose.RestrictionAdapter#valuesFrom(com.hp.hpl.jena.ontology.OntProperty, com.hp.hpl.jena.ontology.OntResource)
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