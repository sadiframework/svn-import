package org.sadiframework.utils.virtuoso;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.sadiframework.utils.JDBCUtils;
import org.sadiframework.utils.virtuoso.VirtuosoServerUtils.VirtuosoProcess;
import org.sadiframework.vocab.Virtuoso;

import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.virtual.sparql.VirtuosoSPARQLEndpoint;

import com.google.common.io.Files;

/**
 * Test for VirtuosoRDFCopy, a commandline tool for copying the
 * contents of a Virtuoso RDF quad store to another Virtuoso RDF
 * quad store.
 *
 * This class is not a JUnit test because it is not fully automated;
 * it requires user to input passwords during the test.  When
 * connecting to the src/dest Virtuoso instances, enter the default
 * password "dba".  When creating the user "test_user" at the
 * dest Virtuoso instance, choose the password "password".
 */
public class VirtuosoRDFCopyTest {

	protected static final Logger log = Logger.getLogger(VirtuosoRDFCopyTest.class);

	protected static Connection srcAdminConn = null;
	protected static VirtuosoProcess srcVirtuosoProcess = null;
	protected static VirtuosoProcess destVirtuosoProcess = null;
	protected static final String TEST_GRAPH = "test_graph";
	protected static final String TEST_USER = "test_user";
	protected static final String TEST_PASSWORD = "password";
	protected static final String TEST_TTL_FILE = "test.data.ttl";
	protected static final String TEST_SCRIPT = "test.script.sh";
	protected static SPARQLEndpoint srcSPARQLEndpoint;
	protected static SPARQLEndpoint destSPARQLEndpoint;

	protected static final String COUNT_QUERY_TEMPLATE = "SELECT COUNT(*) FROM <%s> WHERE { ?s ?p ?o }";
	protected static final String TEST_INSERT_QUERY = String.format("INSERT INTO GRAPH <%s> { <a> <b> <c> }", TEST_GRAPH);

	public static void main(String argv[]) throws IOException, SQLException {

		List<Test> tests = new ArrayList<Test>();
		tests.add(new Test("RDF copy with no locking", new String[] {}));
		tests.add(new Test("RDF copy with source lock",	new String[] {"--lock-src"}));
		tests.add(new Test("RDF copy with dest lock", new String[] {"--lock-dest"}));
		tests.add(new Test("RDF copy with src and dest lock", new String[] {"--lock-src", "--lock-dest"}));

		for (Test test : tests) {
			log.info(String.format("RUNNING TEST '%s'", test.name));
			try {
				testRDFCopy(test.argv);
				log.info(String.format("Test '%s' PASSED", test.name));
			} catch(Exception e) {
				log.info(String.format("Test '%s' FAILED", test.name), e);
			}
		}

	}

	public static void testRDFCopy(String extraArgs[]) throws IOException, SQLException {

		try {

			setup();

			List<Map<String,String>> results;

			// confirm that the test graph does not yet exist in dest

			results = destSPARQLEndpoint.selectQuery(String.format(COUNT_QUERY_TEMPLATE, TEST_GRAPH));
			assert (getCountQueryResult(results)== 0);

			// backup overwritten graphs in /tmp

			File tempDir = Files.createTempDir();

			// build argv

			List<String> argv = new ArrayList<String>();

			String standardArgs[] = new String[] {
					"--overwrite-graphs",
					"--backup-dir", tempDir.toString(),
					String.valueOf(srcVirtuosoProcess.getISQLPort()),
					String.valueOf(destVirtuosoProcess.getISQLPort()),
			};

			for(String arg : standardArgs)
				argv.add(arg);

			for(String arg : extraArgs)
				argv.add(arg);

			// do the copy

			VirtuosoRDFCopy.main(argv.toArray(new String[0]));

			// confirm that test graph was correctly copied

			results = destSPARQLEndpoint.selectQuery(String.format(COUNT_QUERY_TEMPLATE, TEST_GRAPH));
			assert (getCountQueryResult(results)== 1);

			String query = String.format("SPARQL SELECT * FROM <%s> WHERE { <a> <b> <c> }", TEST_GRAPH);
			assert destSPARQLEndpoint.selectQuery(query).size() == 1;

			// confirm that permissions for TEST_USER were copied to dest

			String destSPARQLURL = String.format("http://%s:%d/sparql-auth", destVirtuosoProcess.getHost(), destVirtuosoProcess.getHTTPPort());
			SPARQLEndpoint destAuthSPARQLEndpoint = new VirtuosoSPARQLEndpoint(destSPARQLURL, TEST_USER, TEST_PASSWORD);

			destAuthSPARQLEndpoint.updateQuery(TEST_INSERT_QUERY);

		} finally {
			teardown();
		}

	}

	public static void setup() throws IOException, SQLException {

		srcVirtuosoProcess = VirtuosoServerUtils.startVirtuosoInstance();
		srcAdminConn = VirtuosoUtils.getPersistentJDBCConnection(srcVirtuosoProcess.getHost(), srcVirtuosoProcess.getISQLPort(), "dba", "dba");

		destVirtuosoProcess = VirtuosoServerUtils.startVirtuosoInstance();

		// 'nobody' user indicates default permissions for users of auth SPARQL endpoint

		VirtuosoUtils.setDefaultGraphPerms(srcAdminConn, Virtuoso.NOBODY_USER, Virtuoso.GRAPH_PERM_READ_BIT);

		// create an authorized SPARQL user

		VirtuosoUtils.createUser(srcAdminConn, TEST_USER, TEST_PASSWORD);
		VirtuosoUtils.setGraphPerms(srcAdminConn, TEST_GRAPH, TEST_USER, (Virtuoso.GRAPH_PERM_READ_BIT | Virtuoso.GRAPH_PERM_WRITE_BIT));

		// load some test data into a named graph in src

		JDBCUtils.executeUpdateQuery(srcAdminConn, String.format("SPARQL %s", TEST_INSERT_QUERY));

		// access src/dest through SPARQL endpoints

		String srcSPARQLURL = String.format("http://%s:%d/sparql", srcVirtuosoProcess.getHost(), srcVirtuosoProcess.getHTTPPort());
		srcSPARQLEndpoint = new VirtuosoSPARQLEndpoint(srcSPARQLURL);

		String destSPARQLURL = String.format("http://%s:%d/sparql", destVirtuosoProcess.getHost(), destVirtuosoProcess.getHTTPPort());
		destSPARQLEndpoint = new VirtuosoSPARQLEndpoint(destSPARQLURL);

	}

	public static void teardown() {
		try { srcAdminConn.close(); } catch (Exception e) {}
		if (srcVirtuosoProcess != null)
			srcVirtuosoProcess.shutdown();
		if (destVirtuosoProcess != null)
			destVirtuosoProcess.shutdown();
	}

	protected static int getCountQueryResult(List<Map<String,String>> results) {
		Map<String,String> firstRow = results.get(0);
		String columnName = firstRow.keySet().iterator().next();
		return Integer.valueOf(firstRow.get(columnName));
	}

	protected static class Test {
		public String name;
		public String[] argv;
		public Test(String name, String[] argv) {
			this.name = name;
			this.argv = argv;
		}
	}
}
