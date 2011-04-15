package ca.wilkinsonlab.sparqlassist;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.stringtree.json.JSONWriter;

import ca.wilkinsonlab.sparqlassist.AutoCompleteRequest.Category;

public class AutoCompleteServlet extends HttpServlet
{
	private static final Logger log = Logger.getLogger(AutoCompleteServlet.class);
	
	private static final long serialVersionUID = 1L;

	private JSONWriter json;
	private Map<String, AutoCompleter> autoCompleters;
	private Thread cleanupThread;
	
	@Override
	public void init() throws ServletException
	{
		super.init();
		json = new JSONWriter(false); // don't add class to JSON map
		autoCompleters = new HashMap<String, AutoCompleter>();
		cleanupThread = new Thread(new CleanupRunnable(), "AutoComplete cleanup thread");
		cleanupThread.start();
	}

	@Override
	public void destroy()
	{
		cleanupThread = null; // CleanupRunnable checks this so it knows when to stop...
		super.destroy();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String id = request.getParameter("id");
		if (id != null) {
			AutoCompleteRequest req = new AutoCompleteRequest();
			req.setQuery(request.getParameter("query"));
			String category = request.getParameter("category");
			if (category == null)
				req.setCategory(Category.DEFAULT);
			else if (category.equalsIgnoreCase("predicates"))
				req.setCategory(Category.PROPERTY);
			else if (category.equalsIgnoreCase("individuals"))
				req.setCategory(Category.INDIVIDUAL);
			else if (category.equalsIgnoreCase("namespaces"))
				req.setCategory(Category.NAMESPACE);
			req.setSPARQL(request.getParameter("sparql"));
			String caret = request.getParameter("caret");
			if (caret != null)
				req.setCaret(caret);
			if (log.isDebugEnabled())
				log.debug(String.format("%s: processing request %s", id, req));
			try {
				String out = json.write(getAutoCompleter(id).processRequest(req));
				if (log.isDebugEnabled())
					log.debug(String.format("%s: sending response %s", id, out));
				response.setContentType("application/json");
				response.getWriter().write(out);
			} catch (Exception e) {
				log.error(String.format("error processing request %s", req), e);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
			if (log.isDebugEnabled())
				log.debug(String.format("%s: finished processing request", id));
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}
	
	/**
	 * Create and return a new AutoCompleter for a fresh SPARQL Assist instance.
	 * @return a new AutoCompleter
	 */
	protected AutoCompleter createAutoCompleter()
	{
		return new AutoCompleter();
	}
	
	private AutoCompleter getAutoCompleter(String id)
	{
		synchronized (autoCompleters) {
			AutoCompleter autoCompleter = autoCompleters.get(id);
			if (autoCompleter == null) {
				log.debug(String.format("creating new AutoCompleter with id %s", id));
				autoCompleter = new AutoCompleter();
				autoCompleters.put(id, autoCompleter);
			}
			return autoCompleter;
		}
	}
	
	private class CleanupRunnable implements Runnable
	{
		int msBetweenScans;
		int msBeforeCleanup;
		
		public CleanupRunnable()
		{
			// TODO make these configurable...
			// maximum time before cleanup = msBeforeCleanup + msBetweenScans
			msBetweenScans = 300000; // 5 minutes 
			msBeforeCleanup = 900000; // 15 minutes
		}
		
		public void run()
		{
			while (cleanupThread != null) {
				try {
					Thread.sleep(msBetweenScans);
				} catch (InterruptedException e) {
					log.warn(String.format("cleanup thread interrupted: %s", e.getMessage()));
				}
				long cutoff = System.currentTimeMillis() - msBeforeCleanup;
				if (log.isTraceEnabled())
					log.trace(String.format("cutoff for cleanup is %d", cutoff));
				for (Iterator<Map.Entry<String, AutoCompleter>> i = autoCompleters.entrySet().iterator(); i.hasNext(); ) {
					Map.Entry<String, AutoCompleter> entry = i.next();
					AutoCompleter ac = entry.getValue();
					if (log.isTraceEnabled())
						log.trace(String.format("AutoCompleter last accessed at %d", ac.getLastAccess()));
					if (ac.getLastAccess() < cutoff) {
						i.remove();
						if (log.isDebugEnabled())
							log.debug(String.format("cleaning up AutoCompleter %s", entry.getKey()));
						ac.destroy();
					}
				}
			}
			// servlet is going down, so cleanup leftover AutoCompleters...
			synchronized (autoCompleters) {
				for (Iterator<Map.Entry<String, AutoCompleter>> i = autoCompleters.entrySet().iterator(); i.hasNext(); ) {
					Map.Entry<String, AutoCompleter> entry = i.next();
					AutoCompleter ac = entry.getValue();
					i.remove();
					if (log.isDebugEnabled())
						log.debug(String.format("cleaning up AutoCompleter %s", entry.getKey()));
					ac.destroy();
				}
			}
		}
	}
}
