package org.sadiframework.decompose;

import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Luke McCarthy
 */
public class RestrictionAdapter implements RestrictionVisitor
{
	/* (non-Javadoc)
	 * @see org.sadiframework.decompose.RestrictionVisitor#visit(com.hp.hpl.jena.ontology.Restriction)
	 */
	public void visit(Restriction restriction)
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
			} else if ( restriction.isCardinalityRestriction() ) {
				if ( restriction.isMinCardinalityRestriction()) {
					minCardinality(onProperty, restriction.asMinCardinalityRestriction().getMinCardinality());
				} else if (restriction.isMaxCardinalityRestriction()) {
					maxCardinality(onProperty, restriction.asMaxCardinalityRestriction().getMaxCardinality());
				} else {
					cardinality(onProperty, restriction.asCardinalityRestriction().getCardinality());
				}
			} else {
				onProperty(onProperty); // TODO is this even possible?
			}
		}
	}
	
	/**
	 * 
	 * @param onProperty
	 */
	public void onProperty(OntProperty onProperty)
	{
	}
	
	/**
	 * 
	 * @param onProperty
	 * @param valuesFrom
	 */
	public void valuesFrom(OntProperty onProperty, OntResource valuesFrom)
	{
		onProperty(onProperty);
	}
	
	/**
	 * 
	 * @param onProperty
	 * @param hasValue
	 */
	public void hasValue(OntProperty onProperty, RDFNode hasValue)
	{
		onProperty(onProperty);
	}
	
	/**
	 * 
	 * @param onProperty
	 * @param minCardinality
	 */
	public void minCardinality(OntProperty onProperty, int minCardinality)
	{
		onProperty(onProperty);
	}
	
	/**
	 * 
	 * @param onProperty
	 * @param cardinality
	 */
	public void cardinality(OntProperty onProperty, int cardinality)
	{
		onProperty(onProperty);
	}
	
	/**
	 * 
	 * @param onProperty
	 * @param maxCardinality
	 */
	public void maxCardinality(OntProperty onProperty, int maxCardinality)
	{
		onProperty(onProperty);
	}
}