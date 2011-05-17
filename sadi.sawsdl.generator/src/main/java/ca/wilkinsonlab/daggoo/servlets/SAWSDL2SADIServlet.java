package ca.wilkinsonlab.daggoo.servlets;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.Velocity;

import ca.wilkinsonlab.daggoo.SAWSDLService;
import ca.wilkinsonlab.daggoo.engine.DaggooTask;
import ca.wilkinsonlab.daggoo.listeners.ServletContextListener;
import ca.wilkinsonlab.daggoo.utils.IOUtils;
import ca.wilkinsonlab.daggoo.utils.WSDLConfig;
import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.tasks.TaskManager;
import ca.wilkinsonlab.sadi.utils.ContentType;
import ca.wilkinsonlab.sadi.utils.QueryableErrorHandler;
import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Servlet implementation class SAWSDL2SADIServlet
 */
public class SAWSDL2SADIServlet extends AsynchronousServiceServlet {

    public static final String POLL_PARAMETER = "poll";
    
    public static final String SERVLET_NAME = SAWSDL2SADIServlet.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SAWSDL2SADIServlet.class);

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SAWSDL2SADIServlet() {
	super();
    }

    /**
     * @see Servlet#init(ServletConfig)
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
	super.init(config);
	Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogChute");
    }
    
    @Override
    public void init() throws ServletException {
	// need to init these since we dont call through to parents init()
	errorHandler = new QueryableErrorHandler();
        modelMaker = createModelMaker();
    }
    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
    	String taskId = request.getParameter(POLL_PARAMETER);
    	String[] pathInfo = getPathInfo(request);
    	String service = pathInfo[0];
    	String extended = pathInfo[1];
    	if (!StringUtils.isEmpty(service)) {
    		if (taskId != null) {
    			DaggooTask task = (DaggooTask)TaskManager.getInstance().getTask(taskId);
    			if (task == null || !task.getServiceName().equals(service)) {
    				// send error response
    				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("invocation for '%s' does not exist", taskId));
    				return;
    			} else if (task.isFinished()) {
    				response.setContentType(getContentType(request).getHTTPHeader());
    				Throwable error = task.getError();
    				if (error == null ) {
    					outputSuccessResponse(response, task.getOutputModel());
    					TaskManager.getInstance().disposeTask(taskId);
    				} else {
    					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error.toString());
    					return;
    				}
    			} else {
    				// not finished
    				String redirectUrl = getPollUrl(request, task.getId());
    				long waitTime = getSuggestedWaitTime(task);
    				outputInterimResponse(response, redirectUrl, waitTime);
    			}
    		} else {
    			@SuppressWarnings("unchecked")
    			Map<String, SAWSDLService> services = (Map<String, SAWSDLService>) getServletContext().getAttribute(ServletContextListener.SAWSDL_SERVICE_MAP);
    			if (services == null) {
    				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Null service map encountered. Please report this error!");
    				return;
    			}
    			log.debug(service + " " + extended);
    			boolean sendServiceDescription = true;

    			// more parts to this path ...
    			// service has the service name ... should we check to see if
    			// its empty?
    			// extended has {lifting|lowering|sawsdl} filename ...
    			if (!StringUtils.isEmpty(extended)) {
    				sendServiceDescription = false;
    			}

    			// if service doesnt have a / in it, check for service
    			// description
    			if (!sendServiceDescription) {
    				// TODO check to see if we are asking for the SAWSDL,
    				// lowering or lifting schema
    				String serviceDir = (String) getServletContext().getAttribute(ServletContextListener.SERVICE_DIR_LOCATION);
    				if (serviceDir == null) {
    					response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    					return;
    				}
    				serviceDir += (serviceDir.endsWith("/") ? "" : "/");
    				String file = serviceDir;
    				if (!services.containsKey(service)) {
    					response.sendError(HttpServletResponse.SC_NOT_FOUND,
    							String.format("service, %s, was not found.", service));
    					return;
    				}
    				SAWSDLService s = services.get(service);

    				if (extended.equals("lifting")) {
    					response.setContentType("application/xml");
    					file += s.getLiftingSchemaLocation();
    				} else if (extended.equals("lowering")) {
    					response.setContentType("application/xml");
    					file += s.getLoweringSchemaLocation();
    				} else if (extended.equals("owl")) {
    					response.setContentType("application/rdf+xml");
    					file += s.getOwlClassLocation();
    				} else if (extended.equals("sawsdl")) {
    					response.setContentType("application/xml");
    					// this one is the full path ... no +=
    					file = s.getWsdlLocation();
    				} else {
    					response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Please enter one of [lifting|lowering|owl|sawsdl], not " + extended);
    					return;
    				}
    				// Send the file.
    				OutputStream out = response.getOutputStream();
    				IOUtils.returnFile(file, out);
    				return;

    			} else {
    				if (services.containsKey(service)) {
    					// TODO make the locationURI, etc be this domain ...
    					response.setContentType("application/rdf+xml");
    					String serviceDir = (String) getServletContext().getAttribute(ServletContextListener.SERVICE_DIR_LOCATION);
    					if (serviceDir == null) {
    						response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    						return;
    					}
    					serviceDir += (serviceDir.endsWith("/") ? "" : "/");
    					String file = String.format("%s%s/description.rdf", serviceDir, service);
    					// Send the file.
    					OutputStream out = response.getOutputStream();
    					IOUtils.returnFile(file, out);
    					return;

    				} else {
    					// TODO no service, throw exception?
    					response.sendError(HttpServletResponse.SC_NOT_FOUND,
    							String.format("service, %s, was not found.", service));
    					return;
    				}
    			}
    		}
    	} else {
			log.debug("empty request, redirecting to index.jsp...");
			getServletConfig().getServletContext().getRequestDispatcher("/index.jsp").forward(request, response);
			return;
    	}
    }

	/* TODO this could be cleaned up further...
	 */
	private static String[] getPathInfo(HttpServletRequest request) {
		String service = "";
		String extended = "";
		String[] pathInfo = request.getPathInfo().split("/");
		if (pathInfo.length > 0) {
    		if (StringUtils.isEmpty(pathInfo[0])) { // started with "/"
    			if (pathInfo.length > 1){ 
	    			service = pathInfo[1];
	    			if (pathInfo.length > 2)
	    				extended = pathInfo[2];
    			}
    		} else { // did not start with "/"
    			log.info("if this happens, Eddie was right");
    			service = pathInfo[0];
    			if (pathInfo.length > 1)
    				extended = pathInfo[1];
    		}
    	}
    	return new String[]{ service, extended };
	}
    
    public static ContentType getContentType(HttpServletRequest request) {
            ContentType contentType = null;
            for (Enumeration<?> headers = request.getHeaders("Accept"); headers.hasMoreElements(); ) {
                    String headerString = (String)headers.nextElement();
                    for (String header: headerString.split(",\\s*")) {
                            contentType = ContentType.getContentType(header);
                            if (contentType != null)
                                    return contentType;
                    }
            }
            return ContentType.RDF_XML;
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
    	String[] pathInfo = getPathInfo(request);
    	String service = pathInfo[0];
//    	String extended = pathInfo[1];
		if (StringUtils.isEmpty(service)) {
		    // show form
			log.debug("no service request posted, redirecting to index.jsp...");
			getServletConfig().getServletContext().getRequestDispatcher("/index.jsp").forward(request, response);
		    return;
		} else {	    
			@SuppressWarnings("unchecked")
			Map<String, SAWSDLService> services = (Map<String, SAWSDLService>) getServletContext().getAttribute(ServletContextListener.SAWSDL_SERVICE_MAP);
			if (services == null) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Null service map encountered. Please report this error!");
				return;
			}
			response.setContentType(getContentType(request).getHTTPHeader());
			// create a new task
			SAWSDLService s = services.get(service);
			if (s != null) {
				s.setRequest(request);
				s.setResponse(response);
				String mappingPrefix = request.getRequestURL().toString();
				mappingPrefix = mappingPrefix.substring(0, mappingPrefix.length() - (service.length()+1));
				//String mappingPrefix = mappingPrefixes.get(service);
				processInput(s, mappingPrefix);
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}
    }
    
    protected void processInput(SAWSDLService s, String mappingPrefix) throws IOException {
	
    	String inputString = readInputFromRequest(s.getRequest());
    	Model inputModel = readInputIntoModel(s.getRequest(), inputString);
    	Model outputModel = createOutputModel();
    	WSDLConfig wsdl = null;
    	try {
    		wsdl = new WSDLConfig(new File(s.getWsdlLocation()).toURI().toURL());
    	} catch (Exception e) {
    		outputErrorResponse(s.getResponse(), e);
    		if (inputModel != null)
    			inputModel.close();
    		if (outputModel != null)
    			outputModel.close();
    		return;
    	}
    	wsdl.setCurrentService(s.getName());

    	/* process each input batch in it's own task thread...*/
    	DaggooTask task = new DaggooTask(s, mappingPrefix, inputString);
    	TaskManager.getInstance().startTask(task);

    	String inputClass = wsdl.getPrimaryInputs().keySet().iterator().next();
    	inputClass = wsdl.getPrimaryInputs().get(inputClass).substring(inputClass.length() + 1);
    	Collection<Resource> newBatch = inputModel.listResourcesWithProperty(RDF.type, inputModel.createResource(inputClass)).toList();
    	try {
    		/* add the poll location data to the output that will be returned immediately...*/

    		Resource pollResource = outputModel.createResource(getPollUrl(s.getRequest(), task.getId()));
    		for (Resource inputNode: newBatch) {
    			Resource outputNode = outputModel.getResource(inputNode.getURI());
    			outputNode.addProperty(RDFS.isDefinedBy, pollResource);
    		}
    		if (inputModel != null) {
    			inputModel.close();   
    		}   
    		outputSuccessResponse(s.getResponse(), outputModel);
    	} catch (Exception e) {
    		outputErrorResponse(s.getResponse(), e);
    		if (inputModel != null)
    			inputModel.close();
    		if (outputModel != null)
    			outputModel.close();
    	}
    }
    
    protected String readInputFromRequest(HttpServletRequest request) throws IOException {
	Model inputModel = ModelFactory.createMemModelMaker().createFreshModel();
	String contentType = request.getContentType();
	if (contentType.equals("application/rdf+xml")) {
	    inputModel.read(request.getInputStream(), "", "RDF/XML");
	} else if (contentType.equals("text/rdf+n3")) {
	    inputModel.read(request.getInputStream(), "", "N3");
	} else {
	    inputModel.read(request.getInputStream(), "");
	}
	StringWriter writer = new StringWriter();
	inputModel.write(writer, "RDF/XML-ABBREV");
	return writer.toString();
    }
    
    protected Model readInputIntoModel(HttpServletRequest request, String input) throws IOException {
	Model inputModel = ModelFactory.createMemModelMaker().createFreshModel();
	String contentType = request.getContentType();
	if (contentType.equals("application/rdf+xml")) {
	    inputModel.read(new ByteArrayInputStream(input.getBytes()), "", "RDF/XML");
	} else if (contentType.equals("text/rdf+n3")) {
	    inputModel.read(new ByteArrayInputStream(input.getBytes()), "", "N3");
	} else {
	    inputModel.read(new ByteArrayInputStream(input.getBytes()), "");
	}
	return inputModel;
	
    }
    
    private void outputInterimResponse(HttpServletResponse response, String redirectUrl, long waitTime) throws IOException {
	response.setHeader("Pragma", String.format("%s = %s", SADI.ASYNC_HEADER, waitTime));
	response.setHeader("Retry-After", String.valueOf(waitTime/1000));
	response.sendRedirect(redirectUrl);
    }
    
    @Override
    protected String getPollUrl(HttpServletRequest request, String taskId) {
	return String.format("%s?%s=%s", request.getRequestURL().toString(), POLL_PARAMETER, taskId);
    }
}
