package org.sadiframework.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.sadiframework.utils.KeggUtils;

public class KeggUtilsIT
{
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
				log.info(String.format("record for %s:\n%s", keggId, idToRecordMap.get(keggId)));
			}
		} catch(Exception e) {
			fail(String.format("error occurred invoking KEGG API: ", ExceptionUtils.getStackTrace(e)));
		}
	}
}
