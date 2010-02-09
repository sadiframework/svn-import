package ca.wilkinsonlab.sadi.virtuoso;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.wilkinsonlab.sadi.utils.http.HttpUtils;

/**
 * A class encapsulating basic functionality for accessing a registry in a
 * Virtuoso triple store through a SPARQL endpoint.
 * @author Luke McCarthy
 */
public class VirtuosoSPARQLEndpoint
{
	protected URL sparqlEndpoint;
	protected String graphName;
	
	public VirtuosoSPARQLEndpoint(URL sparqlEndpoint)
	{
		this(sparqlEndpoint, null);
	}
	
	public VirtuosoSPARQLEndpoint(URL sparqlEndpoint, String graphName)
	{
		this.sparqlEndpoint = sparqlEndpoint;
		this.graphName = graphName;
	}
	
	/**
	 * Posts the specified query to the SPARQL endpoint and returns the
	 * resulting variable bindings.
	 * @param query the SPARQL query
	 * @return a list of variable bindings that satisfy the query
	 * @throws IOException 
	 */
	public List<Map<String, String>> executeQuery(String query)
	throws IOException
	{
		Object result = HttpUtils.postAndFetchJson(sparqlEndpoint, getPostParameters(query));
		return convertResults(result);
	}

	protected Map<String, String> getPostParameters(String query)
	{
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("query", query);
		parameters.put("format", "JSON");
		if (graphName != null)
			parameters.put("default-graph-uri", graphName);
		return parameters;
	}
	
	@SuppressWarnings("unchecked")
	protected static List<Map<String, String>> convertResults(Object result)
	{
		List<Map<String, Map>> virtuosoBindings = (List)((Map)((Map)result).get("results")).get("bindings");
		List<Map<String, String>> localBindings = new ArrayList<Map<String, String>>(virtuosoBindings.size());
		for (Map<String, Map> virtuosoBinding: virtuosoBindings) {
			Map<String, String> ourBinding = new HashMap<String, String>();
			for (String variable: virtuosoBinding.keySet()) {
				ourBinding.put(variable, (String)virtuosoBinding.get(variable).get("value"));
			}
			localBindings.add(ourBinding);
		}
		return localBindings;
	}
}
