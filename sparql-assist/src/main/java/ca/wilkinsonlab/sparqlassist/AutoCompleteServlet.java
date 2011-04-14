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
		json = new JSONWriter();
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
				log.debug(String.format("%s: finished processing reuqest", id));
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
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
			msBetweenScans = 900000; // 15 minutes
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
				for (Iterator<AutoCompleter> i = autoCompleters.values().iterator(); i.hasNext(); ) {
					AutoCompleter ac = i.next();
					if (ac.getLastAccess() < System.currentTimeMillis() - msBeforeCleanup) {
						i.remove();
						ac.destroy();
					}
				}
			}
			// servlet is going down, so cleanup leftover AutoCompleters...
			synchronized (autoCompleters) {
				for (Iterator<AutoCompleter> i = autoCompleters.values().iterator(); i.hasNext(); ) {
					AutoCompleter ac = i.next();
					i.remove();
					ac.destroy();
				}
			}
		}
	}
}
