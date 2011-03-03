package ca.wilkinsonlab.daggoo.servlets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import ca.wilkinsonlab.daggoo.OwlDatatypeMapping;
import ca.wilkinsonlab.daggoo.SAWSDLService;
import ca.wilkinsonlab.daggoo.WSDLParser;
import ca.wilkinsonlab.daggoo.listeners.ServletContextListener;
import ca.wilkinsonlab.daggoo.utils.IOUtils;

/**
 * Servlet implementation class WSDL2SAWSDL
 */
public class WSDL2SAWSDL extends HttpServlet {
    
    private static final long serialVersionUID = 1L;

    private static String TEMPLATE_DIR = "";
    
    private static String BASE_URL = null;
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogChute");

        
        TEMPLATE_DIR = (String) config.getServletContext().getAttribute(ServletContextListener.TEMPLATE_LOCATION);
        if (TEMPLATE_DIR == null) {
            TEMPLATE_DIR = "";
        }
        
        URL url = getClass().getResource(String.format("%s/%s", TEMPLATE_DIR, "HEADER.vm"));
        if (url == null) {
            Velocity.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH,TEMPLATE_DIR);
        } else {
            try {
        	String s = new File(url.toURI()).getParent();
        	TEMPLATE_DIR = s;
		Velocity.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH,s);
	    } catch (URISyntaxException e) {
		Velocity.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH,TEMPLATE_DIR);
	    }
        }
        System.out.println("init: " + TEMPLATE_DIR);
    }
    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public WSDL2SAWSDL() {
	super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request,
	    HttpServletResponse response) throws ServletException, IOException {
	if (BASE_URL == null) {
	    // had problems setting the BASE_URL in the listener, so I am going to do it here ...
	    String contextPath = request.getContextPath() == null || request.getContextPath().trim().equals("") ? 
		    "" : 
			request.getContextPath()+ "/";
	    String s = (String)getServletContext().getAttribute(ServletContextListener.SERVER_BASE_ADDRESS);
	    if (contextPath.startsWith("/")) {
		contextPath = contextPath.length() > 1 ? contextPath.substring(1) : "";
	    }
	    s = String.format("%s%s",
		    s,
		    contextPath
	    );
	    BASE_URL = s;
	}
	// got hold of the user session
	HttpSession session = request.getSession(true); // true creates a new session if one doesnt exist
	if (session.isNew()) {
	    session.setAttribute("state", 0);
	}
	int state =((Integer)session.getAttribute("state")).intValue(); 
	switch (state) {
	case 0: {
	    Template template = null;
	    try {
		template = Velocity.getTemplate("step_1.vm");
	    } catch (Exception e) {
		e.printStackTrace();
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
	    }
	    VelocityContext context = new VelocityContext();
	    context.put("title", String.format("%s - step 1: provide WSDL", this.getClass().getSimpleName()));
	    context.put("servlet_action", this.getClass().getSimpleName());
	    if (session.getAttribute("error") != null) {
		context.put("error", session.getAttribute("error"));
		session.removeAttribute("error");
	    }
	    // fill in the context with our values
	    StringWriter writer = new StringWriter();
	    template.merge(context, writer);
	    response.setContentType("text/html");
	    response.getOutputStream().println(writer.toString());
	    session.setAttribute("state", 1);
	}
	break;
	case 1: {
	    String parameter = request.getParameter("wsdl");
	    if (parameter == null) {
		parameter = (String)session.getAttribute("wsdl");
	    }
	    boolean hasNext = request.getParameter("next") != null && request.getParameter("next").trim().equals("true");
	    // check if we submitted form or if the page was reloaded ...
	    if (!hasNext) {
		// page reload
		int s = ((Integer)session.getAttribute("state")).intValue() - 1;
		session.setAttribute("state", s <0 ? 0 : s);
		response.
		sendRedirect(String.format(
			"%s%s",
			request.getContextPath() == null || request.getContextPath().trim().equals("") ? 
				""
				: request.getContextPath(), "/"
				+ getClass().getSimpleName()));
		return;
	    }
	    
	    URL url = null;
	    WSDLParser wSDLParser = null;
	    ArrayList<String> services = null;
	    try {
		url = new URL(parameter);
		wSDLParser = new WSDLParser(url);
		services = wSDLParser.getOperationNames();
		Collections.sort(services);
	    } catch (Exception e) {
		session.setAttribute("error", String.format("Error extracting Operation names from the wsdl:\n\n'%s'.", e.getMessage()));
		// back to step 0 ... 
		session.setAttribute("state", 0);
		response.sendRedirect(String.format(
			"%s%s",
			request.getContextPath() == null
				|| request.getContextPath().trim().equals("") ? ""
				: request.getContextPath(), "/"
				+ WSDL2SAWSDL.class.getSimpleName()));
		return;
	    }
	    response.setContentType("text/html");
	    session.setAttribute("wsdl", parameter);
	    if (services != null) {
		Template template = Velocity.getTemplate("step_2.vm");
		VelocityContext context = new VelocityContext();
		// fill in the context with our values
		context.put("title", String.format("%s - step 2: select service", this.getClass().getSimpleName()));
		context.put("services", services);
		context.put("wsdlLoc", parameter);
		context.put("servlet_action", this.getClass().getSimpleName());
		if (session.getAttribute("error") != null) {
		    context.put("error", session.getAttribute("error"));
		    session.removeAttribute("error");
		}
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		response.getOutputStream().println(writer.toString());
		// invalidate the session if we didnt find any services.
		if (services.size() > 0) {
		    session.setAttribute("state", 2);
		    session.setAttribute("WSDLParser", wSDLParser);
		} else {
		    session.setAttribute("state", 0);
		    session.invalidate();
		}
	    }
	    
	}
	break;
	case 2: {
	    boolean hasNext = request.getParameter("next") != null && request.getParameter("next").trim().equals("true");
	    // check if we submitted form or if the page was reloaded ...
	    if (!hasNext) {
		// page reload
		int s = ((Integer)session.getAttribute("state")).intValue() - 1;
		session.setAttribute("state", s <0 ? 0 : s);
		response.sendRedirect(String.format(
			"%s%s",
			request.getContextPath() == null
				|| request.getContextPath().trim().equals("") ? ""
				: request.getContextPath(), "/"
				+ WSDL2SAWSDL.class.getSimpleName()));
		return;
	    }
	    String parameter = request.getParameter("operationname");
	    if (parameter == null) {
		// got here via page reload?
		if (session.getAttribute("operationname") != null) {
		    parameter = (String)session.getAttribute("operationname");
		}
	    }
	    session.setAttribute("operationname", parameter);
	    WSDLParser wSDLParser = (WSDLParser)session.getAttribute("WSDLParser");
	    wSDLParser.processWSDL(parameter);
	    System.out.println(wSDLParser.getInputSoapDatatypes() + "\n" + wSDLParser.getOutputSoapDatatypes());
	    response.setContentType("text/html");
	    Template template = Velocity.getTemplate("step_3.vm");
	    // fill in the context with our values
	    VelocityContext context = new VelocityContext();
	    context.put("title", String.format("%s - step 3: SADI Service Details", this.getClass().getSimpleName()));
	    context.put("operationname", parameter);
	    context.put("servlet_action", this.getClass().getSimpleName());
	    if (session.getAttribute("error") != null) {
		context.put("error", session.getAttribute("error"));
		session.removeAttribute("error");
	    }
	    StringWriter writer = new StringWriter();
	    template.merge(context, writer);
	    response.getOutputStream().println(writer.toString());
	    session.setAttribute("state", 3);
	}
	break;
	case 3: {
	    
	    boolean hasNext = request.getParameter("next") != null && request.getParameter("next").trim().equals("true");
	    // check if we submitted form or if the page was reloaded ...
	    if (!hasNext) {
		// page reload
		int s = ((Integer)session.getAttribute("state")).intValue() - 1;
		session.setAttribute("state", s <0 ? 0 : s);
		response.sendRedirect(String.format(
			"%s%s",
			request.getContextPath() == null
				|| request.getContextPath().trim().equals("") ? ""
				: request.getContextPath(), "/"
				+ WSDL2SAWSDL.class.getSimpleName()));
		return;
	    }
	    String 
	    	serviceName = request.getParameter("servicename"), 
	    	serviceAuthority = request.getParameter("serviceauthority"), 
	    	contactEmail = request.getParameter("serviceemail"), 
	    	description = request.getParameter("servicedescription"), 
	    	serviceType = request.getParameter("servicetype");
	    
	    // check the validity of the service name ...
	    try{
		StringBuilder errors = new StringBuilder("Please correct the following errors before continuing:<br/><br/>");
		int errorCount = 0;
		if (!isValidName(serviceName)) {
		    errorCount++;
		    errors.append("Service name already exists. Please choose a new one!<br/>");
		}
		if (!isValidEmailAddress(contactEmail)) {
		    errorCount++;
		    errors.append("Please enter a valid email address!<br/>");
		}
		if (!isValidAuthority(serviceAuthority)) {
		    errorCount++;
		    errors.append("Please enter a valid service authority! Valid authorities look like 'sadiframework.org', 'www.somedomain.com', etc<br/>");   
		}
		if (errorCount > 0) {
		    response.setContentType("text/html");
		    Template template = Velocity.getTemplate("step_3.vm");
		    // fill in the context with our values
		    VelocityContext context = new VelocityContext();
		    context.put("title", String.format("%s - step 3: SADI Service Details", this.getClass().getSimpleName()));
		    context.put("operationname", session.getAttribute("operationname"));
		    context.put("error", errors.toString());
		    context.put("servicename", serviceName);
		    context.put("serviceemail", contactEmail);
		    context.put("servicedescription", description);
		    context.put("serviceauthority", serviceAuthority);
		    context.put("servicetype", serviceType);
		    context.put("servlet_action", this.getClass().getSimpleName());
		    StringWriter writer = new StringWriter();
		    template.merge(context, writer);
		    response.getOutputStream().println(writer.toString());
		    session.setAttribute("state", 3);
		    return;
		}
	    } catch (Exception e) {
		// shouldnt get here ...
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
		return;
	    }
	    
	    session.setAttribute("servicename", serviceName);
	    session.setAttribute("servicedescription", description);
	    session.setAttribute("serviceauthority", serviceAuthority);
	    session.setAttribute("servicetype", serviceType);
	    session.setAttribute("serviceemail", contactEmail);
	    response.setContentType("text/html");
	    Template template = Velocity.getTemplate("step_4.vm");
	    // fill in the context with our values
	    VelocityContext context = new VelocityContext();
	    context.put("title", String.format("%s - step 4: SOAP 2 SADI input mapping", this.getClass().getSimpleName()));
	    context.put("operation", session.getAttribute("operationname"));
	    context.put("servlet_action", this.getClass().getSimpleName());
	    WSDLParser wSDLParser = (WSDLParser)session.getAttribute("WSDLParser");
	    
	    // update the sawsdl document
	    String baseURL = String.format(
		    "%s%s/%s/", 
		    BASE_URL, 
		    SAWSDL2SADIServlet.SERVLET_NAME, 
		    session.getAttribute("servicename"));
	    wSDLParser.addServiceAttributesToSAWSDL(baseURL, serviceName, serviceAuthority, serviceType, contactEmail, description);
	    
	    context.put("soap_inputs", wSDLParser.getInputSoapDatatypeParameterNames());
	    context.put("required", wSDLParser.getInputSoapDatatypeRequirements());
	    if (session.getAttribute("error") != null) {
		context.put("error", session.getAttribute("error"));
		session.removeAttribute("error");
	    }
	    StringWriter writer = new StringWriter();
	    template.merge(context, writer);
	    response.getOutputStream().println(writer.toString());
	    session.setAttribute("state", 4);
	}
	break;
	case 4: {
	    boolean hasNext = request.getParameter("next") != null && request.getParameter("next").trim().equals("true");
	    // check if we submitted form or if the page was reloaded ...
	    if (!hasNext) {
		// page reload
		int s = ((Integer)session.getAttribute("state")).intValue() - 1;
		session.setAttribute("state", s <0 ? 0 : s);
		response.sendRedirect(String.format(
			"%s%s",
			request.getContextPath() == null
				|| request.getContextPath().trim().equals("") ? ""
				: request.getContextPath(), "/"
				+ WSDL2SAWSDL.class.getSimpleName()));
		return;
	    }
	    // read our inputs to the service
	    WSDLParser wSDLParser = (WSDLParser)session.getAttribute("WSDLParser");
	    int count = wSDLParser.getInputSoapDatatypeParameterNames().size();
	    ArrayList<OwlDatatypeMapping> owlDatatypeMappings = new ArrayList<OwlDatatypeMapping>();
	    Map<String,Boolean> soap2isArrayMap = wSDLParser.getInputSoap2IsArrayMap();
	    for (int x = 0; x < count; x++) {
		String soapId = request.getParameter(x+"_soap_input");
		String prefix = request.getParameter(x+"_soap_prefix");
		String owlClass = request.getParameter(x+"_owl_class");
		String owlProperty = request.getParameter(x+"_owl_property");
		int extraCount = 0;
		try {
		    extraCount = Integer.parseInt(request.getParameter(x+"_extra_count"));
		} catch (NumberFormatException nfe) {
		    
		}
		if (soapId == null || soapId.trim().equals(""))
		    continue;
		if (owlClass == null || owlClass.trim().equals(""))
		    continue;
		if (owlProperty == null || owlProperty.trim().equals(""))
		    continue;
		OwlDatatypeMapping owlDatatypeMapping = new OwlDatatypeMapping();
		owlDatatypeMapping.setSoapId(soapId);
		owlDatatypeMapping.setPrefix(prefix);
		owlDatatypeMapping.setOwlProperty(owlProperty);
		owlDatatypeMapping.setValuesFrom(owlClass);
		owlDatatypeMapping.setArray(soap2isArrayMap.containsKey(soapId) ? soap2isArrayMap.get(soapId) : false);
		for (int y = 0; y < extraCount; y++) {
		    String extraProperty = request.getParameter(String.format("%s_%s_owl_property", x, y+1));
		    String extraClass = request.getParameter(String.format("%s_%s_owl_class", x, y+1));
		    owlDatatypeMapping.addExtra(extraProperty, extraClass);
		}
		owlDatatypeMappings.add(owlDatatypeMapping);
	    }
	    session.setAttribute("input_owl_x", owlDatatypeMappings);
	    System.out.println(owlDatatypeMappings);
	    
	    response.setContentType("text/html");
	    Template template = Velocity.getTemplate("step_5.vm");
	    // fill in the context with our values
	    VelocityContext context = new VelocityContext();
	    context.put("title", String.format("%s - step 5: SOAP 2 SADI output mapping", this.getClass().getSimpleName()));
	    context.put("operation", session.getAttribute("operationname"));
	    context.put("soap_outputs", wSDLParser.getOutputSoapDatatypeParameterNames());
	    context.put("required", wSDLParser.getOutputSoapDatatypeRequirements());
	    context.put("servlet_action", this.getClass().getSimpleName());
	    if (session.getAttribute("error") != null) {
		context.put("error", session.getAttribute("error"));
		session.removeAttribute("error");
	    }
	    StringWriter writer = new StringWriter();
	    template.merge(context, writer);
	    response.getOutputStream().println(writer.toString());
	    session.setAttribute("state", 5);
	}
	break;
	case 5: {
	    boolean hasNext = request.getParameter("next") != null && request.getParameter("next").trim().equals("true");
	    // check if we submitted form or if the page was reloaded ...
	    if (!hasNext) {
		// page reload
		int s = ((Integer)session.getAttribute("state")).intValue() - 1;
		session.setAttribute("state", s <0 ? 0 : s);
		response.sendRedirect(String.format(
			"%s%s",
			request.getContextPath() == null
				|| request.getContextPath().trim().equals("") ? ""
				: request.getContextPath(), "/"
				+ WSDL2SAWSDL.class.getSimpleName()));
		return;
	    }
	    // read our outputs to the service
	    String owlDocument;
	    WSDLParser wSDLParser = (WSDLParser)session.getAttribute("WSDLParser");
	    int count = wSDLParser.getOutputSoapDatatypeParameterNames().size();
	    ArrayList<OwlDatatypeMapping> owlDatatypeMappings = new ArrayList<OwlDatatypeMapping>();
	    Map<String,Boolean> soap2isArrayMap = wSDLParser.getOutputSoap2IsArrayMap();
	    for (int x = 0; x < count; x++) {
		String soapId = request.getParameter(x+"_soap_output");
		String owlClass = request.getParameter(x+"_owl_class");
		String owlProperty = request.getParameter(x+"_owl_property");
		int extraCount = 0;
		try {
		    extraCount = Integer.parseInt(request.getParameter(x+"_extra_count"));
		} catch (NumberFormatException nfe) {
		    
		}
		if (soapId == null || soapId.trim().equals(""))
		    continue;
		if (owlClass == null || owlClass.trim().equals(""))
		    continue;
		if (owlProperty == null || owlProperty.trim().equals(""))
		    continue;
		OwlDatatypeMapping owlDatatypeMapping = new OwlDatatypeMapping();
		owlDatatypeMapping.setSoapId(soapId);
		owlDatatypeMapping.setArray(soap2isArrayMap.containsKey(soapId) ? soap2isArrayMap.get(soapId) : false);
		owlDatatypeMapping.setOwlProperty(owlProperty);
		owlDatatypeMapping.setValuesFrom(owlClass);
		for (int y = 0; y < extraCount; y++) {
		    String extraProperty = request.getParameter(String.format("%s_%s_owl_property", x, y+1));
		    String extraClass = request.getParameter(String.format("%s_%s_owl_class", x, y+1));
		    owlDatatypeMapping.addExtra(extraProperty, extraClass);
		}
		owlDatatypeMappings.add(owlDatatypeMapping);
	    }
	    session.setAttribute("output_owl_x", owlDatatypeMappings);
	    Template template = Velocity.getTemplate("owl_class.vm");	    
	    VelocityContext context = new VelocityContext();
	    context.put("owl_inputs", session.getAttribute("input_owl_x"));
	    context.put("owl_outputs", owlDatatypeMappings);
	    context.put("base", 
		    String.format("%s%s/%s/owl", 
			    BASE_URL, 
			    SAWSDL2SADIServlet.SERVLET_NAME, 
			    session.getAttribute("servicename")
		    )
	    );
	    StringWriter writer = new StringWriter();
	    template.merge(context, writer);
	    // our owl document to save (contains our input/output owl classes)
	    owlDocument = writer.toString();
	    session.setAttribute("owl", owlDocument);
	    
	    
	    // save the sawsdl
	    try {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.serialize(wSDLParser.getSawsdlDoc(), baos);
		session.setAttribute("sawsdl", new String(baos.toByteArray()));
	    } catch (Exception e) {}
	    
	    // now actually save everything
	    saveGeneratedService(session);
	    
	    response.setContentType("text/html");
	    
	    template = Velocity.getTemplate("step_6.vm");
	    // fill in the context with our values
	    context = new VelocityContext();
	    context.put("title", String.format("%s - All done!", this.getClass().getSimpleName()));
	    
	    String url = String.format("%s%s/%s", BASE_URL, SAWSDL2SADIServlet.SERVLET_NAME, (String)session.getAttribute("servicename"));
	    context.put("url_description", url);
	    context.put("url_lowering", url + "/lowering");
	    context.put("url_lifting", url + "/lifting");
	    context.put("url_owl", url + "/owl");
	    context.put("url_sawsdl", url + "/sawsdl");
	    context.put("servicename", session.getAttribute("servicename"));
	    context.put("operationname", session.getAttribute("operationname"));
	    context.put("url", String.format("%s%s", BASE_URL, getClass().getSimpleName()));
	    writer = new StringWriter();
	    template.merge(context, writer);
	    response.getOutputStream().println(writer.toString());
	    session.invalidate();
	}
	break;
	default:
	    break;
	}
	

    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doPost(HttpServletRequest request,
	    HttpServletResponse response) throws ServletException, IOException {
	doGet(request, response);
    }    
    
    private boolean saveGeneratedService(HttpSession session) {	
	// needs to generate the OWL document containing our input/output classes
	// the description.rdf (service signature file) 
	// the lowering/lifting schemas 
	// the SAWSDL document
	final String serviceDir = (String) getServletContext().getAttribute(ServletContextListener.SERVICE_DIR_LOCATION);
	// update the mappings file
	final String serviceMappings = (String) getServletContext().getAttribute(ServletContextListener.MAPPING_FILE_LOCATION);
	
	String name = (String)session.getAttribute("servicename");
	// create our service directory
	File main = new File(serviceDir, name);
	if (main.mkdirs()) {
	    // save our owl document
	    try {
		FileOutputStream fos = new FileOutputStream(new File(main, String.format("%s.owl", name)));
		fos.write(session.getAttribute("owl").toString().getBytes());
		fos.flush();
		fos.close();
	    } catch (FileNotFoundException e) {
		return false;
	    } catch (IOException e) {
		return false;
	    }
	    
	    // save our sawsdl document
	    try {
		FileOutputStream fos = new FileOutputStream(new File(main, String.format("%s.sawsdl", name)));
		fos.write(session.getAttribute("sawsdl").toString().getBytes());
		fos.flush();
		fos.close();
	    } catch (FileNotFoundException e) {
		return false;
	    } catch (IOException e) {
		return false;
	    }
	    // save our service description (ALWAYS AFTER WE SAVE THE SAWSDL!!)
	    try {
		FileOutputStream fos = new FileOutputStream(new File(main, "description.rdf"));
		fos.write(get_service_description(session).getBytes());
		fos.flush();
		fos.close();
	    } catch (FileNotFoundException e) {
		return false;
	    } catch (IOException e) {
		return false;
	    }
	    
	    // save our loweringSchema
	    try {
		FileOutputStream fos = new FileOutputStream(new File(main, "lowering.xml"));
		WSDLParser wsdlParser = (WSDLParser)session.getAttribute("WSDLParser");
		Template template = Velocity.getTemplate("sparql.vm");	    
		VelocityContext context = new VelocityContext();
		context.put("owl_property_mappings", session.getAttribute("input_owl_x"));
		context.put("base", 
			String.format("%s%s/%s/owl", 
				BASE_URL, 
				SAWSDL2SADIServlet.SERVLET_NAME, 
				session.getAttribute("servicename")
			)
		);
		context.put("select_variables", wsdlParser.getInputSoapDatatypeParameterNames());
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		String sparql = writer.toString();
		// get the template portion of the lowering schema
		template = Velocity.getTemplate("lowering_template.vm");
		writer = new StringWriter();
		template.merge(context, writer);
		fos.write(IOUtils.GenerateLoweringSchema(sparql, writer.toString().trim()).getBytes());
		fos.flush();
		fos.close();
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
		return false;
	    } catch (IOException e) {
		e.printStackTrace();
		return false;
	    }
	    // save our liftingSchema
	    try {
		FileOutputStream fos = new FileOutputStream(new File(main, "lifting.xml"));
		@SuppressWarnings("unchecked")
		ArrayList<OwlDatatypeMapping> owlDatatypeMappings = (ArrayList<OwlDatatypeMapping>)session.getAttribute("output_owl_x");
		Template template = Velocity.getTemplate("lifting_template.vm");	    
		VelocityContext context = new VelocityContext();
		context.put("owl_property_mappings", session.getAttribute("output_owl_x"));
		context.put("sadi_output_owl_class", 
			String.format("%s%s/%s/owl#outputClass", 
				BASE_URL, 
				SAWSDL2SADIServlet.SERVLET_NAME, 
				session.getAttribute("servicename")
			)
		);
		
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		fos.write(IOUtils.GenerateLiftingSchema(owlDatatypeMappings, writer.toString()).getBytes());
		fos.flush();
		fos.close();
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
		return false;
	    } catch (IOException e) {
		e.printStackTrace();
		return false;
	    }
	    
	    // save a mapping
	    if (!IOUtils.addSAWSDLService(
		    new File(serviceMappings), 
		    name, 
		    String.format("%s/%s.sawsdl", name, name), 
		    String.format("%s/%s.owl", name, name),
		    String.format("%s/%s", name, "lowering.xml"),
		    String.format("%s/%s", name, "lifting.xml"))) {
		// TODO remove main and all of its contents ....
		return false;
	    }
	}
	return true;
    }
    
    private String get_service_description(final HttpSession session) throws IOException {
	String base = BASE_URL;
	String url = String.format("%s%s/%s", base, SAWSDL2SADIServlet.SERVLET_NAME, (String)session.getAttribute("servicename"));
	
	String inputClass = url + "/owl#inputClass";
	String outputClass = url + "/owl#outputClass";
	
	Velocity.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH,TEMPLATE_DIR);
	Template template = Velocity.getTemplate("signature.vm");
	VelocityContext context = new VelocityContext();
	context.put("desc", (String)session.getAttribute("servicedescription"));
	context.put("name", (String)session.getAttribute("servicename"));
	context.put("authority", (String)session.getAttribute("serviceauthority"));
	context.put("authoritative", false);
	context.put("email", (String)session.getAttribute("serviceemail"));
	context.put("type", (String)session.getAttribute("servicetype"));
	context.put("input", inputClass);
	context.put("output", outputClass);
	context.put("uri", url);
	context.put("id", url);
	context.put("url", url);
	context.put("sigURL", url);
	// fill in the context with our values
	StringWriter writer = new StringWriter();
	template.merge(context, writer);
	return writer.toString();
    }
    
    public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-]{1,256}" +
        "\\@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
    );
    private boolean isValidEmailAddress(String addressString) {
        Matcher match = EMAIL_ADDRESS_PATTERN.matcher(addressString);
        if (match.find()) {
            return true;
        }
        return false;
    }
    
    private boolean isValidName(String name) throws Exception{
	final String serviceMappings = (String) getServletContext().getAttribute(ServletContextListener.MAPPING_FILE_LOCATION);
	
	for (SAWSDLService s : IOUtils.getSAWSDLServices(new File(serviceMappings))) {
	    if (name.trim().equals(s.getName())) {
		return false;
	    }
	}
	return true;
    }
    
    private boolean isValidAuthority(String auth) {
	if (auth != null) {
	    Matcher match = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-]{1,256}" +
		        "(" +
		            "\\." +
		            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,256}" +
		        ")+").matcher(auth);
	    if (match.find()) {
		return true;
	    }
	}
	return false;
    }
}
