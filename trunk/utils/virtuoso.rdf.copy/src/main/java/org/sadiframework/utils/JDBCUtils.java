package org.sadiframework.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class JDBCUtils {

	protected static final Logger log = Logger.getLogger(JDBCUtils.class);

	public static List<List<String>> executeQuery(Connection conn, String sql) throws SQLException {
		return executeAndCloseQuery(conn.prepareStatement(sql));
	}

	public static List<List<String>> executeAndCloseQuery(PreparedStatement stmt) throws SQLException {

		List<List<String>> resultRows = new ArrayList<List<String>>();

		ResultSet rset = null;

		try {
			rset = stmt.executeQuery();
			int numCols = rset.getMetaData().getColumnCount();
			while (rset.next()) {
				List<String> resultRow = new ArrayList<String>();
				for (int i = 1; i <= numCols; i++)
					resultRow.add(rset.getString(i));
				resultRows.add(resultRow);
			}

			return resultRows;

		} finally {
			try { if (rset != null) rset.close(); } catch(Exception e) { }
			try { if (stmt != null) stmt.close(); } catch(Exception e) { }
		}

	}

	public static int executeUpdateQuery(Connection conn, String sql) throws SQLException {
		return executeAndCloseUpdateQuery(conn.prepareStatement(sql));
	}

	public static int executeAndCloseUpdateQuery(PreparedStatement stmt) throws SQLException {
		try {
			return stmt.executeUpdate();
		} finally {
			try { if (stmt != null) stmt.close(); } catch(Exception e) { }
		}
	}

	public static String getHost(String jdbcURL) {
		try {
			URI uri = new URI(jdbcURL.replaceFirst("jdbc:",""));
			return uri.getHost();
		} catch(URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static int getPort(String jdbcURL) {
		try {
			URI uri = new URI(jdbcURL.replaceFirst("jdbc:",""));
			return uri.getPort();
		} catch(URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
