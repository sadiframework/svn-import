package ca.wilkinsonlab.sadi.utils;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class ModelDiffTest
{
	@Test
	public void testDiff()
	{
		Model model1 = ModelFactory.createDefaultModel();
		Model model2 = ModelFactory.createDefaultModel();
		Resource subject = ResourceFactory.createResource("subject");
		Property property = ResourceFactory.createProperty("property");
		Resource objectResource = ResourceFactory.createResource("object");
		Literal objectLiteral = ResourceFactory.createPlainLiteral("literal");
		Resource object1 = ResourceFactory.createResource("object1");
		Literal literal1 = ResourceFactory.createPlainLiteral("literal1");
		Resource object2 = ResourceFactory.createResource("object2");
		Literal literal2 = ResourceFactory.createPlainLiteral("literal2");
		Resource anon1 = model1.createResource();
		Resource anon2 = model2.createResource();
		model1.add(subject, property, objectResource);
		model2.add(subject, property, objectResource);
		model1.add(subject, property, objectLiteral);
		model2.add(subject, property, objectLiteral);
		model1.add(subject, property, object1);
		model2.add(subject, property, object2);
		model1.add(subject, property, literal1);
		model2.add(subject, property, literal2);
		model1.add(anon1, property, objectResource);
		model2.add(anon2, property, objectResource);
		ModelDiff diff = ModelDiff.diff(model1, model2);
		System.out.println("In model1, not model2:" + RdfUtils.logStatements(diff.inXnotY));
		System.out.println("In model2, not model1:" + RdfUtils.logStatements(diff.inYnotX));
	}

}
