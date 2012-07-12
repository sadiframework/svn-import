package ca.wilkinsonlab.sadi.service.proxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.sadiframework.service.ServiceServlet;
import org.sadiframework.tasks.TaskManager;
import org.sadiframework.utils.ContentType;
import org.sadiframework.utils.QueryableErrorHandler;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;

public class ServiceProxyServlet extends ServiceServlet
{
	private static final Logger log = Logger.getLogger(ServiceProxyServlet.class);
	private static final long serialVersionUID = 1L;

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
		modelMaker = createModelMaker();  // TODO support persistent models in properties file?
	}
	
	@Override
	public void doGet (HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String[] path = StringUtils.split(request.getPathInfo(), '/');
		try {
			String taskID = path[0];
			if (taskID.equals("js")) {
				response.setContentType("application/javascript");
				response.getWriter().print(IOUtils.toString(ServiceProxyServlet.class.getResourceAsStream(request.getPathInfo())));
				return;
			} else if (request.getPathInfo().endsWith("html") || request.getPathInfo().endsWith("rdf")) {
				response.getWriter().print(IOUtils.toString(ServiceProxyServlet.class.getResourceAsStream("/html" + request.getPathInfo())));
				return;
			}
			ServiceProxyTask task = (ServiceProxyTask)TaskManager.getInstance().getTask(taskID);
			if (isFinished(task)) {
				log.debug(String.format("task %s: writing output model", taskID));
				Model model = task.getModel();
				String contentType = ServiceServlet.getContentType(request).getHTTPHeader();
				response.setContentType(contentType);
				RDFWriter writer = model.getWriter(ContentType.getContentType(contentType).getJenaLanguage());
				writer.write(model, response.getWriter(), "");
			} else {
				// send redirect to avoid HTTP timeout...
				String redirectURL = getTaskURL(request, taskID);
				log.debug(String.format("task %s: sending redirect to %s", taskID, redirectURL));
				response.sendRedirect(redirectURL);
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "no such resource");
		}
	}
	
	private String getTaskURL(HttpServletRequest request, String taskID)
	{
		String baseURL = getServiceURL();
		if (baseURL == null) {
			baseURL = request.getRequestURL().toString();
			String pathInfo = request.getPathInfo();
			if (pathInfo != null) {
				pathInfo = pathInfo.substring(1); // remove leading slash...
				try {
					pathInfo = URLEncoder.encode(pathInfo, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// this shouldn't happen...
					log.error(e.toString(), e);
				}
				pathInfo = "/" + pathInfo; // re-add leading slash...
				baseURL = StringUtils.substring(baseURL, 0, -pathInfo.length());
			}
		}
		return String.format("%s/%s", baseURL, taskID);
	}

	private boolean isFinished(ServiceProxyTask task)
	{
		StopWatch watch = new StopWatch();
		watch.start();
		while (watch.getTime() < 30000) { // loop for 30 seconds...
			log.trace(String.format("task %s: checking if finished", task.getId()));
			if (task.isFinished())
				return true;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		return false;
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		if (StringUtils.isEmpty(request.getPathInfo())) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no remote service specified");
		} else {
			String serviceURL = request.getPathInfo().substring(1); // skip leading slash...
			log.debug(String.format("new request to invoke service %s", serviceURL));
			Model inputModel = readInput(request);
			ServiceProxyTask task = new ServiceProxyTask(serviceURL, inputModel);
			String taskID = TaskManager.getInstance().startTask(task);
			String taskURL = getTaskURL(request, taskID);
			response.sendRedirect(taskURL);
		}
	}
}
