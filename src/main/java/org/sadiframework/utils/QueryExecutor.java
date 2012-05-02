package org.sadiframework.utils;

import java.util.List;
import java.util.Map;

import org.sadiframework.SADIException;

import com.hp.hpl.jena.rdf.model.Model;


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
	
	/**
	 * Execute the specified CONSTRUCT query and return an RDF model.
	 * @param query a SPARQL CONSTRUCT query
	 * @return the RDF model that satisfy the query
	 */
	public Model executeConstructQuery(String query) throws SADIException;
}
