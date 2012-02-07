package ca.wilkinsonlab.sadi.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

import ca.wilkinsonlab.sadi.decompose.ClassTracker;
import ca.wilkinsonlab.sadi.decompose.ClassVisitor;
import ca.wilkinsonlab.sadi.decompose.RestrictionAdapter;
import ca.wilkinsonlab.sadi.decompose.VisitingDecomposer;

public class MinimalModelDecomposer extends VisitingDecomposer
{
	private static final Logger log = Logger.getLogger(MinimalModelDecomposer.class);
	
	private Model model;
	private Resource subject;
	private OntClass asClass;
	private Set<String> visited;

	public MinimalModelDecomposer(Resource individual, OntClass asClass)
	{
		this(ModelFactory.createDefaultModel(), individual, asClass);
	}
	
	public MinimalModelDecomposer(Model model, Resource individual, OntClass asClass)
	{
		this(model, individual, asClass, new HashSet<String>());
	}
	
	public MinimalModelDecomposer(Model model, Resource individual, OntClass asClass, Set<String> visited)
	{
		this.model = model;
		this.subject = individual;
		this.asClass = asClass;
		this.visited = visited;
		this.tracker = new MinimalModelClassTracker();
		this.classVisitor = new MinimalModelClassVisitor();
		this.restrictionVisitor = new MinimalModelRestrictionVisitor();
	}
	
	public void decompose()
	{
		decompose(asClass);
	}
	
	public Model getModel()
	{
		return model;
	}
	
	private static String getHashKey(Resource individual, Resource asClass)
	{
		return String.format("%s %s", individual, asClass);
//				individual.isURIResource() ? individual.getURI() : individual.getId(),
//				asClass.isURIResource() ? asClass.getURI() : asClass.getId()
//		);
	}
	
	private class MinimalModelRestrictionVisitor extends RestrictionAdapter
	{
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
						new MinimalModelDecomposer(model, object, clazz, visited).decompose();
					}
				}
			}
		}
	}
	
	private class MinimalModelClassTracker implements ClassTracker
	{
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
	}
	
	private class MinimalModelClassVisitor implements ClassVisitor
	{
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
		 * @see ca.wilkinsonlab.sadi.decompose.ClassVisitor#visitPreDecompose(com.hp.hpl.jena.ontology.OntClass)
		 */
		@Override
		public void visitPreDecompose(OntClass c)
		{
			if (log.isTraceEnabled())
				log.trace(String.format("visiting %s as %s", subject, LabelUtils.getLabel(c)));
			
			/* if the individual is explicitly declared as a member of the 
			 * target class, add that type statement to the model...
			 */
			if (c.isURIResource() && subject.hasProperty(RDF.type, c))
				model.add(subject, RDF.type, c);
		}
		
		/* (non-Javadoc)
		 * @see ca.wilkinsonlab.sadi.decompose.ClassVisitor#visitPostDecompose(com.hp.hpl.jena.ontology.OntClass)
		 */
		public void visitPostDecompose(OntClass c)
		{
		}
	}
}
