package ca.wilkinsonlab.sadi.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public abstract class QueryClient
{
	protected final static Logger LOGGER = Logger.getLogger(QueryClient.class);
	
	public List<Map<String, String>> synchronousQuery(String query)
	{
		QueryRunner queryRunner = getQueryRunner(query, null);
		queryRunner.run();
		return queryRunner.results;
	}

	/* TODO use java.util.concurrent to return Future objects...
	 */
	public void asynchronousQuery(String query, QueryClientCallback callback)
	{
		new Thread(getQueryRunner(query, callback)).start();
	}
	
	protected abstract QueryRunner getQueryRunner(String query, QueryClientCallback callback);
	
	/* TODO provide methods that return RDF; query into a Jena Model? return RDF text?
	 */
	
	public static interface QueryClientCallback
	{
		public abstract void onFailure(String errorMessage);
		
		public abstract void onSuccess(List<Map<String, String>> results);
	}
	
	protected static abstract class QueryRunner implements Runnable
	{
		protected String query;
		protected QueryClientCallback callback;
		protected List<Map<String, String>> results;
		
		public QueryRunner(String query, QueryClientCallback callback)
		{
			this.query = query;
			this.callback = callback;
			
			results = new ArrayList<Map<String, String>>(0);
		}
	}
}
