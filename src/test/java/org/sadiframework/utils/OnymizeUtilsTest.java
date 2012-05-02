package org.sadiframework.utils;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sadiframework.utils.LabelUtils;
import org.sadiframework.utils.OnymizeUtils;
import org.sadiframework.utils.OwlUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class OnymizeUtilsTest
{
//	private static final Logger log = Logger.getLogger(OnymizeUtilsTest.class);
	
	static final String NS = "http://sadiframework.org/ontologies/OnymizeUtilsTest.owl#";
	
	static OntModel model;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		model.read( OwlUtilsTest.class.getResourceAsStream("OnymizeUtilsTest.owl"), NS );
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		model.close();
	}
	
	@Test
	public void testGetClassByLabel()
	{
		OntClass classWithRestriction = model.getOntClass(NS + "ClassWithRestriction");
		Restriction r = OwlUtils.listRestrictions(classWithRestriction).iterator().next();
		String label = LabelUtils.getLabel(r);
		OntClass r2 = OnymizeUtils.getClassByLabel(model, label);
		assertEquals(r, r2);
	}
}
