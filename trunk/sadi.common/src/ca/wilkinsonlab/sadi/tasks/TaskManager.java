package ca.wilkinsonlab.sadi.tasks;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class that simplifies spawning, tracking and cleaning up background
 * tasks.
 * 
 * @author Luke McCarthy
 */
public class TaskManager implements UncaughtExceptionHandler
{
	private static final Log log = LogFactory.getLog(TaskManager.class);
	
	private static final TaskManager theInstance = new TaskManager();
	
	private long taskCounter;
	private Map<String, Task> idToTask;
	private Map<Thread, Task> threadToTask;
	private TaskSweeper taskSweeper;
	
	private TaskManager()
	{
		log.debug("New TaskManager instantiated");
		
		taskCounter = 0;
		idToTask = new HashMap<String, Task>();
		threadToTask = new HashMap<Thread, Task>();
		
		taskSweeper = new TaskSweeper();
		taskSweeper.start();
	}
	
	/**
	 * Returns the singleton instance of the task manager.
	 * @return the singleton
	 */
	public static TaskManager getInstance()
	{
		return theInstance;
	}
	
	/**
	 * Start the specified task running.
	 * @param task the task
	 * @return a unique identifier for the task
	 */
	public String startTask(Task task)
	{
		String id = getNextUniqueId();
		task.id = id;
		idToTask.put(id, task);
		
		Thread thread = new Thread(task);
		thread.setUncaughtExceptionHandler(this);
		threadToTask.put(thread, task);
		thread.start();
		
		return task.id;
	}
	
	/**
	 * Returns the latest status message from a task.
	 * @param id the id of the task to poll
	 * @return the latest status message
	 */
	public String pollTask(String id)
	{
		Task task = getTask(id);
		if (task == null) {
			return null;
		} else {
			return task.getStatus();
		}
	}
	
	/**
	 * Returns the task corresponding to an id.
	 * @param id the id of the task
	 * @return the task
	 */
	public Task getTask(String id)
	{
		return idToTask.get(id);
	}
	
	/**
	 * Returns the task corresponding to a thread.
	 * @param thread the thread running the task
	 * @return the task
	 */
	public Task getTask(Thread thread)
	{
		return threadToTask.get(thread);
	}
	
	/**
	 * Clean up the task corresponding to an id.
	 * @param id the id of the task
	 */
	public void disposeTask(String id)
	{
		// avoid exceptions if a task is deleted twice...
		Task task = getTask(id);
		if (task == null)
			return;
		
		idToTask.remove(id);
		
		Thread t = null;
		for (Entry<Thread, Task> entry: threadToTask.entrySet())
			if (entry.getValue().equals(task))
				t = entry.getKey();
		if (t != null)
			threadToTask.remove(t);
		
		/* TODO actively dispose task?
		 */
		System.gc();
	}

	public void uncaughtException(Thread t, Throwable e)
	{
		Task task = threadToTask.get(t);
		if ( task != null ) {
			task.fatalError(e);
			log.error( "RuntimeException in task " + task.toString(), e );
		} else {
			log.error( "RuntimeException in unknown task", e );
		}
	}
	
	private synchronized String getNextUniqueId()
	{
		/* TODO maintain uniqueness through restarts?
		 */
		return String.format("%X", ++taskCounter);
	}
	
	private class TaskSweeper extends Thread
	{
		private static final long SLEEP_INTERVAL = 15 * 60 * 1000;
		private static final long TASK_LIFESPAN = 8 * 60 * 60 * 1000;
		
		boolean done;
		
		public TaskSweeper()
		{
			super(TaskSweeper.class.getCanonicalName());
			setDaemon(true);
			done = false;
		}
		
		public void run()
		{
			while (!done) {
				sweepQueue();
				try {
					Thread.sleep(SLEEP_INTERVAL);
				} catch (InterruptedException e) {
				
				}
			}
		}
		
		private void sweepQueue()
		{
			/* shouldn't have to worry about Thread concurrency because I'm just
			 * cleaning up local references; if anybody else is actually holding a
			 * reference to a Task, they'll still be able to use it...
			 */
			long expiryDate = Calendar.getInstance().getTimeInMillis() - TASK_LIFESPAN;
			Collection<Task> tasksToDispose = new ArrayList<Task>();
			for (Task task: idToTask.values())
				if (task.isFinished() && task.getCompletionTime().getTime() < expiryDate)
					tasksToDispose.add(task);
			for (Task task: tasksToDispose)
				disposeTask(task.id);
		}
	}
}