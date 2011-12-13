package org.sadiframework.vocab;


public class Virtuoso {

	public static final String ANONYMOUS_SPARQL_USER = "SPARQL";
	public static final String NOBODY_USER = "nobody";
	public static final String DBA_USER = "dba";

	public static final String SPARQL_UPDATE_ROLE = "SPARQL_UPDATE";
	public static final String SPARQL_SPONGER_ROLE = "SPARQL_SPONGE";

	public static final int GRAPH_PERM_READ_BIT = 1;
	public static final int GRAPH_PERM_WRITE_BIT = 2;
	public static final int GRAPH_PERM_SPONGER_WRITE_BIT = 4;
	public static final int GRAPH_PERM_LIST_GRAPH_GROUP_BIT = 7;
	public static final int GRAPH_PERM_ALL_BITS = (GRAPH_PERM_READ_BIT | GRAPH_PERM_WRITE_BIT | GRAPH_PERM_SPONGER_WRITE_BIT | GRAPH_PERM_LIST_GRAPH_GROUP_BIT);

	public static final String BAD_LOGIN_ERROR_CODE = "CL034";
	public static final String USER_ALREADY_HAS_ROLE_ERROR_CODE = "U0013";
	public static final String USER_DOESNT_HAVE_ROLE_ERROR_CODE = "U0014";
	public static final String SPARUL_INSERT_DENIED_ERROR_CODE = "SR619";

}
