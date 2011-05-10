package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.Test;


public class SPARQLResultsXMLUtilsTest {

	@Test
	public void testGetResultsFromSPARQLXML()
	{
		URL testFile = SPARQLResultsXMLUtilsTest.class.getResource("sparqlresults.example.xml");
		try {
			List<Map<String,String>> results = SPARQLResultsXMLUtils.getResultsFromSPARQLXML(testFile.openStream());

			assertTrue(results.size() == 5);
			assertTrue(results.get(0).get("s").equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drug_interactions/DB00008_DB01223"));
			assertTrue(results.get(0).get("p").equals("http://www.w3.org/2000/01/rdf-schema#label"));
			assertTrue(results.get(0).get("o").equals("DB00008 DB01223"));
		}
		catch(Exception e) {
			fail("Unable to parse SPARQL results XML: " + e.getMessage());
		}
	}
}
