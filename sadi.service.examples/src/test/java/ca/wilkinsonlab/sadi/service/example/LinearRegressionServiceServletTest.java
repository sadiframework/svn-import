package ca.wilkinsonlab.sadi.service.example;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.util.List;

import org.junit.Test;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class LinearRegressionServiceServletTest
{
	@Test
	public void testOWL() throws Exception
	{
		OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		model.read( new FileInputStream("src/main/webapp/regression.owl"), "" );
		model.read( new FileInputStream("src/test/resources/regression-input-extra.rdf"), "" );
		model.prepare();
		
		OntProperty x = model.getOntProperty( "http://sadiframework.org/examples/regression.owl#x" );
		assertFalse( "failed to find values of x", model.listResourcesWithProperty(x).toList().isEmpty() );
		
		OntClass hasX = model.getOntClass( "http://sadiframework.org/examples/regression.owl#hasX" );
		assertFalse( "failed to identify dynamic hasX", hasX.listInstances().toList().isEmpty() );

		OntClass pairedValue = model.getOntClass( "http://sadiframework.org/examples/regression.owl#PairedValue" );
		assertFalse( "failed to identify dynamic PairedValues", pairedValue.listInstances().toList().isEmpty() );
		
		OntClass datedValue = model.getOntClass( "http://sadiframework.org/examples/regression.owl#DatedValue" );
		assertFalse( "failed to identify dynamic DatedValues", datedValue.listInstances().toList().isEmpty() );
		
		OntClass inputClass = model.getOntClass( "http://sadiframework.org/examples/regression.owl#InputClass" );
		List<? extends OntResource> list = inputClass.listInstances().toList();
		for (int i=2; i<=4; ++i) {
			String inputURI = String.format("http://sadiframework.org/examples/input/regression%d", i);
			assertTrue( String.format("failed to identify %s instance %s", inputClass, inputURI),
					list.contains( model.getOntResource(inputURI) ) );
		}
	}
}
