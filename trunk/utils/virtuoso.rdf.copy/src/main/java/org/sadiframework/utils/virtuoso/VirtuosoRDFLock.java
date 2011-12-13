package org.sadiframework.utils.virtuoso;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sadiframework.utils.JDBCUtils;
import org.sadiframework.vocab.Virtuoso;

import virtuoso.jdbc3.VirtuosoException;

public class VirtuosoRDFLock {

	protected static final Logger log = Logger.getLogger(VirtuosoRDFLock.class);

	static Hashtable<String, VirtuosoRDFLock> uniqueLocks = new Hashtable<String, VirtuosoRDFLock>();

	protected Connection conn;
	protected boolean isLocked = false;

	protected Set<String> origSPARQLRoles;
	protected Map<String,Map<String,Integer>> origGraphPerms;

	protected File recoveryFile;

	protected VirtuosoRDFLock(Connection conn) throws SQLException {
		this.conn = conn;
	}

	public synchronized static VirtuosoRDFLock getLock(Connection conn) throws SQLException {

		String url = conn.getMetaData().getURL();
		String host = JDBCUtils.getHost(url);
		int port = JDBCUtils.getPort(url);

		String key = String.format("%s:%d", host, port);

		if (!uniqueLocks.containsKey(key))
			uniqueLocks.put(key, new VirtuosoRDFLock(conn));

		return uniqueLocks.get(key);
	}

	public synchronized void lock() throws SQLException, IOException {

		if (!isLocked) {

			origGraphPerms = VirtuosoUtils.getGraphPerms(conn);

			/*
			 * The user 'SPARQL', which is the user for anonymous SPARQL access via the
			 * http://<virtuoso host>/sparql URL, may or may not have the following
			 * roles:
			 *
			 * => SPARQL_UPDATE (permission to create new data via SPARUL queries)
			 * => SPARQL_SPONGE (permission to create new data indirectly, via Virtuoso "spongers")
			 *
			 * To correctly lock/unlock the triple store, it is necessary to revoke these roles
			 * from the user 'SPARQL', and then restore them on unlock.
			 *
			 * Problem #1:
			 *
			 * I couldn't find a straightforward way to list assigned roles of a user through
			 * isql. However, if a user doesn't have a certain role and you try to revoke it,
			 * a VirtuosoException is thrown. I use this to indirectly determine the originally
			 * assigned roles of the 'SPARQL' user.
			 *
			 * Problem #2:
			 *
			 * VirtuosoException doesn't provide unique error codes via the standard getErrorCode()
			 * method. To work around this, I parse the error message.
			 */

			origSPARQLRoles = new HashSet<String>();
			origSPARQLRoles.add(Virtuoso.SPARQL_UPDATE_ROLE);
			origSPARQLRoles.add(Virtuoso.SPARQL_SPONGER_ROLE);

			try {
				VirtuosoUtils.revokeRole(conn, Virtuoso.ANONYMOUS_SPARQL_USER, Virtuoso.SPARQL_UPDATE_ROLE);
			} catch(VirtuosoException e) {
				if(VirtuosoUtils.getErrorCode(e).equals(Virtuoso.USER_DOESNT_HAVE_ROLE_ERROR_CODE))
					origSPARQLRoles.remove(Virtuoso.SPARQL_UPDATE_ROLE);
				else
					throw e;
			}

			try {
				VirtuosoUtils.revokeRole(conn, Virtuoso.ANONYMOUS_SPARQL_USER, Virtuoso.SPARQL_SPONGER_ROLE);
			} catch(VirtuosoException e) {
				if(VirtuosoUtils.getErrorCode(e).equals(Virtuoso.USER_DOESNT_HAVE_ROLE_ERROR_CODE))
					origSPARQLRoles.remove(Virtuoso.SPARQL_SPONGER_ROLE);
				else
					throw e;
			}

			/* isql commands to restore original user roles/permissions, if rdf copy is aborted */

			setISQLRecoveryFile(createRecoveryISQLFile(origSPARQLRoles, origGraphPerms));

			/* disable write permissions on all graphs for all users, except 'dba' */

			try {

				conn.setAutoCommit(false);

				Map<String,Map<String,Integer>> lockedGraphPerms = new HashMap<String,Map<String,Integer>>();

				for (String graphURI : origGraphPerms.keySet()) {
					if (!lockedGraphPerms.containsKey(graphURI))
						lockedGraphPerms.put(graphURI, new HashMap<String,Integer>());
					for (String user : origGraphPerms.get(graphURI).keySet()) {
						if (user.equals(Virtuoso.DBA_USER))
							continue;
						int origPerms = origGraphPerms.get(graphURI).get(user);
						int lockedPerms = origPerms & ~(Virtuoso.GRAPH_PERM_WRITE_BIT | Virtuoso.GRAPH_PERM_SPONGER_WRITE_BIT);
						lockedGraphPerms.get(graphURI).put(user, lockedPerms);
					}
				}

				VirtuosoUtils.mergeGraphPerms(lockedGraphPerms, conn);

				conn.commit();

			} finally {
				conn.setAutoCommit(true);
			}

			isLocked = true;
		}
	}

	public synchronized void unlock() throws SQLException {

		if (isLocked) {

			if (getISQLRecoveryFile() != null) {
				getISQLRecoveryFile().delete();
				setISQLRecoveryFile(null);
			}

			for (String role : origSPARQLRoles) {
				try {
					VirtuosoUtils.grantRole(conn, Virtuoso.ANONYMOUS_SPARQL_USER, role);
				} catch(VirtuosoException e) {
					// "SPARQL" user will have the role if we user calls unlock() more than once
					if (!VirtuosoUtils.getErrorCode(e).equals(Virtuoso.USER_ALREADY_HAS_ROLE_ERROR_CODE))
						throw e;
				}
			}

			VirtuosoUtils.mergeGraphPerms(origGraphPerms, conn);

			isLocked = false;

		}
	}

	protected File createRecoveryISQLFile(Set<String> anonymousSPARQLRoles,	Map<String,Map<String,Integer>> graphPerms) throws SQLException, IOException {

		Map<String,Map<String,Integer>> origGraphPerms = VirtuosoUtils.getGraphPerms(conn);
		Map<String,Map<String,Integer>> mergedGraphPerms = VirtuosoUtils.mergeGraphPerms(graphPerms, origGraphPerms);

		List<String> stmts = new ArrayList<String>();

		stmts.add("SET AUTOCOMMIT OFF");
		stmts.add("DELETE FROM rdf_graph_user");

		// 1. Set default permissions for all graphs

		if (mergedGraphPerms.get(null) != null) {

			// 1a. nobody

			if (graphPerms.get(null).get(Virtuoso.NOBODY_USER) != null)
				stmts.add(VirtuosoUtils.getDefaultGraphPermsUpdateQueryString(Virtuoso.NOBODY_USER, mergedGraphPerms.get(null).get(Virtuoso.NOBODY_USER)));

			// 1b. ordinary users

			for (String user : mergedGraphPerms.get(null).keySet()) {
				if (user.equals(Virtuoso.NOBODY_USER))
					continue;
				stmts.add(VirtuosoUtils.getDefaultGraphPermsUpdateQueryString(user, mergedGraphPerms.get(null).get(user)));
			}

		}

		// 2. Set graph-specific permissions

		for (String graphURI : mergedGraphPerms.keySet()) {

			if (graphURI == null)
				continue;

			// 2a. nobody

			if (mergedGraphPerms.get(graphURI).get(Virtuoso.NOBODY_USER) != null)
				stmts.add(VirtuosoUtils.getGraphPermsUpdateQueryString(graphURI, Virtuoso.NOBODY_USER, mergedGraphPerms.get(graphURI).get(Virtuoso.NOBODY_USER)));

			// 2b. ordinary users

			for (String user : mergedGraphPerms.get(graphURI).keySet()) {
				if (user.equals(Virtuoso.NOBODY_USER))
					continue;
				stmts.add(VirtuosoUtils.getGraphPermsUpdateQueryString(graphURI, user, mergedGraphPerms.get(graphURI).get(user)));
			}

		}

		stmts.add("COMMIT WORK");
		stmts.add("SET AUTOCOMMIT ON");

		/* write statements to file */

		String url = conn.getMetaData().getURL();
		String host = JDBCUtils.getHost(url);
		int port = JDBCUtils.getPort(url);

		File recoveryFile = new File(String.format("unlock.%s.%d.isql", host, port));
		Writer writer = new BufferedWriter(new FileWriter(recoveryFile));

		for (String stmt : stmts) {
			writer.write(stmt);
			writer.write(";\n");
		}

		writer.close();

		log.info(String.format(
				"created recovery isql file %s. If you abort the copy, you can manually " +
				"unlock the src Virtuoso instance with 'isql %s:%d dba <password> %s'",
				recoveryFile,
				host,
				port,
				recoveryFile));

		return recoveryFile;
	}

	public void setISQLRecoveryFile(File recoveryFile) {
		this.recoveryFile = recoveryFile;
	}

	public File getISQLRecoveryFile() {
		return recoveryFile;
	}

}
