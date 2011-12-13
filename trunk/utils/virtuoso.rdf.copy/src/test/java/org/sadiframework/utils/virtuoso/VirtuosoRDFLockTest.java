package org.sadiframework.utils.virtuoso;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sadiframework.utils.JDBCUtils;
import org.sadiframework.utils.virtuoso.VirtuosoServerUtils.VirtuosoProcess;
import org.sadiframework.vocab.Virtuoso;

import virtuoso.jdbc3.VirtuosoException;
import ca.wilkinsonlab.sadi.client.virtual.sparql.VirtuosoSPARQLEndpoint;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils.HttpStatusException;


public class VirtuosoRDFLockTest {

	protected static final Logger log = Logger.getLogger(VirtuosoRDFLockTest.class);

	protected static Connection adminConn;
	protected static Connection userConn;
	protected static VirtuosoProcess virtuosoProcess;
	protected static VirtuosoSPARQLEndpoint anonSPARQLEndpoint;
	protected static VirtuosoSPARQLEndpoint authSPARQLEndpoint;
	protected static final String TEST_GRAPH = "test";
	protected static final String TEST_USER = "sparqlUser";
	protected static final String TEST_PASSWORD = "sparqlUser";
	protected static final String NOBODY_USER = "nobody";
	protected static final String TEST_UPDATE_QUERY = String.format("INSERT INTO GRAPH <%s> { <a> <b> <c> }", TEST_GRAPH);
	protected static final String TEST_SELECT_QUERY = String.format("SELECT * FROM <%s> WHERE { ?s ?p ?o } LIMIT 1", TEST_GRAPH);

	@BeforeClass
	public static void setupBeforeClass() throws IOException, SQLException {

		virtuosoProcess = VirtuosoServerUtils.startVirtuosoInstance();
		adminConn = VirtuosoUtils.getPersistentJDBCConnection(virtuosoProcess.getHost(), virtuosoProcess.getISQLPort(), "dba", "dba");

		// 'nobody' user indicates default permissions for users of auth SPARQL endpoint

		VirtuosoUtils.setDefaultGraphPerms(adminConn, NOBODY_USER, Virtuoso.GRAPH_PERM_READ_BIT);

		// create an authorized SPARQL user

		VirtuosoUtils.createUser(adminConn, TEST_USER, TEST_PASSWORD);
		VirtuosoUtils.setDefaultGraphPerms(adminConn, TEST_USER, Virtuoso.GRAPH_PERM_READ_BIT);
		VirtuosoUtils.setGraphPerms(adminConn, TEST_GRAPH, TEST_USER, (Virtuoso.GRAPH_PERM_READ_BIT | Virtuoso.GRAPH_PERM_WRITE_BIT));

		userConn = VirtuosoUtils.getPersistentJDBCConnection(virtuosoProcess.getHost(), virtuosoProcess.getISQLPort(), TEST_USER, TEST_PASSWORD);

		// anonymous and authorized SPARQL endpoints

		String anonSPARQLURL = String.format("http://localhost:%d/sparql", virtuosoProcess.getHTTPPort());
		anonSPARQLEndpoint = new VirtuosoSPARQLEndpoint(anonSPARQLURL);

		String authSPARQLURL = String.format("http://localhost:%d/sparql-auth", virtuosoProcess.getHTTPPort());
		authSPARQLEndpoint = new VirtuosoSPARQLEndpoint(authSPARQLURL, TEST_USER, TEST_PASSWORD);

	}

	@AfterClass
	public static void teardownAfterClass() {
		try { adminConn.close(); } catch (Exception e) {}
		try { userConn.close(); } catch (Exception e) {}
		if (virtuosoProcess != null)
			virtuosoProcess.shutdown();
	}

	@Test
	public void testAnonSPARQLLock() throws IOException, SQLException
	{
		// make anonymous SPARQL endpoint world-writable prior to locking
		VirtuosoUtils.grantRole(adminConn, Virtuoso.ANONYMOUS_SPARQL_USER, Virtuoso.SPARQL_UPDATE_ROLE);
		VirtuosoUtils.setDefaultGraphPerms(adminConn, TEST_USER, (Virtuoso.GRAPH_PERM_READ_BIT | Virtuoso.GRAPH_PERM_WRITE_BIT));
		VirtuosoUtils.setDefaultGraphPerms(adminConn, NOBODY_USER, (Virtuoso.GRAPH_PERM_READ_BIT | Virtuoso.GRAPH_PERM_WRITE_BIT));

		VirtuosoRDFLock lock = VirtuosoRDFLock.getLock(adminConn);

		try {

			anonSPARQLEndpoint.updateQuery(TEST_UPDATE_QUERY);
			anonSPARQLEndpoint.updateQuery(TEST_SELECT_QUERY);

			lock.lock();

			anonSPARQLEndpoint.updateQuery(TEST_SELECT_QUERY);

			try {
				anonSPARQLEndpoint.updateQuery(TEST_UPDATE_QUERY);
				fail("lock failed to block anonymous SPARQL update query");
			} catch(HttpStatusException e) {
				// Virtuoso returns HTTP 500 on forbidden update query to anon SPARQL endpoint
				if (e.getStatusCode() != 500)
					throw e;
			}

			lock.unlock();

			anonSPARQLEndpoint.updateQuery(TEST_UPDATE_QUERY);
			anonSPARQLEndpoint.updateQuery(TEST_SELECT_QUERY);

		} finally {
			lock.unlock();
			VirtuosoUtils.revokeRole(adminConn, Virtuoso.ANONYMOUS_SPARQL_USER, Virtuoso.SPARQL_UPDATE_ROLE);
			VirtuosoUtils.setDefaultGraphPerms(adminConn, TEST_USER, Virtuoso.GRAPH_PERM_READ_BIT);
			VirtuosoUtils.setDefaultGraphPerms(adminConn, NOBODY_USER, Virtuoso.GRAPH_PERM_READ_BIT);
		}

	}

	@Test
	public void testAuthSPARQLLock() throws IOException, SQLException
	{

		// SPARQL queries through ISQL are prefixed with "SPARQL" to differentiate from SQL queries
		String isqlUpdateQuery = String.format("SPARQL %s", TEST_UPDATE_QUERY);
		String isqlSelectQuery = String.format("SPARQL %s", TEST_SELECT_QUERY);

		VirtuosoRDFLock lock = VirtuosoRDFLock.getLock(adminConn);

		try {

			authSPARQLEndpoint.updateQuery(TEST_UPDATE_QUERY);
			JDBCUtils.executeUpdateQuery(userConn, isqlUpdateQuery);

			authSPARQLEndpoint.updateQuery(TEST_SELECT_QUERY);
			JDBCUtils.executeUpdateQuery(userConn, isqlSelectQuery);

			lock.lock();

			try {
				authSPARQLEndpoint.updateQuery(TEST_UPDATE_QUERY);
				fail("lock failed to block SPARQL update query for auth SPARQL endpoint");
			} catch(HttpStatusException e) {
				// Virtuoso returns HTTP 500 on forbidden SPARQL update query
				if (e.getStatusCode() != 500)
					throw e;
			}

			try {
				JDBCUtils.executeUpdateQuery(userConn, isqlUpdateQuery);
				fail("lock failed to block SPARQL update query through ISQL");
			} catch(VirtuosoException e) {
				if (!VirtuosoUtils.getErrorCode(e).equals(Virtuoso.SPARUL_INSERT_DENIED_ERROR_CODE))
					throw e;
			}

			authSPARQLEndpoint.updateQuery(TEST_SELECT_QUERY);
			JDBCUtils.executeUpdateQuery(userConn, isqlSelectQuery);

			lock.unlock();

			authSPARQLEndpoint.updateQuery(TEST_UPDATE_QUERY);
			JDBCUtils.executeUpdateQuery(userConn, isqlUpdateQuery);

			authSPARQLEndpoint.updateQuery(TEST_SELECT_QUERY);
			JDBCUtils.executeUpdateQuery(userConn, isqlSelectQuery);

		} finally {
			lock.unlock();
		}

	}

	@Test
	public void testISQLRecoveryFile() throws IOException, SQLException
	{

		// SPARQL queries through ISQL are prefixed with "SPARQL" to differentiate from SQL queries
		String isqlUpdateQuery = String.format("SPARQL %s", TEST_UPDATE_QUERY);
		String isqlSelectQuery = String.format("SPARQL %s", TEST_SELECT_QUERY);

		VirtuosoRDFLock lock = VirtuosoRDFLock.getLock(adminConn);

		try {

			authSPARQLEndpoint.updateQuery(TEST_UPDATE_QUERY);
			JDBCUtils.executeUpdateQuery(userConn, isqlUpdateQuery);

			authSPARQLEndpoint.updateQuery(TEST_SELECT_QUERY);
			JDBCUtils.executeUpdateQuery(userConn, isqlSelectQuery);

			lock.lock();

			try {
				authSPARQLEndpoint.updateQuery(TEST_UPDATE_QUERY);
				fail("lock failed to block SPARQL update query for auth SPARQL endpoint");
			} catch(HttpStatusException e) {
				// Virtuoso returns HTTP 500 on forbidden SPARQL update query
				if (e.getStatusCode() != 500)
					throw e;
			}

			try {
				JDBCUtils.executeUpdateQuery(userConn, isqlUpdateQuery);
				fail("lock failed to block SPARQL update query through ISQL");
			} catch(VirtuosoException e) {
				if (!VirtuosoUtils.getErrorCode(e).equals(Virtuoso.SPARUL_INSERT_DENIED_ERROR_CODE))
					throw e;
			}

			authSPARQLEndpoint.updateQuery(TEST_SELECT_QUERY);
			JDBCUtils.executeUpdateQuery(userConn, isqlSelectQuery);

			lock.unlock();

			authSPARQLEndpoint.updateQuery(TEST_UPDATE_QUERY);
			JDBCUtils.executeUpdateQuery(userConn, isqlUpdateQuery);

			authSPARQLEndpoint.updateQuery(TEST_SELECT_QUERY);
			JDBCUtils.executeUpdateQuery(userConn, isqlSelectQuery);

		} finally {
			lock.unlock();
		}

	}


}
