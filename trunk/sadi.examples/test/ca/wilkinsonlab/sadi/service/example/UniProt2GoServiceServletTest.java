package ca.wilkinsonlab.sadi.service.example;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;

public class UniProt2GoServiceServletTest
{
	private static final ModelMaker modelMaker = ModelFactory.createMemModelMaker();
	
	@Test
	public void testProcessInputModel()
	{
		Model inputModel = modelMaker.createFreshModel();
		inputModel.read(UniProt2GoServiceServletTest.class.getResourceAsStream("/uniprot2go-input.rdf"), "");
		
		Model expectedOutputModel = modelMaker.createFreshModel();
		expectedOutputModel.read(UniProt2GoServiceServletTest.class.getResourceAsStream("/uniprot2go-output.rdf"), "");
//		System.out.println("Expected output");
//		System.out.println(RdfUtils.logStatements(expectedOutputModel));
		
		Model actualOutputModel = new UniProt2GoServiceServlet().processInput(inputModel);
//		System.out.println("Actual output");
//		System.out.println(RdfUtils.logStatements(actualOutputModel));
		assertTrue("service does not produce expected output", actualOutputModel.isIsomorphicWith(expectedOutputModel));
	}
}
