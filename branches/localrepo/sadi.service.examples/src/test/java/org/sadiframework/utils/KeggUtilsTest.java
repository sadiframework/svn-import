package org.sadiframework.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;
import org.sadiframework.utils.KeggUtils;

public class KeggUtilsTest 
{
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
