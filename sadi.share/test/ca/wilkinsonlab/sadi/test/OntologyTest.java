package ca.wilkinsonlab.sadi.test;

import ca.wilkinsonlab.sadi.utils.OwlUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class OntologyTest
{
	public static void main(String[] args)
	{
		OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_RULE_INF );
		OntProperty belongsToPathway = OwlUtils.getOntPropertyWithLoad(model, "http://sadiframework.org/ontologies/predicates.owl#belongsToPathway");
		OntProperty inverseProperty = belongsToPathway.getInverse();
		System.out.println(inverseProperty);
	}
}
