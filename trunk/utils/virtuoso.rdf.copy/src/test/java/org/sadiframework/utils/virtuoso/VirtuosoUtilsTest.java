package org.sadiframework.utils.virtuoso;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sadiframework.utils.JDBCUtils;
import org.sadiframework.utils.virtuoso.VirtuosoServerUtils.VirtuosoProcess;
import org.sadiframework.vocab.Virtuoso;

import com.google.common.io.Files;

public class VirtuosoUtilsTest {

	protected final static String TEST_GRAPH = "test_graph";
	protected final static String TEST_GRAPH_2 = "test_graph_2";
	protected final static String TEST_USER = "test_user";
	protected final static String TEST_USER_2 = "test_user_2";

	protected static final String TEST_TTL_FILE = "test.data.ttl";

	@Test
	public void testMergeGraphPerms() {

		Map<String,Map<String,Integer>> srcPerms = new HashMap<String,Map<String,Integer>>();
		Map<String,Map<String,Integer>> destPerms = new HashMap<String,Map<String,Integer>>();
		Map<String,Map<String,Integer>> mergedPerms;

 		/*
		 * Test 1: Demotion of default perms for 'nobody', due to user perms on a specific graph
		 *
		 * src:    {test_graph={test_user=3}}
		 * dest:   {null={nobody=7}}
		 * merged: {null={nobody=3}, test_graph={test_user=3}}
		 */

		srcPerms.clear();
		srcPerms.put(TEST_GRAPH, new HashMap<String,Integer>());
		srcPerms.get(TEST_GRAPH).put(TEST_USER, Virtuoso.GRAPH_PERM_READ_BIT|Virtuoso.GRAPH_PERM_WRITE_BIT);

		destPerms.clear();
		srcPerms.put(null, new HashMap<String,Integer>());
		srcPerms.get(null).put(Virtuoso.NOBODY_USER, Virtuoso.GRAPH_PERM_ALL_BITS);

		mergedPerms = VirtuosoUtils.mergeGraphPerms(srcPerms, destPerms);

		assertTrue(mergedPerms.get(null).get(Virtuoso.NOBODY_USER) == (Virtuoso.GRAPH_PERM_READ_BIT|Virtuoso.GRAPH_PERM_WRITE_BIT));
		assertTrue(mergedPerms.get(TEST_GRAPH).get(TEST_USER) == (Virtuoso.GRAPH_PERM_READ_BIT|Virtuoso.GRAPH_PERM_WRITE_BIT));
		assertFalse(mergedPerms.get(null).containsKey(TEST_USER));
		assertFalse(mergedPerms.get(TEST_GRAPH).containsKey(Virtuoso.NOBODY_USER));

 		/*
		 * Test 2: Demotion of default perms for 'nobody', due to default perms for a user
		 *
		 * src:    {null={test_user=1}}
		 * dest:   {null={nobody=7}}
		 * merged: {null={nobody=1, test_user=1}}
		 */

		srcPerms.clear();
		srcPerms.put(null, new HashMap<String,Integer>());
		srcPerms.put(TEST_GRAPH, new HashMap<String,Integer>());
		srcPerms.get(TEST_GRAPH).put(TEST_USER, Virtuoso.GRAPH_PERM_READ_BIT);

		destPerms.clear();
		srcPerms.put(null, new HashMap<String,Integer>());
		srcPerms.get(null).put(Virtuoso.NOBODY_USER, Virtuoso.GRAPH_PERM_ALL_BITS);

		mergedPerms = VirtuosoUtils.mergeGraphPerms(srcPerms, destPerms);

		assertTrue(mergedPerms.get(null).get(Virtuoso.NOBODY_USER) == Virtuoso.GRAPH_PERM_READ_BIT);
		assertTrue(mergedPerms.get(TEST_GRAPH).get(TEST_USER) == Virtuoso.GRAPH_PERM_READ_BIT);

		/*
		 * Test 3: Creation of default perms for 'nobody', due to default perms for a user
		 *
		 * src:    {null={test_user=1}}
		 * dest:   {}
		 * merged: {null={nobody=1, test_user=1}}
		 */

		srcPerms.clear();
		srcPerms.put(TEST_GRAPH, new HashMap<String,Integer>());
		srcPerms.get(TEST_GRAPH).put(TEST_USER, Virtuoso.GRAPH_PERM_READ_BIT);

		destPerms.clear();

		mergedPerms = VirtuosoUtils.mergeGraphPerms(srcPerms, destPerms);

		assertTrue(mergedPerms.get(null).get(Virtuoso.NOBODY_USER) == Virtuoso.GRAPH_PERM_READ_BIT);
		assertTrue(mergedPerms.get(TEST_GRAPH).get(TEST_USER) == Virtuoso.GRAPH_PERM_READ_BIT);

		/*
		 * Test 4: Src permissions overwrite dest permission for a regular user
		 *
		 * src:    {null={nobody=0}, test_graph={test_user=3}}
		 * dest:   {null={nobody=0}, test_graph={test_user=7}}
		 * merged: {null={nobody=0}, test_graph={test_user=3}}
		 */

		srcPerms.clear();
		srcPerms.put(null, new HashMap<String,Integer>());
		srcPerms.get(null).put(Virtuoso.NOBODY_USER, 0);
		srcPerms.put(TEST_GRAPH, new HashMap<String,Integer>());
		srcPerms.get(TEST_GRAPH).put(TEST_USER, Virtuoso.GRAPH_PERM_READ_BIT|Virtuoso.GRAPH_PERM_WRITE_BIT);

		destPerms.clear();
		destPerms.put(null, new HashMap<String,Integer>());
		destPerms.get(null).put(Virtuoso.NOBODY_USER, 0);
		destPerms.put(TEST_GRAPH, new HashMap<String,Integer>());
		destPerms.get(TEST_GRAPH).put(TEST_USER, Virtuoso.GRAPH_PERM_ALL_BITS);

		mergedPerms = VirtuosoUtils.mergeGraphPerms(srcPerms, destPerms);

		assertTrue(mergedPerms.get(null).get(Virtuoso.NOBODY_USER) == 0);
		assertTrue(mergedPerms.get(TEST_GRAPH).get(TEST_USER) == (Virtuoso.GRAPH_PERM_READ_BIT|Virtuoso.GRAPH_PERM_WRITE_BIT));

		/*
		 * Test 5: Demote 'nobody' perms for a graph, due to existence of another user for graph at dest
		 *
		 * src:    {null={nobody=0}, test_graph={nobody=3, test_user=3}}
		 * dest:   {null={nobody=0}, test_graph={test_user_2=1}}
		 * merged: {null={nobody=0}, test_graph={nobody=1, test_user=3, test_user_2=1}}
		 */

		srcPerms.clear();
		srcPerms.put(null, new HashMap<String,Integer>());
		srcPerms.get(null).put(Virtuoso.NOBODY_USER, 0);
		srcPerms.put(TEST_GRAPH, new HashMap<String,Integer>());
		srcPerms.get(TEST_GRAPH).put(Virtuoso.NOBODY_USER, Virtuoso.GRAPH_PERM_READ_BIT|Virtuoso.GRAPH_PERM_WRITE_BIT);
		srcPerms.get(TEST_GRAPH).put(TEST_USER, Virtuoso.GRAPH_PERM_READ_BIT|Virtuoso.GRAPH_PERM_WRITE_BIT);

		destPerms.clear();
		destPerms.put(null, new HashMap<String,Integer>());
		destPerms.get(null).put(Virtuoso.NOBODY_USER, 0);
		destPerms.put(TEST_GRAPH, new HashMap<String,Integer>());
		destPerms.get(TEST_GRAPH).put(TEST_USER_2, Virtuoso.GRAPH_PERM_READ_BIT);

		mergedPerms = VirtuosoUtils.mergeGraphPerms(srcPerms, destPerms);

		assertTrue(mergedPerms.get(null).get(Virtuoso.NOBODY_USER) == 0);
		assertTrue(mergedPerms.get(TEST_GRAPH).get(Virtuoso.NOBODY_USER) == Virtuoso.GRAPH_PERM_READ_BIT);
		assertTrue(mergedPerms.get(TEST_GRAPH).get(TEST_USER) == (Virtuoso.GRAPH_PERM_READ_BIT|Virtuoso.GRAPH_PERM_WRITE_BIT));
		assertTrue(mergedPerms.get(TEST_GRAPH).get(TEST_USER_2) == Virtuoso.GRAPH_PERM_READ_BIT);

	}


	@Test
	public void testWriteTTL() throws IOException, SQLException {

		VirtuosoProcess process = null;
		Connection conn = null;

		try {

			process = VirtuosoServerUtils.startVirtuosoInstance();

			// insert some relative URIs into a graph

			conn = VirtuosoUtils.getPersistentJDBCConnection(process.getHost(), process.getISQLPort(), "dba", "dba");
			JDBCUtils.executeUpdateQuery(conn, String.format("SPARQL INSERT INTO GRAPH <%s> { <a> <b> <c> }", TEST_GRAPH));

			// check that relative URIs are written out unmodified from Virtuoso

			File tempDir = Files.createTempDir();
			File outputFile = new File(tempDir, "output.rdf");
			tempDir.deleteOnExit();
			outputFile.deleteOnExit();

			OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));

			VirtuosoUtils.writeTTL(
					process.getHost(),
					process.getISQLPort(),
					"dba",
					"dba",
					TEST_GRAPH,
					os
				);

			os.close();

			InputStream is = new FileInputStream(outputFile);
			String ttl = IOUtils.toString(is);
			is.close();

			JDBCUtils.executeUpdateQuery(conn, String.format("DB.DBA.TTLP('%s', '', '%s')", ttl, TEST_GRAPH_2));

			String query = String.format("SPARQL SELECT * FROM <%s> WHERE { <a> <b> <c> }", TEST_GRAPH_2);
			assertTrue(JDBCUtils.executeQuery(conn, query).size() == 1);

		} finally {
			if (process != null)
				process.shutdown();
			if (conn != null)
				conn.close();
		}

	}

}
