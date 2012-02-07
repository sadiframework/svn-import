package ca.wilkinsonlab.sadi.owl2sparql;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.decompose.ClassTracker;
import ca.wilkinsonlab.sadi.decompose.DefaultClassTracker;
import ca.wilkinsonlab.sadi.decompose.RestrictionAdapter;
import ca.wilkinsonlab.sadi.utils.LabelUtils;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class QueryGeneratingRestrictionVisitor extends RestrictionAdapter
{
	private static final Logger log = Logger.getLogger(QueryGeneratingRestrictionVisitor.class);
	
	private ClassTracker tracker;
	private StringBuilder construct;
	private StringBuilder where;
	private VariableGenerator generator;
	private String variable;
	
	public QueryGeneratingRestrictionVisitor(ClassTracker tracker, StringBuilder construct, StringBuilder where, VariableGenerator generator, String variable)
	{
		this.tracker = tracker;
		this.construct = construct;
		this.where = where;
		this.generator = generator;
		this.variable = variable.startsWith("?") ? variable : String.format("?%s", variable);
	}
	
	@Override
	public void visit(Restriction restriction)
	{
		if (log.isTraceEnabled()) {
			log.trace(String.format("visit(%s)", LabelUtils.getRestrictionString(restriction)));
		}
		OntProperty onProperty = restriction.getOnProperty();
		if (onProperty != null) {
			super.visit(restriction);
		} else {
			/* this shouldn't happen; if it does, we'll have to do something
			 * like what we do in getRestrictionString...
			 */
			log.warn(String.format("onProperty was null in restriction %s", LabelUtils.getRestrictionString(restriction)));
		}
	}
	
	@Override
	public void valuesFrom(OntProperty onProperty, OntResource valuesFrom)
	{
		if (log.isTraceEnabled()) {
			log.trace(String.format("valuesFrom(%s, %s)", LabelUtils.getLabel(onProperty), LabelUtils.getLabel(valuesFrom)));
		}
		String nextVariable = generator.nextVariable(LabelUtils.getLabel(valuesFrom));
		construct.append(variable)
	             .append(" <").append(onProperty.getURI()).append("> ")
	             .append(nextVariable).append(" . \n");
		Set<OntProperty> subProperties = OwlUtils.listSubProperties(onProperty, false);
		RdfUtils.removeAnonymousNodes(subProperties);
		if (subProperties.isEmpty()) {
			where.append(variable)
			     .append(" <").append(onProperty.getURI()).append("> ")
	             .append(nextVariable).append(" . \n");
		} else {
			subProperties.add(onProperty);
			for (Iterator<OntProperty> i = subProperties.iterator(); i.hasNext(); ) {
				OntProperty p = i.next();
				where.append("{\n").append(variable)
				     .append(" <").append(p.getURI()).append("> ")
				     .append(nextVariable).append(" . \n}");
				if (i.hasNext())
					where.append(" UNION ");
			}
			where.append(" . \n");
		}
		// TODO check inverse property...
		
		if (valuesFrom.isClass()) {
			QueryGeneratingDecomposer valuesFromDecomposer = 
				new QueryGeneratingDecomposer(new DefaultClassTracker(tracker), construct, where, generator, nextVariable);
			valuesFromDecomposer.decompose(valuesFrom.asClass());
		} else {
			throw new UnsupportedOperationException("non-class valuesFrom not supported");
		}
	}

	@Override
	public void hasValue(OntProperty onProperty, RDFNode hasValue)
	{
		if (log.isTraceEnabled()) {
			log.trace(String.format("hasValue(%s, %s)", LabelUtils.getLabel(onProperty), LabelUtils.toString(hasValue)));
		}
		construct.append(variable)
	             .append(" <").append(onProperty.getURI()).append("> ");
		where.append(variable)
		     .append(" <").append(onProperty.getURI()).append("> ");
		if (hasValue.isURIResource()) {
			construct.append("<").append(hasValue.asResource().getURI()).append(">").append(" . \n");
			where.append("<").append(hasValue.asResource().getURI()).append(">").append(" . \n");
			// TODO check inverse property...
		} else if (hasValue.isAnon()) {
			// start a new variable and enumerate its properties...
			throw new UnsupportedOperationException("value 'anonymous resource' not yet implemented");
		} else if (hasValue.isLiteral()) {
			construct.append(hasValue.toString()).append(" . \n");
			where.append(hasValue.toString()).append(" . \n");
		}
	}

	@Override
	public void minCardinality(OntProperty onProperty, int minCardinality)
	{
		if (log.isTraceEnabled()) {
			log.trace(String.format("minCardinality(%s, %d)", LabelUtils.getLabel(onProperty), minCardinality));
		}
		if (minCardinality < 1) {
			return; // nothing to do...
		} else if (minCardinality == 1) {
			String var1 = generator.nextVariable(variable);
			String var2 = generator.nextVariable(variable);
			construct.append(variable)
		             .append(" <").append(onProperty.getURI()).append("> ")
		             .append(var1).append(" . \n");
			where.append(variable)
			     .append(" <").append(onProperty.getURI()).append("> ")
			     .append(var1).append(" . \n")
			     .append(variable)
			     .append(" <").append(onProperty.getURI()).append("> ")
			     .append(var2).append(" FILTER ").append(var1).append("!=").append(var2);
				// TODO what goes after FILTER?  .?
		} else {
			throw new UnsupportedOperationException("minCardinality > 1 not supported");
		}
	}

	@Override
	public void cardinality(OntProperty onProperty, int cardinality)
	{
		throw new UnsupportedOperationException("exactly cardinality not supported");
	}

	@Override
	public void maxCardinality(OntProperty onProperty, int maxCardinality)
	{
		throw new UnsupportedOperationException("exactly cardinality not supported");
	}
}
