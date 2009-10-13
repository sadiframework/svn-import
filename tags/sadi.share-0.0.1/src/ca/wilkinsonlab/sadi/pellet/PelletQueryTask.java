package ca.wilkinsonlab.sadi.pellet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.tasks.QueryTask;

public class PelletQueryTask extends QueryTask
{
	@SuppressWarnings("unused")
	private final static Log LOGGER = LogFactory.getLog(PelletQueryTask.class);

	public PelletQueryTask(String queryString)
	{
		super(queryString);
	}
	
	public void run()
	{	
        results = new PelletClient().synchronousQuery(queryString);
        success();
	}
}
