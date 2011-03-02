package ca.wilkinsonlab.daggoo.servlets;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileChangeEvent;
import org.apache.commons.vfs.FileListener;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.DefaultFileMonitor;
import org.apache.velocity.app.Velocity;
import org.xml.sax.SAXException;

import ca.wilkinsonlab.daggoo.SAWSDLService;
import ca.wilkinsonlab.daggoo.engine.DaggooTask;
import ca.wilkinsonlab.daggoo.listeners.ServletContextListener;
import ca.wilkinsonlab.daggoo.utils.IOUtils;
import ca.wilkinsonlab.daggoo.utils.WSDLConfig;
import ca.wilkinsonlab.sadi.tasks.Task;
import ca.wilkinsonlab.sadi.tasks.TaskManager;
import ca.wilkinsonlab.sadi.utils.DurationUtils;
import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Servlet implementation class SAWSDL2SADIServlet
 */
public class SAWSDL2SADIServlet extends HttpServlet {

    public static final String POLL_PARAMETER = "poll";
    
    public static final String SERVLET_NAME = SAWSDL2SADIServlet.class.getSimpleName();

    private ConcurrentHashMap<String, SAWSDLService> services = new ConcurrentHashMap<String, SAWSDLService>();
    
    private ConcurrentHashMap<String, String> mappingPrefixes = new ConcurrentHashMap<String, String>();

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SAWSDL2SADIServlet.class);
    
    private DefaultFileMonitor fm;

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
	
	final String serviceMappings = (String) config.getServletContext().getAttribute(ServletContextListener.MAPPING_FILE_LOCATION);
	final String serviceDir = (String) config.getServletContext().getAttribute(ServletContextListener.SERVICE_DIR_LOCATION);

	if (serviceMappings != null) {
	    init_services(serviceMappings, serviceDir);
	    // TODO move this out of here and into the context listener ... on fileChanged, we can set a flag that something has changed ...
	    // FIXME this will cause a memory leak
	    try {
		FileSystemManager fsManager = VFS.getManager();
		// listen for changes to serviceMappings
		FileObject fileObject = fsManager.resolveFile(serviceMappings);
		fm = new DefaultFileMonitor(new FileListener() {

		    public void fileDeleted(FileChangeEvent arg0) throws Exception {
			// shouldn't happen
		    }

		    public void fileCreated(FileChangeEvent arg0) throws Exception {
			// not necessary
		    }

		    public void fileChanged(FileChangeEvent arg0) throws Exception {
			// reload our mappings ...
			init_services(serviceMappings, serviceDir);
		    }
		}); 
		fm.addFile(fileObject);
		fm.start();
	    } catch (Exception e) {
		// TODO
	    }
	    
	} else {
	    throw new ServletException("Unable to find init parameters. Please check your configuration!");
	}
    }

    
    
    @Override
    public void destroy() {
        super.destroy();
        // FIXME is this enough to stop polling?
        if (fm != null)
            fm.stop();
        fm = null;
    }
    
    /*
     * @param serviceMappings
     * @param serviceDir
     */
    private void init_services(String serviceMappings, String serviceDir) {
	try {
	services = new ConcurrentHashMap<String, SAWSDLService>();
	for (SAWSDLService s : IOUtils.getSAWSDLServices(new File(serviceMappings))) {
	    if (serviceDir != null) {
		File realPath = new File(serviceDir,
			s.getWsdlLocation());
		s.setWsdlLocation(realPath.getAbsolutePath());
		// a lowering/lifing mapping prefix for later use
		mappingPrefixes.put(s.getName(), new File(serviceDir).toURI().toURL().toString());
	    }
	    services.put(s.getName(), s);
	}
	} catch (SAXException e) {
	// TODO throw exception?
	e.printStackTrace();
	} catch (IOException e) {
	// TODO throw exception?
	e.printStackTrace();
	}
	log.info(services);
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
	    throws ServletException, IOException {
	String taskId = request.getParameter(POLL_PARAMETER);
	if (taskId != null) {
	    
	    String requestURI = request.getRequestURI();
	    String service = "";
	    Pattern regexp = Pattern.compile(String.format("%s/(\\w+)*$", getClass().getSimpleName()));
	    Matcher matcher = regexp.matcher(requestURI);

	    if (matcher.find()) {
		service = matcher.group(1);
	    }

	    if (service.startsWith("/")) {
		// relic, shouldnt get here ...
		service = service.substring(1);
	    }
	    DaggooTask task = (DaggooTask)TaskManager.getInstance().getTask(taskId);
	    if (task == null || !task.getServiceName().equals(service)) {
		// send error response
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("invocation for '%s' does not exist", taskId));
	    } else if (task.isFinished()) {
		Throwable error = task.getError();
		if (error == null ) {
		    outputSuccessResponse(response, task.getOutputModel());
		    TaskManager.getInstance().disposeTask(taskId);
		} else {
		    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error.toString());
		}
	    } else {
		// not finished
		String redirectUrl = getPollUrl(request, task.getId());
                long waitTime = getSuggestedWaitTime(task);
                outputInterimResponse(response, redirectUrl, waitTime);
	    }
	} else {
	    // show the service description
	    String requestURI = request.getRequestURI();
	    //String service = requestURI.length() >= servletPath.length() ? requestURI.substring(servletPath.length()) : "";
	    String service = "";
	    String extended = "";
	    Pattern regexp = Pattern.compile(String.format("%s/(\\w+)*(/.*)*$", getClass().getSimpleName()));
	    Matcher matcher = regexp.matcher(requestURI);

	    if (matcher.find()) {
		service = matcher.group(1) == null ? "" : matcher.group(1);
		extended = matcher.group(2) == null ? "" : matcher.group(2);
		if (extended.startsWith("/")){
		    extended = extended.substring(1);
		}
		log.debug(String.format("%d serv(%s), ext(%s)", matcher.groupCount(), service,extended));
	    }
	    
	    if (service.isEmpty()) {
		// show form
		response.sendRedirect(String.format(
			"%s%s",
			request.getContextPath() == null
				|| request.getContextPath().trim().equals("") ? ""
				: request.getContextPath(), "/"
				+ WSDL2SAWSDL.class.getSimpleName()));
		log.info("empty request, redirecting to WSDL2SAWSDL ...");
		return;
	    } else {
		log.info(service + " " + extended);
		boolean sendServiceDescription = true;

		// more parts to this path ...
		// service has the service name ... should we check to see if
		// its empty?
		// extended has {lifting|lowering|sawsdl} filename ...
		if (!extended.trim().equals("")) {
		    sendServiceDescription = false;
		}

		// if service doesnt have a / in it, check for service
		// description
		if (!sendServiceDescription) {
		    // TODO check to see if we are asking for the SAWSDL,
		    // lowering or lifting schema
		    String serviceDir = (String) getServletContext()
			    .getAttribute(
				    ServletContextListener.SERVICE_DIR_LOCATION);
		    if (serviceDir == null) {
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			return;
		    }
		    serviceDir += (serviceDir.endsWith("/") ? "" : "/");
		    String file = serviceDir;
		    if (!services.containsKey(service)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND,
				String.format("service, %s, was not found.",
					service));
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
			String serviceDir = (String) getServletContext()
				.getAttribute(
					ServletContextListener.SERVICE_DIR_LOCATION);
			if (serviceDir == null) {
			    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			    return;
			}
			serviceDir += (serviceDir.endsWith("/") ? "" : "/");
			String file = String.format("%s%s/description.rdf",
				serviceDir, service);
			// Send the file.
			OutputStream out = response.getOutputStream();
			IOUtils.returnFile(file, out);
			return;

		    } else {
			// TODO no service, throw exception?
			response.sendError(HttpServletResponse.SC_NOT_FOUND,
				String.format("service, %s, was not found.",
					service));
			return;
		    }
		}
		

	    }
	}
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	String requestURI = request.getRequestURI();
	String service = "";
	Pattern regexp = Pattern.compile(String.format("%s/(\\w+)*$", getClass().getSimpleName()));
	Matcher matcher = regexp.matcher(requestURI);

	if (matcher.find()) {
	    service = matcher.group(1);
	}

	if (service.startsWith("/")) {
	    // relic, shouldnt get here ...
	    service = service.substring(1);
	}
	if (service.startsWith("/")) {
	    service = service.substring(1);
	}
	if (service.isEmpty()) {
	    // show form
	    log.info("no service request posted; TODO create SAWSDL");
	    response.sendRedirect("WSDL2SAWSDL");
	    return;
	} else {
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
	
	String inputString = readInput(s.getRequest());
        Model inputModel = readInputIntoModel(s.getRequest(), inputString);
        WSDLConfig wsdl = null;
        try {
            wsdl = new WSDLConfig(new File(s.getWsdlLocation()).toURI().toURL());
        } catch (Exception e) {
            // TODO what should i do?
            e.printStackTrace();
            if (inputModel != null)
        	inputModel.close();
            return;
        }
        wsdl.setCurrentService(s.getName());
        
        /* process each input batch in it's own task thread...*/
        DaggooTask task = new DaggooTask(s, mappingPrefix, inputString);
        TaskManager.getInstance().startTask(task);
        
	String inputClass = wsdl.getPrimaryInputs().keySet().iterator().next();
	inputClass = wsdl.getPrimaryInputs().get(inputClass).substring(inputClass.length() + 1);
        Collection<Resource> newBatch = inputModel.listResourcesWithProperty(RDF.type, inputModel.createResource(inputClass)).toList();
        
        /* add the poll location data to the output that will be returned immediately...*/
        Model outputModel = createOutputModel();
        Resource pollResource = outputModel.createResource(getPollUrl(s.getRequest(), task.getId()));
        for (Resource inputNode: newBatch) {
                Resource outputNode = outputModel.getResource(inputNode.getURI());
                outputNode.addProperty(RDFS.isDefinedBy, pollResource);
        }
        if (inputModel != null) {
            inputModel.close();   
        }       
        outputSuccessResponse(s.getResponse(), outputModel);
    }

    protected String readInput(HttpServletRequest request) throws IOException
    {
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
    protected Model readInputIntoModel(HttpServletRequest request, String input) throws IOException
    {
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
    
    private void outputInterimResponse(HttpServletResponse response, String redirectUrl, long waitTime) throws IOException
    {
            /* according to spec, "response SHOULD contain a short hypertext note with a hyperlink to the new URI(s)",
             * but Tomcat doesn't want us to send a body with the redirect; this probably won't be a problem...
            Model model = modelMaker.createFreshModel();
            model.createResource(redirectUrl, outputClass);
            model.write(response.getWriter());
             */
            response.setHeader("Pragma", String.format("%s = %s", SADI.ASYNC_HEADER, DurationUtils.format(waitTime)));
            response.sendRedirect(redirectUrl);
    }
    
    protected void outputSuccessResponse(HttpServletResponse response, Model outputModel) throws IOException
    {
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            response.setContentType("application/rdf+xml");
            RDFWriter writer = outputModel.getWriter("RDF/XML-ABBREV");
            writer.write(outputModel, response.getWriter(), "");
    }
    
    protected String getPollUrl(HttpServletRequest request, String taskId)
    {
            /* TODO allow override of request URL?
             * not really useful if proxies are set up properly...
             */
            return String.format("%s?%s=%s", request.getRequestURL().toString(), POLL_PARAMETER, taskId);
    }
    
    protected long getSuggestedWaitTime(Task task)
    {
            return 5000;
    }

    protected Model prepareOutputModel(Model inputModel, Resource inputClass, Resource outputClass)
    {
	// TODO instead of this, use the input URIS and a velocity template to contruct these temporary models
	/* 2009-03-17 Luke and Mark decide that inputs will be explicitly typed,
	 * so reasoning is not required here...
	 */
	Model outputModel = createOutputModel();
	for (ResIterator i = inputModel.listSubjectsWithProperty(RDF.type, inputClass); i.hasNext();) {
	    outputModel.createResource(i.nextResource().getURI(), outputClass);
	}
	return outputModel;
    }
    protected Model createOutputModel()
    {
	Model model = ModelFactory.createMemModelMaker().createFreshModel();
	log.trace(String.format("created output model %s", model.hashCode()));
	return model;
    }


}
