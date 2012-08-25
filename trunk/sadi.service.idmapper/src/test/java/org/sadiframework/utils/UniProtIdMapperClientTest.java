package org.sadiframework.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class UniProtIdMapperClientTest {

	private static final Log log = LogFactory.getLog(UniProtIdMapperClient.class);

	@Test
	public void testInvokeUniprotIdMapper() throws IOException
	{
		URL url = UniProtIdMapperClientTest.class.getResource("/random.sgd.ids");
		List<String> sgdIds = FileUtils.readLines(new File(url.getPath()));
		Map<String, String> mappings = UniProtIdMapperClient.invoke("SGD_ID", "ACC", sgdIds);
		log.info(String.format("UniProt ID service returned %d mappings for %d input IDs: %s", mappings.size(), sgdIds.size(), mappings));
	}

}