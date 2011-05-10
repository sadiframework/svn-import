package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

public class KeggUtilsTest 
{
	private static final Logger log = Logger.getLogger(KeggUtilsTest.class);
	
	// Note: This test will fail if KEGG is down.
	@Test
	public void testGetKeggRecords()
	{
		String[] keggIds = { "hsa:79587", "path:ko00232", "cpd:C00013" }; 

		Collection<String> ids = new ArrayList<String>();
		for(String keggGeneId : keggIds) {
			ids.add(keggGeneId);
		}

		Map<String,String> idToRecordMap = null;
		try {
			idToRecordMap = KeggUtils.getKeggRecords(ids);
			for(String keggId : keggIds) {
				assertTrue(idToRecordMap.containsKey(keggId));
				log.info(String.format("record for %s:\n%s", keggId, idToRecordMap.get(keggId)));
			}
		} catch(Exception e) {
			fail(String.format("error occurred invoking KEGG API: ", ExceptionUtils.getStackTrace(e)));
		}
	}
	
	@Test
	public void testGetSectionsFromKeggRecord() 
	{
		/*
		 * Note: "REFERENCE" is intentionally omitted here. REFERENCE sections
		 * are not returned by getSectionsFromKeggRecord() because they are
		 * only type of section that can have more than one instance in a 
		 * single record.
		 */
		
		String[] sectionLabels = { 
				"ENTRY", 
				"NAME", 
				"DESCRIPTION", 
				"CLASS",
				"PATHWAY_MAP",
				"DISEASE",
				"DRUG",
				"DBLINKS",
				"ORGANISM",
				"GENE",
				"COMPOUND",
				"REL_PATHWAY",
				"KO_PATHWAY",
		};
		
		String expectedTextForDRUG = 
			"D00123  Cyanamide (JP15)\n" +
			"D00131  Disulfiram (JP15/USP/INN)\n" +
			"D00707  Fomepizole (USAN/INN)";
		
		try {
			
			String exampleRecord = SPARQLStringUtils.readFully(KeggUtils.class.getResourceAsStream("/kegg.pathway.hsa00010.txt"));
			Map<String,String> sectionMap = KeggUtils.getSectionsFromKeggRecord(exampleRecord);
			
			for(String label : sectionLabels) {
				assertTrue(sectionMap.containsKey(label));
			}
			
			assertTrue(sectionMap.get("DRUG").equals(expectedTextForDRUG));
		
		} catch(Exception e) {
			fail(String.format("error occurred reading input file for test: ", ExceptionUtils.getStackTrace(e)));
		}

	}
}
