package org.sadiframework.utils.virtuoso;

import java.beans.PropertyVetoException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sadiframework.utils.ConsoleUtils;
import org.sadiframework.utils.JDBCUtils;
import org.sadiframework.vocab.Virtuoso;

import virtuoso.jdbc3.VirtuosoException;
import virtuoso.jena.driver.VirtModel;

import com.hp.hpl.jena.rdf.model.Model;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class VirtuosoUtils {

	protected static final Logger log = Logger.getLogger(VirtuosoUtils.class);

	public static Map<String,Map<String,Integer>> getGraphPerms(Connection conn, String graphURI, boolean includeDefaultGraphPerms) throws SQLException {

		Map<String,Map<String,Integer>> allGraphPerms = getGraphPerms(conn);
		Map<String,Map<String,Integer>> graphPerms = new HashMap<String,Map<String,Integer>>();

		Set<String> graphUsers = new HashSet<String>();

		if (allGraphPerms.containsKey(graphURI)) {
			graphPerms.put(graphURI, new HashMap<String,Integer>());
			for (String user : allGraphPerms.get(graphURI).keySet()) {
				graphUsers.add(user);
				int perms = allGraphPerms.get(graphURI).get(user);
				graphPerms.get(graphURI).put(user, perms);
			}
		}

		if (includeDefaultGraphPerms && allGraphPerms.containsKey(null)) {
			graphPerms.put(null, new HashMap<String,Integer>());
			for (String user : allGraphPerms.get(null).keySet()) {
				if (user.equals(Virtuoso.NOBODY_USER) || graphUsers.contains(user)) {
					int perms = allGraphPerms.get(null).get(user);
					graphPerms.get(null).put(user, perms);
				}
			}
		}

		return graphPerms;

	}

	public static Map<String,Map<String,Integer>> getGraphPerms(Connection conn) throws SQLException {

		/*
		 * graph URI => (username => permissions number)
		 * permissions number: 1 = read, 2 = write, 4 = sponger write, 7 = list members of graph group
		 */

		Map<String, Map<String,Integer>> graphPerms = new HashMap<String, Map<String,Integer>>();

		StringBuilder query = new StringBuilder()
			.append("SELECT id_to_iri(rgu_graph_iid), u_name, rgu_permissions\n")
			.append("FROM rdf_graph_user,sys_users\n")
			.append("WHERE (rgu_user_id = u_id)\n");

		for(List<String> resultRow : JDBCUtils.executeQuery(conn, query.toString())){

			String graph = resultRow.get(0);
			String user = resultRow.get(1);
			Integer perms = Integer.valueOf(resultRow.get(2));

			if (!graphPerms.containsKey(graph))
				graphPerms.put(graph, new HashMap<String,Integer>());

			Map<String,Integer> userPerms = graphPerms.get(graph);
			userPerms.put(user, perms);

		}

		return graphPerms;
	}

	public static void mergeGraphPerms(Map<String,Map<String,Integer>> graphPerms, Connection conn) throws SQLException {

		Map<String,Map<String,Integer>> origGraphPerms = getGraphPerms(conn);
		Map<String,Map<String,Integer>> mergedGraphPerms = mergeGraphPerms(graphPerms, origGraphPerms);

		/*
		 * The copying of graph permissions may require the creation of Virtuoso
		 * users that don't yet exist at the destination. As I could not figure out a way
		 * to copy a user account automatically from one Virtuoso instance to
		 * another, the users must be created interactively (i.e. with a password prompt).
		 */

		for (String graphURI : graphPerms.keySet()) {
			for (String user : graphPerms.get(graphURI).keySet()) {
				if (!userExists(conn, user))
					createUserInteractive(conn, user);
			}
		}

		try {

			conn.setAutoCommit(false);

			JDBCUtils.executeUpdateQuery(conn, "DELETE FROM rdf_graph_user");

			// 1. Set default permissions for all graphs

			if (mergedGraphPerms.get(null) != null) {

				// 1a. nobody

				if (mergedGraphPerms.get(null).get(Virtuoso.NOBODY_USER) != null)
					setDefaultGraphPerms(conn, Virtuoso.NOBODY_USER, mergedGraphPerms.get(null).get(Virtuoso.NOBODY_USER));

				// 1b. ordinary users

				for (String user : mergedGraphPerms.get(null).keySet()) {
					if (user.equals(Virtuoso.NOBODY_USER))
						continue;
					setDefaultGraphPerms(conn, user, mergedGraphPerms.get(null).get(user));
				}
			}

			// 2. Set graph-specific permissions

			for (String graphURI : mergedGraphPerms.keySet()) {

				if (graphURI == null)
					continue;

				// 2a. nobody

				if (mergedGraphPerms.get(graphURI).get(Virtuoso.NOBODY_USER) != null)
					setGraphPerms(conn, graphURI, Virtuoso.NOBODY_USER, mergedGraphPerms.get(graphURI).get(Virtuoso.NOBODY_USER));

				// 2b. ordinary users

				for (String user : mergedGraphPerms.get(graphURI).keySet()) {
					if (user.equals(Virtuoso.NOBODY_USER))
						continue;
					setGraphPerms(conn, graphURI, user, mergedGraphPerms.get(graphURI).get(user));
				}
			}

			conn.commit();

		} finally {
			conn.setAutoCommit(true);
		}
	}

	protected static Map<String,Map<String,Integer>> mergeGraphPerms(Map<String,Map<String,Integer>> srcGraphPerms, Map<String,Map<String,Integer>> destGraphPerms) {

		Map<String,Map<String,Integer>> mergedGraphPerms = new HashMap<String,Map<String,Integer>>();

		/* Init mergedGraphPerms to a deep copy of destGraphPerms */

		for (String graphURI : destGraphPerms.keySet()) {
			if (!mergedGraphPerms.containsKey(graphURI))
				mergedGraphPerms.put(graphURI, new HashMap<String,Integer>());
			for (String user : destGraphPerms.get(graphURI).keySet()) {
				int perms = destGraphPerms.get(graphURI).get(user);
				mergedGraphPerms.get(graphURI).put(user, perms);
			}
		}

		/*
		 * Copy srcGraphPerms into destGraphPerms.
		 *
		 * Where both src and dest specify perms for
		 * the same user/graph combination, the original
		 * perms for dest will be overwritten.  (This
		 * includes permissions involving the user 'nobody'
		 * and/or the default graph (null)).
		 */

		for (String graphURI : srcGraphPerms.keySet()) {
			if (!mergedGraphPerms.containsKey(graphURI))
				mergedGraphPerms.put(graphURI, new HashMap<String,Integer>());
			for (String user : srcGraphPerms.get(graphURI).keySet()) {
				int perms = srcGraphPerms.get(graphURI).get(user);
				/*
				if (graphURI == null || user.equals(Virtuoso.NOBODY_USER)) {
					if (mergedGraphPerms.containsKey(graphURI) && mergedGraphPerms.get(graphURI).containsKey(user)) {
						int destPerms = mergedGraphPerms.get(graphURI).get(user);
						perms &= destPerms;
					}
				}
				*/
				mergedGraphPerms.get(graphURI).put(user, perms);
			}
		}

		/*
		 * Demote default graph permission for users where necessary,
		 * to ensure that they are never broader than permissions for
		 * a specific graph.
  		 */

		Map<String,Integer> defaultUserPermsMasks = new HashMap<String,Integer>();

		for (String graphURI : mergedGraphPerms.keySet()) {
			for (String user : mergedGraphPerms.get(graphURI).keySet()) {
				if (user.equals(Virtuoso.NOBODY_USER) || user.equals(Virtuoso.DBA_USER))
					continue;
				int perms = mergedGraphPerms.get(graphURI).get(user);
				if (!defaultUserPermsMasks.containsKey(user))
					defaultUserPermsMasks.put(user, 0);
				int defaultUserPermsMask = defaultUserPermsMasks.get(user);
				defaultUserPermsMasks.put(user, defaultUserPermsMask |= ~perms);
			}
		}

		for (String user : defaultUserPermsMasks.keySet()) {
			int defaultUserPerms;
			if (mergedGraphPerms.containsKey(null) && mergedGraphPerms.get(null).containsKey(user)) {
				defaultUserPerms = mergedGraphPerms.get(null).get(user);
				defaultUserPerms &= ~(defaultUserPermsMasks.get(user));
				mergedGraphPerms.get(null).put(user, defaultUserPerms);
			}
		}

		/*
		 * For each graph, demote permissions for 'nobody' user to ensure that
		 * they are never broader than the permissions of a regular user.
		 */

		int nobodyDefaultGraphPermsMask = 0;

		for (String graphURI : mergedGraphPerms.keySet()) {

			int nobodyGraphPermsMask = 0;

			for (String user : mergedGraphPerms.get(graphURI).keySet()) {
				if (user.equals(Virtuoso.NOBODY_USER))
					continue;
				int perms = mergedGraphPerms.get(graphURI).get(user);
				nobodyGraphPermsMask |= ~perms;
				nobodyDefaultGraphPermsMask |= ~perms;
			}

			if (mergedGraphPerms.get(graphURI).containsKey(Virtuoso.NOBODY_USER)) {
				int nobodyPerms = mergedGraphPerms.get(graphURI).get(Virtuoso.NOBODY_USER);
				nobodyPerms &= ~nobodyGraphPermsMask;
				mergedGraphPerms.get(graphURI).put(Virtuoso.NOBODY_USER, nobodyPerms);
				nobodyDefaultGraphPermsMask |= ~nobodyPerms;
			}

		}

		/*
		 * Make sure that the default graph perms for 'nobody' are narrower
		 * than the perms for 'nobody' on any specific graph.
		 */

		int nobodyDefaultGraphPerms;

		if (srcGraphPerms.containsKey(null) && srcGraphPerms.get(null).containsKey(Virtuoso.NOBODY_USER))
			nobodyDefaultGraphPerms = srcGraphPerms.get(null).get(Virtuoso.NOBODY_USER);
		else
			nobodyDefaultGraphPerms = Virtuoso.GRAPH_PERM_ALL_BITS;

		nobodyDefaultGraphPerms &= ~nobodyDefaultGraphPermsMask;

		if ((mergedGraphPerms.containsKey(null) && mergedGraphPerms.get(null).containsKey(Virtuoso.NOBODY_USER)) ||
			nobodyDefaultGraphPerms != Virtuoso.GRAPH_PERM_ALL_BITS) {

			if (!mergedGraphPerms.containsKey(null))
				mergedGraphPerms.put(null, new HashMap<String,Integer>());
			mergedGraphPerms.get(null).put(Virtuoso.NOBODY_USER, nobodyDefaultGraphPerms);

		}

		return mergedGraphPerms;
	}

	protected static void setDefaultGraphPerms(Connection conn, String user, int permissions) throws SQLException  {
		JDBCUtils.executeAndCloseUpdateQuery(getGraphPermsUpdateQuery(conn, null, user, permissions));
	}

	protected static void setGraphPerms(Connection conn, String graphURI, String user, int permissions) throws SQLException {
		JDBCUtils.executeAndCloseUpdateQuery(getGraphPermsUpdateQuery(conn, graphURI, user, permissions));
	}


	public static PreparedStatement getGraphPermsUpdateQuery(Connection conn, String graphURI, String user, int permissions) throws SQLException {
		PreparedStatement stmt;
		if (graphURI == null) {
			/* prepared statements don't work -- error SQ083: Can't generate scalar exp 105
			stmt = conn.prepareStatement("RDF_DEFAULT_USER_PERMS_SET(??, ??)");
			stmt.setString(1, user);
			stmt.setInt(2, permissions);
			*/
			stmt = conn.prepareStatement(getDefaultGraphPermsUpdateQueryString(user, permissions));
		}
		else {
			/* prepared statements don't work -- error SQ083: Can't generate scalar exp 105
			stmt = conn.prepareStatement("RDF_GRAPH_USER_PERMS_SET(??, ??, ??)");
			stmt.setString(1, graphURI);
			stmt.setString(2, user);
			stmt.setInt(3, permissions);
			*/
			stmt = conn.prepareStatement(getGraphPermsUpdateQueryString(graphURI, user, permissions));
		}
		return stmt;
	}

	public static String getDefaultGraphPermsUpdateQueryString(String user, int permissions) {
		return String.format("RDF_DEFAULT_USER_PERMS_SET('%s', %d)", user, permissions);
	}

	public static String getGraphPermsUpdateQueryString(String graphURI, String user, int permissions) {
		return String.format("RDF_GRAPH_USER_PERMS_SET('%s', '%s', %d)", graphURI, user, permissions);
	}

	public static void grantRole(Connection conn, String user, String role) throws SQLException {
		JDBCUtils.executeAndCloseUpdateQuery(getGrantRoleQuery(conn, user, role));
	}

	public static PreparedStatement getGrantRoleQuery(Connection conn, String user, String role) throws SQLException {
		/* prepared statements don't work -- error SQ083: Can't generate scalar exp 105
		PreparedStatement stmt = conn.prepareStatement("USER_GRANT_ROLE(??, ??)");
		stmt.setString(1, user);
		stmt.setString(2, role);
		*/
		PreparedStatement stmt = conn.prepareStatement(getGrantRoleQueryString(user, role));
		return stmt;
	}

	public static String getGrantRoleQueryString(String user, String role) {
		return String.format("USER_GRANT_ROLE('%s', '%s')", user, role);
	}

	public static void revokeRole(Connection conn, String user, String role) throws SQLException {
		JDBCUtils.executeAndCloseUpdateQuery(getRevokeRoleQuery(conn, user, role));
	}

	public static PreparedStatement getRevokeRoleQuery(Connection conn, String user, String role) throws SQLException {
		/* prepared statements don't work -- error SQ083: Can't generate scalar exp 105
		PreparedStatement stmt = conn.prepareStatement("USER_REVOKE_ROLE(??, ??)");
		stmt.setString(1, user);
		stmt.setString(2, role);
		*/
 		PreparedStatement stmt = conn.prepareStatement(String.format("USER_REVOKE_ROLE('%s', '%s')", user, role));
		return stmt;
	}

	public static Connection getPersistentJDBCConnection(String host, int port, String username, String password) throws SQLException {

		ComboPooledDataSource dataSource = new ComboPooledDataSource();

		try {
			dataSource.setDriverClass("virtuoso.jdbc3.Driver");
			dataSource.setJdbcUrl(getJDBCURL(host, port));
			dataSource.setUser(username);
			dataSource.setPassword(password);
		} catch(PropertyVetoException e) {
			throw new RuntimeException(e);
		}

		return dataSource.getConnection();

	}

	public static void checkpoint(Connection conn) throws SQLException {
		JDBCUtils.executeUpdateQuery(conn, "CHECKPOINT");
	}

	public static Collection<String> getGraphURIs(Connection conn) throws SQLException {
		/*
		 * The following query works correctly with Virtuoso 5, but in Virtuoso 6 there is a
		 * bug where it omits recently created graphs from the results.
		 */
		//String query = "SPARQL SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o } }";
		String query = "SELECT DISTINCT id_to_iri(G) FROM rdf_quad";
		List<List<String>> results = JDBCUtils.executeQuery(conn, query);
		Collection<String> graphURIs = new ArrayList<String>();
		for (List<String> row : results)
			graphURIs.add(row.get(0));
		return graphURIs;
	}

	public static String getJDBCURL(String host, int port) {
		return String.format("jdbc:virtuoso://%s:%d", host, port);
	}

	public static void copyGraph(
			String graphURI,
			String srcHost,
			int srcPort,
			String srcUser,
			String srcPassword,
			String destHost,
			int destPort,
			String destUser,
			String destPassword,
			File backupDir,
			boolean overwriteGraph)

	throws SQLException, IOException
	{

		String srcURL = VirtuosoUtils.getJDBCURL(srcHost, srcPort);
		String destURL = VirtuosoUtils.getJDBCURL(destHost, destPort);

		Model srcModel = VirtModel.openDatabaseModel(graphURI, srcURL, srcUser, srcPassword);
		Model destModel = VirtModel.openDatabaseModel(graphURI, destURL, destUser, destPassword);

		if (destModel.size() > 0)
			backupGraph(destHost, destPort, destUser, destPassword, graphURI, backupDir);

		if (overwriteGraph) {
			log.info(String.format("clearing graph <%s> from dest Virtuoso (%s:%d)", graphURI, destHost, destPort));
			destModel.removeAll();
		}

		log.info(String.format("copying graph <%s> from src Virtuoso (%s:%d) to dest Virtuoso (%s:%d)", graphURI, srcHost, srcPort, destHost, destPort));
		destModel.add(srcModel);

		/* try this in case of performance issues
		Model chunkModel = ModelFactory.createDefaultModel();

		StmtIterator i = srcModel.listStatements();
		long totalSize = 0;
		while (i.hasNext()) {
			int size = 0;
			while (i.hasNext() && size < chunkSize) {
				chunkModel.add(i.next());
				size++;
				totalSize++;
			}
			destModel.add(chunkModel);
			chunkModel.removeAll();
			log.info(String.format("copied %d triples", totalSize));
		}

		i.close();
		chunkModel.close();
		*/

		srcModel.close();
		destModel.close();
	}

	protected static void backupGraph(String host, int port, String user, String password, String graphURI, File backupDir) throws IOException {

		String basename = null;
		try {
			basename = URLEncoder.encode(graphURI, "UTF-8");
		} catch(UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		File backupFile = new File(backupDir, String.format("%s.ttl", basename));
		OutputStream os = new BufferedOutputStream(new FileOutputStream(backupFile));

		log.info(String.format("backing up graph <%s> from %s:%d to %s", graphURI, host, port, backupFile.toString()));

		writeTTL(host, port, user, password, graphURI, os);
		os.close();

	}

	protected static void createUserInteractive(Connection conn, String username) throws SQLException {

		String jdbcURL = conn.getMetaData().getURL();
		String host = JDBCUtils.getHost(jdbcURL);
		int port = JDBCUtils.getPort(jdbcURL);

		log.info(String.format("Creating Virtuoso user '%s' at %s:%d", username, host, port));
		String prompt = String.format("Enter desired password for '%s': ", username);
		String password = ConsoleUtils.getPassword(prompt, true);

		createUser(conn, username, password);
	}

	public static void createUser(Connection conn, String username, String password) throws SQLException {

		/* prepared statements don't work -- error SQ083: Can't generate scalar exp 105
		PreparedStatement query = conn.prepareStatement("USER_CREATE(??, ??)");
		query.setString(1, username);
		query.setString(2, password);
		JDBCUtils.executeAndCloseUpdateQuery(query);
		*/
		String query = String.format("USER_CREATE('%s','%s')", username, password);
		JDBCUtils.executeUpdateQuery(conn, query);

		/*
		 * All graph users should have the SPARQL_UPDATE role.  We catch
		 * the exception here so that a caller can invoke createUser
		 * with the same username more than once.
		 */
		try {
			grantRole(conn, username, Virtuoso.SPARQL_UPDATE_ROLE);
		} catch(VirtuosoException e) {
			if (!getErrorCode(e).equals(Virtuoso.USER_ALREADY_HAS_ROLE_ERROR_CODE))
				throw e;
		}
	}

	public static boolean userExists(Connection conn, String username) throws SQLException {
		/* prepared statements don't work -- error SQ083: Can't generate scalar exp 105
		PreparedStatement query = conn.prepareStatement("SELECT * FROM sys_users WHERE u_name = ??");
		query.setString(1, username);
		return (JDBCUtils.executeAndCloseQuery(query).size() > 0);
		*/
		String query = String.format("SELECT * FROM sys_users WHERE u_name = '%s'", username);
		return (JDBCUtils.executeQuery(conn, query).size() > 0);
	}

	public static String getErrorCode(VirtuosoException e) {
		if (!e.getMessage().contains(":"))
			return null;
		return e.getMessage().split(":")[0];
	}

	public static void writeTTL(
			String host,
			int port,
			String username,
			String password,
			String graphURI,
			OutputStream os
	) throws IOException
	{

		/*
		* Note: Jena has several bugs with respect to loading/writing data with relative URIs.
		* As such, the only safe RDF format to use here here is "TURTLE" or "N-TRIPLES".
		*
		* Bug #1: When reading an RDF/XML into a model, it doesn't remove the '#' chars
		* from the beginning of relative URIs.
		*
		* Bug #2: Jena can write a Model containing relative URIs to a TURTLE file, but
		* throws an exception on attempting to read a TURTLE file containing relative URIs.
		*
		* Bug #3: Even with the "allowBadURIs" option, the RDF/XML writer will throw an
		* exception if the Model contains relative URIs in the predicate position.
		*/

		String url = VirtuosoUtils.getJDBCURL(host, port);
		Model virtModel = VirtModel.openDatabaseModel(graphURI, url, username, password);
		virtModel.write(os, "TURTLE", "");
		virtModel.close();

	}

}
