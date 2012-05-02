package org.sadiframework.vocab;

public class TestQueryDB {

//	public static final String DEFAULT_QUERYDB_ENDPOINT = "http://localhost:8890/sparql";

//	public static final String DEFAULT_QUERYDB_GRAPH = "http://querydb/";
	
//	public static final String NS = "http://sadiframework.org/testquerydb.owl#";
	public static final String NS = "http://biordf.net/cardioSHARE/testquerydb.owl#";
	
	public static final String PREDICATE_NUMCONSTANTS = NS + "numConstants";
	/**
	 * The SPARQL query string.
	 */
	public static final String PREDICATE_QUERYSTRING = NS + "queryString";
	/**
	 * The connected subgraph from which the final query was generated, in
	 * the form of a SPARQL query containing no variables.
	 */
	public static final String PREDICATE_SUBGRAPHSTRING = NS + "subgraphString";
	public static final String PREDICATE_QUERYDEPTH = NS + "queryDepth";
	public static final String PREDICATE_QUERYDIAMETER = NS + "queryDiameter";
	public static final String PREDICATE_MAXFANOUT = NS + "maxFanout";
	public static final String PREDICATE_TIMESTAMP = NS + "timestamp";
}
