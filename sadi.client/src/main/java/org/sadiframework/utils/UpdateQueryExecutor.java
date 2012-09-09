package org.sadiframework.utils;

import org.sadiframework.SADIException;

/**
 * A generic interface for classes that execute SPARQL UPDATE queries.
 * @author Luke McCarthy
 */
public interface UpdateQueryExecutor extends QueryExecutor
{
	/**
	 * Execute the specified update query.
	 * @param query a SPARQL UPDATE query
	 * @throws SADIException
	 */
	public void executeUpdateQuery(String query) throws SADIException;
}
