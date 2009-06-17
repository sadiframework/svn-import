package ca.wikinsonlab.sadi.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class QueryClient
{
	public final static Log LOGGER = LogFactory.getLog(QueryClient.class);
	
	public List<Map<String, String>> synchronousQuery(String query)
	{
		QueryRunner queryRunner = getQueryRunner(query, null);
		queryRunner.run();
		return queryRunner.results;
	}

	/**
	 * Execute a synchronous query, but abort the query and return
	 * null if the query does not complete within a specified time 
	 * limit.
	 * 
	 * @author Ben Vandervalk
	 * @param query The SPARQL query to be executed
	 * @param timeout The time limit, in milliseconds.
	 * @return query results on success, null on timeout
	 */
	public List<Map<String, String>> synchronousQuery(String query, int timeout)
	{
		boolean queryFinished = true;
		QueryRunner queryRunner = getQueryRunner(query, null);
		try {
			Thread queryThread = new Thread(queryRunner);
			queryThread.start();
			queryThread.join(timeout);
			if(queryThread.isAlive()) {
				queryFinished = false;
				queryThread.stop();
				//queryThread.join();
				LOGGER.trace("Query aborted due to timeout.");
			}
		}
		catch(InterruptedException e) {
		}
		
		if(queryFinished)
			return queryRunner.results;
		else
			return null;
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
