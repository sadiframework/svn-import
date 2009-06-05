package ca.wilkinsonlab.sadi.service.example;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class ErmineJServiceServletTest
{
	private static final ModelMaker modelMaker = ModelFactory.createMemModelMaker();
	
	public static Model convertInputToRdf(InputStream is) throws IOException
	{
		Model model = modelMaker.createFreshModel();
		Resource GoProbeSet = model.getResource("http://sadiframework.org/examples/ermineJ.owl#GoProbeSet");
		Resource GoProbe = model.getResource("http://sadiframework.org/examples/ermineJ.owl#GoProbe");
		Resource GoAnnotated = model.getResource("http://sadiframework.org/examples/ermineJ.owl#GoAnnotated");
		Resource GoTerm = model.getResource("http://purl.oclc.org/SADI/LSRN/GO_Record");
		Property element = model.getProperty("http://sadiframework.org/examples/common.owl#element");
		Property expressionLevel = model.getProperty("http://sadiframework.org/examples/ermineJ.owl#expressionLevel");
		Property mappedTo = model.getProperty("http://sadiframework.org/examples/ermineJ.owl#mappedTo");
		Property hasInputTerm = model.getProperty("http://es-01.chibi.ubc.ca/~benv/predicates.owl#hasGOTerm");
		
		String inputUri = "http://sadiframework.org/examples/input/erminej1";
		String inputPrefix = inputUri + "#";
		Resource input = model.createResource(inputUri, GoProbeSet);
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("#"))
				continue;
			
			String[] fields = line.split("\t");
			String probeId = fields[0];
			String expression = fields[1];
			String geneId = fields[2];
			String[] goTerms = fields[4].split("\\|");
			
			Resource probe = model.createResource(inputPrefix + probeId, GoProbe);
			probe.addProperty(expressionLevel, expression);
			
			Resource gene = model.createResource(inputPrefix + geneId, GoAnnotated);
			for (String goTerm: goTerms) {
				Resource goRecord = model.createResource(getGoUri(goTerm), GoTerm);
				gene.addProperty(hasInputTerm, goRecord);
			}
			
			probe.addProperty(mappedTo, gene);
			input.addProperty(element, probe);
		}
		return model;
	}
	
	public static String getGoUri(String goId)
	{
		if (goId.startsWith("GO:"))
			goId = goId.substring(3);
		return String.format("%s%s", "http://biordf.net/moby/GO/", goId);
	}
	
	@Test
	public void testConvertInputToRdf() throws Exception
	{
		Model expectedInputModel = modelMaker.createFreshModel();
		expectedInputModel.read(ErmineJServiceServletTest.class.getResourceAsStream("/ermineJ-input.rdf"), "");
		
		Model convertedModel = convertInputToRdf(ErmineJServiceServletTest.class.getResourceAsStream("/HG-U133_Plus_2.na26.annot_test.ErmineJ"));
		assertTrue("input parser does not produce expected result", convertedModel.isIsomorphicWith(expectedInputModel));
	}
	
	@Test
	public void testProcessInputModel() throws Exception
	{
		Model inputModel = convertInputToRdf(ErmineJServiceServletTest.class.getResourceAsStream("/HG-U133_Plus_2.na26.annot_test.ErmineJ"));
//		System.out.println("Input");
//		System.out.println(RdfUtils.logStatements(inputModel));
		
		Model expectedOutputModel = modelMaker.createFreshModel();
		expectedOutputModel.read(ErmineJServiceServletTest.class.getResourceAsStream("/ermineJ-output.rdf"), "");
//		System.out.println("Expected output");
//		System.out.println(RdfUtils.logStatements(expectedOutputModel));
		
		Model actualOutputModel = new ErmineJServiceServlet().processInput(inputModel);
//		System.out.println("Actual output");
//		System.out.println(RdfUtils.logStatements(actualOutputModel));
		assertTrue("service does not produce expected output", actualOutputModel.isIsomorphicWith(expectedOutputModel));
	}
}
