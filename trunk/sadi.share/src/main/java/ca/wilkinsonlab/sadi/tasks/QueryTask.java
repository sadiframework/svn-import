package ca.wilkinsonlab.sadi.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class QueryTask extends Task
{
	protected String queryString;
	protected List<Map<String, String>> results;
	
	public QueryTask(String queryString)
	{
		this.queryString = queryString;
		results = new ArrayList<Map<String, String>>();
	}
	
	public String getQueryString()
	{
		return queryString;
	}
	
	public List<Map<String, String>> getResults()
	{
		return results;
	}
	
	public String toString()
	{
		StringBuilder buf = new StringBuilder();
		buf.append("[id:");
		buf.append(id);
		buf.append(", query:\"");
		buf.append(queryString);
		buf.append("\"]");
		return buf.toString();
	}
}
