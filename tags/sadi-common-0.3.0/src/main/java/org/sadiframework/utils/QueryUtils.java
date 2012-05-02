package org.sadiframework.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sadiframework.utils.RdfUtils;


import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class QueryUtils
{
	public static List<Map<String, String>> convertResultSet(ResultSet resultSet)
	{
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.nextSolution();
			Map<String, String> bindingAsMap = new HashMap<String, String>();
			for (String variable: resultSet.getResultVars()) {
				RDFNode value = solution.get(variable);
				bindingAsMap.put(variable, value != null ? RdfUtils.getPlainString(value.asNode()) : null);
			}
			results.add(bindingAsMap);
		}
		return results;
	}
}
