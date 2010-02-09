package ca.wilkinsonlab.sadi.utils;

import java.util.List;
import java.util.Map;

import ca.wilkinsonlab.sadi.common.SADIException;

/**
 * A generic interface for classes that execute SPARQL queries and return a
 * list of variable bindings.
 * @author Luke McCarthy
 */
public interface QueryExecutor
{
	/**
	 * Execute the specified query and return a list of variable bindings.
	 * Each binding maps variable name to value.
	 * @param query a SPARQL query
	 * @return the variable bindings that satisfy the query
	 */
	public List<Map<String, String>> executeQuery(String query) throws SADIException;
}
