package org.sadiframework.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

public class KeggUtilsIT
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(KeggUtilsIT.class);
	
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
				//log.info(String.format("record for %s:\n%s", keggId, idToRecordMap.get(keggId)));
			}
		} catch(Exception e) {
			fail(String.format("error occurred invoking KEGG API: ", ExceptionUtils.getStackTrace(e)));
		}
	}
	
	@Test
	public void testConvertKeggIDs() throws IOException
	{
		Collection<String> ids = Arrays.asList(new String[]{"ncbi-geneid:6928", "ncbi-geneid:7157"});
		Map<String, String> idMap = KeggUtils.getKeggIdMap("genes", ids);
		//log.info(idMap.toString());
		assertTrue("missing ncbi-geneid:6928 => hsa:6928", idMap.get("ncbi-geneid:6928").equals("hsa:6928"));
		assertTrue("missing ncbi-geneid:7157 => hsa:7157", idMap.get("ncbi-geneid:7157").equals("hsa:7157"));
	}
}
