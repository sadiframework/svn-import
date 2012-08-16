package org.sadiframework.service.proxy;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

import org.sadiframework.service.ServiceServlet;
import org.sadiframework.service.annotations.URI;
import org.sadiframework.tasks.TaskManager;
import org.sadiframework.utils.ContentType;
import org.sadiframework.utils.JsonUtils;
import org.sadiframework.utils.QueryableErrorHandler;
import org.sadiframework.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;

@URI("http://sadiframework.org/services/proxy/get-proxy")
public class GETProxyServlet extends ServiceServlet
{
	private static final Logger log = Logger.getLogger(GETProxyServlet.class);
	private static final long serialVersionUID = 1L;

	/* TODO this is probably unsafe; if we add anything to ServiceServlet that
	 * doesn't get initialized here, we've got runtime problems...
	 */
	@Override
	public void init() throws ServletException
	{
		/* getServiceURL() will usually return null; a service URL only needs
		 * to be set explicitly in baroque network configurations where the
		 * request URL that the service servlet receives is not the external
		 * service URL (and even then, only with asynchronous services that
		 * generate polling URLs from the request URL...)
		 * In these cases, the configured service URL will often not apply 
		 * during service testing (which will usually take place on localhost).
		 * To facilitate testing, the configured service URL will be ignored
		 * if the property "sadi.service.ignoreForcedURL" is set on the JVM.
		 */
		if (System.getProperty(IGNORE_FORCED_URL_SYSTEM_PROPERTY) != null) {
			log.info("ignoring specified service URL");
			ignoreForcedURL = true;
		} else {
			ignoreForcedURL = false;
		}

		errorHandler = new QueryableErrorHandler();
		modelMaker = createModelMaker();
	}
	
	@Override
	public void doGet (HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		if (log.isTraceEnabled())
			log.trace(String.format("request: %s", request));
		
		/* if the request specifies a task ID, retrieve the task;
		 * if not, create a new one...
		 */
		ServiceProxyTask task;
		String taskID = request.getParameter("taskID");
		if (taskID == null) {
			String serviceURL = getProxiedServiceURL(request);
			if (serviceURL == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no service specified");
				return;
			}
			String callback = request.getParameter("callback");
			task = new ServiceProxyTask(serviceURL, assembleInput(request), callback);
			taskID = TaskManager.getInstance().startTask(task);
			if (log.isTraceEnabled())
				log.trace(String.format("started new task with taskID %s", taskID));
		} else {
			task = (ServiceProxyTask)TaskManager.getInstance().getTask(taskID);
			if (task == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, String.format("task %s", taskID));
				return;
			}
		}
		
		/* at this point we have the ID of a running task and a handle on
		 * the task itself;
		 */
		if (finishesBeforeHTTPTimeout(task)) {
			if (log.isDebugEnabled())
				log.debug(String.format("task %s: sending output", taskID));
			String callback = task.getCallback();
			if (callback != null) {
				response.setContentType("application/javascript");
				StringWriter buf = new StringWriter();
				ContentType.RDF_XML.writeModel(task.getModel(), buf, "");
				response.getWriter().format("%s(%s);", callback, 
						JsonUtils.write(buf.toString()));
			} else {
				response.setContentType(super.getContentType(request).getHTTPHeader());
				outputSuccessResponse(response, task.getModel());
			}
			TaskManager.getInstance().disposeTask(taskID);
		} else {
			String redirectURL = getTaskURL(request, taskID);
			if (log.isDebugEnabled())
				log.debug(String.format("task %s: sending redirect to %s", taskID, redirectURL));
			response.sendRedirect(redirectURL);
		}
	}

	protected String getProxiedServiceURL(HttpServletRequest request)
	{
		return request.getParameter("serviceURL");
	}

	protected Model assembleInput(HttpServletRequest request) throws IOException
	{
		Model inputModel = super.createInputModel();
		ContentType contentType = ContentType.getContentType(StringUtils.defaultString(
				request.getParameter("format"), "application/rdf+xml"));
		for (String input: getParameterValues(request, "input")) {
			if (!StringUtils.isEmpty(input)) {
				RdfUtils.loadModelFromInlineRDF(inputModel, input);
			}
		}
		for (String inputURL: getParameterValues(request, "inputURL")) {
			if (!StringUtils.isEmpty(inputURL)) {
				contentType.readModel(inputModel, new URL(inputURL).openStream(), "");
			}
		}
		if (log.isTraceEnabled())
			log.trace("assembled input model:\n" + RdfUtils.logStatements(inputModel));
		return inputModel;
	}
	
	/**
	 * Return the values of the specified parameter, or the empty list if
	 * there are none.  Not null, because that would be dumb.
	 * @return the values of the specified parameter
	 */
	protected static String[] getParameterValues(HttpServletRequest request, String param)
	{
		String[] values = request.getParameterValues(param);
		return values != null ? values : ArrayUtils.EMPTY_STRING_ARRAY;
	}

	@Override
	public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		doGet(request, response);
	}
	
	protected String getTaskURL(HttpServletRequest request, String taskID)
	{
		// TODO this seems really inefficient...
		String explicitServiceURL = getServiceURL();
		StringBuffer buf = explicitServiceURL != null ?
				new StringBuffer(explicitServiceURL) : request.getRequestURL();
		buf.append("?taskID=");
		buf.append(taskID);
		return buf.toString();
	}

	protected static boolean finishesBeforeHTTPTimeout(ServiceProxyTask task)
	{
		StopWatch watch = new StopWatch();
		watch.start();
		while (watch.getTime() < 30000) { // loop for 30 seconds...
			if (log.isTraceEnabled())
				log.trace(String.format("task %s: checking if finished", task.getId()));
			if (task.isFinished())
				return true;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// just go back to sleep...
			}
		}
		return false;
	}
}
