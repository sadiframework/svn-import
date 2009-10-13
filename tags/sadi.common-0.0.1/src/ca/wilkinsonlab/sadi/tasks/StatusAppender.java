package ca.wilkinsonlab.sadi.tasks;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class StatusAppender extends AppenderSkeleton
{
	private final static TaskManager taskManager = TaskManager.getInstance();
	
	@Override
	protected void append(LoggingEvent event)
	{
		Task currentTask = taskManager.getTask(Thread.currentThread());
		if (currentTask != null && event.getMessage() != null) 
			currentTask.setStatus(event.getMessage().toString());
	}

	@Override
	public void close()
	{
		return;
	}

	@Override
	public boolean requiresLayout()
	{
		return false;
	}
}
