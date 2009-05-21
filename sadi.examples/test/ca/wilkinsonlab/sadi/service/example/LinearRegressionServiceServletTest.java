package ca.wilkinsonlab.sadi.service.example;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;

public class LinearRegressionServiceServletTest
{
	private static final ModelMaker modelMaker = ModelFactory.createMemModelMaker();
	
	@Test
	public void testProcessInputModel()
	{
		Model inputModel = modelMaker.createFreshModel();
		inputModel.read(LinearRegressionServiceServletTest.class.getResourceAsStream("/regression-input.rdf"), "");
		
		Model expectedOutputModel = modelMaker.createFreshModel();
		expectedOutputModel.read(LinearRegressionServiceServletTest.class.getResourceAsStream("/regression-output.rdf"), "");
		
		Model actualOutputModel = new LinearRegressionServiceServlet().processInput(inputModel);
		assertTrue("service does not produce expected output", actualOutputModel.isIsomorphicWith(expectedOutputModel));
	}
}
