package ca.wikinsonlab.sadi.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class QueryClient
{
	public List<Map<String, String>> synchronousQuery(String query)
	{
		QueryRunner queryRunner = getQueryRunner(query, null);
		queryRunner.run();
		return queryRunner.results;
	}
	
	/* TODO use java.util.concurrent to return Future objects...
	 */
	public void asynchronousQuery(String query, ClientCallback callback)
	{
		new Thread(getQueryRunner(query, callback)).start();
	}
	
	protected abstract QueryRunner getQueryRunner(String query, ClientCallback callback);
	
	/* TODO provide methods that return RDF; query into a Jena Model? return RDF text?
	 */
	
	public static interface ClientCallback
	{
		public abstract void onFailure(String errorMessage);
		
		public abstract void onSuccess(List<Map<String, String>> results);
	}
	
	protected static abstract class QueryRunner implements Runnable
	{
		protected String query;
		protected ClientCallback callback;
		protected List<Map<String, String>> results;
		
		public QueryRunner(String query, ClientCallback callback)
		{
			this.query = query;
			this.callback = callback;
			
			results = new ArrayList<Map<String, String>>(0);
		}
	}
}
