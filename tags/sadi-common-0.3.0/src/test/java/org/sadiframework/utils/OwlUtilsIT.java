package org.sadiframework.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.sadiframework.utils.OwlUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class OwlUtilsIT
{
	private static final Logger log = Logger.getLogger(OwlUtilsIT.class);
	
	@Test
	public void testLoadMinimalOntologyForUri() throws Exception
	{
		OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		String pURI = "http://sadiframework.org/ontologies/test/InversePropertyTest.owl#p";
		String qURI = "http://sadiframework.org/ontologies/test/InversePropertyTest.owl#q";
		OwlUtils.loadMinimalOntologyForUri(model, pURI);
		OntProperty p = model.getOntProperty(pURI);
		assertNotNull(String.format("minimal model missing property %s", p), p);
		OntProperty q = model.getOntProperty(qURI);
		assertNotNull(String.format("minimal model missing property %s", q), q);
		assertTrue(String.format("%s is not an inverse of %s", p, q), p.isInverseOf(q));
		assertTrue(String.format("%s is not an inverse of %s", q, p), q.isInverseOf(p));
	}

	@Test
	public void testLoadMinimalOntologyFromUri()
	{
		String MINIMAL_ONTOLOGY_NS = "http://sadiframework.org/ontologies/test/MinimalOntologyTest.owl#";
		String rootUri = MINIMAL_ONTOLOGY_NS + "ClassD";

		Resource classD = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "ClassD");
		Resource classA = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "ClassA");
		Resource classB = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "ClassB");
		Resource classC = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "ClassC");
		Resource propA = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "PropertyA");
		Resource propB = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "PropertyB");
		Resource propC = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "PropertyC");
		Resource propD = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "PropertyD");
		Resource propE = ResourceFactory.createResource(MINIMAL_ONTOLOGY_NS + "PropertyE");
		
		try {
			OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
			OwlUtils.loadMinimalOntologyForUri(ontModel, rootUri);
			
			// stuff that the minimal model should have
			assertTrue(String.format("minimal ontology is missing %s", classA), ontModel.containsResource(classA));
			assertTrue(String.format("minimal ontology is missing %s", classB), ontModel.containsResource(classB));
			assertTrue(String.format("minimal ontology is missing %s", classD), ontModel.containsResource(classD));
			assertTrue(String.format("minimal ontology is missing %s", propA), ontModel.containsResource(propA));
			assertTrue(String.format("minimal ontology is missing %s", propB), ontModel.containsResource(propB));
			assertTrue(String.format("minimal ontology is missing %s", propC), ontModel.containsResource(propC));
			assertTrue(String.format("minimal ontology is missing %s", propD), ontModel.containsResource(propD));

			// stuff that the minimal model shouldn't have
			assertFalse(String.format("minimal ontology should not contain %s", classC), ontModel.containsResource(classC));
			assertFalse(String.format("minimal ontology should not contain %s", propE), ontModel.containsResource(propE));
			
		} catch(Exception e) {
			log.error("failed to load minimal ontology for %s", e);
			fail(String.format("failed to load minimal ontology for %s:\n%s", rootUri, e.getMessage()));
		}
	}
	
	// This isn't working right now; figure out why...
//	@Test
//	public void testCreateDummyInstance() throws Exception
//	{
//		OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
//		OntClass clazz = OwlUtils.getOntClassWithLoad(model, "http://semanticscience.org/sadi/ontology/lipinskiserviceontology.owl#hbdasmilesmolecule");
//		Resource instance = OwlUtils.createDummyInstance(clazz);
//		System.out.println(RdfUtils.logStatements(instance.getModel()));
//		model.addSubModel(instance.getModel());
//		assertTrue(String.format("dummy is not an instance of %s", clazz), clazz.listInstances().toSet().contains(instance));
//	}
}
