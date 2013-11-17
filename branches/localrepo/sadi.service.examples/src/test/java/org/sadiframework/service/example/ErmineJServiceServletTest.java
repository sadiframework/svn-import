package org.sadiframework.service.example;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.sadiframework.utils.RdfUtils;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class ErmineJServiceServletTest
{
	private static final Log log = LogFactory.getLog(ErmineJServiceServletTest.class);
	
	@Test
	public void testConvertInputToRdf() throws Exception
	{
		Model expectedInputModel = ModelFactory.createDefaultModel();
		RdfUtils.loadModelFromString(expectedInputModel, "/t/ermineJ.input.1.rdf", getClass());
		
		Model convertedInputModel = convertInputToRdf(ErmineJServiceServletTest.class.getResourceAsStream("/HG-U133_Plus_2.na26.annot_test.ErmineJ"));
		
		if (log.isTraceEnabled()) {
//		    expectedInputModel.write( new java.io.FileOutputStream("/tmp/expected.n3"), "N-TRIPLES" );
//                    convertedInputModel.write( new java.io.FileOutputStream("/tmp/converted.n3"), "N-TRIPLES" );
			log.trace("Expected input\n" + RdfUtils.logModel(expectedInputModel));
			log.trace("Converted input\n" + RdfUtils.logModel(convertedInputModel));
		}
		
		assertTrue("input parser does not produce expected result", convertedInputModel.isIsomorphicWith(expectedInputModel));
	}
	
	public static Model convertInputToRdf(InputStream is) throws IOException
	{
		Model model = ModelFactory.createDefaultModel();
		Resource GoProbeSet = model.getResource("http://sadiframework.org/examples/ermineJ.owl#GoProbeSet");
		Resource GoProbe = model.getResource("http://sadiframework.org/examples/ermineJ.owl#GoProbe");
		Resource GoAnnotated = model.getResource("http://sadiframework.org/examples/ermineJ.owl#GoAnnotated");
		Resource GoTerm = model.getResource("http://purl.oclc.org/SADI/LSRN/GO_Record");
		Property element = model.getProperty("http://sadiframework.org/examples/common.owl#element");
		Property expressionLevel = model.getProperty("http://sadiframework.org/examples/ermineJ.owl#expressionLevel");
		Property mappedTo = model.getProperty("http://sadiframework.org/examples/ermineJ.owl#mappedTo");
		Property hasInputTerm = model.getProperty("http://sadiframework.org/ontologies/predicates.owl#hasGOTerm");
		
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
			probe.addLiteral(expressionLevel, Double.valueOf(expression));
			
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
		return String.format("http://lsrn.org/GO:%s", goId);
	}
}
