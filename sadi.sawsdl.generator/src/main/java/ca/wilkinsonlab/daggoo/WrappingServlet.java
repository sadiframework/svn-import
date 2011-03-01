package ca.wilkinsonlab.daggoo;

import ca.wilkinsonlab.daggoo.utils.DataRecorder;

import org.w3c.dom.*;

import java.io.PrintStream;
import java.net.URL;
import java.util.Vector;
import java.util.logging.*;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import javax.xml.transform.Transformer;

public abstract class WrappingServlet extends HttpServlet{

    public static final String DATARECORDER_CONTEXTPARAM = "dataRecorder"; // how users spec a DataRecorder in the web.xml
    public static final String SRC_PARAM = "srcSpec"; //used in input form creation
    public static final String ID_PARAM = "seahawkId"; //ditto
    public static final String SERVICE_SPEC_PARAM = "service"; //used in example form submission

    private static Logger logger = Logger.getLogger(WrappingServlet.class.getName());

    protected DataRecorder recorder = null;
    protected Transformer responseTransformer;

    /**
     * Registers an object that hooks into various parts of the request and 
     * response in order to provide custom behaviours.
     */
    public void setRecorder(DataRecorder r){
	recorder = r;
	if(responseTransformer != null){
	    recorder.setTransformer(responseTransformer);
	}
    }

    public DataRecorder getRecorder(){
	return recorder;
    }

    /**
     * Stylesheet to apply to service response if it's XML.
     */
    public void setResponseTransformer(Transformer t){
	responseTransformer = t;
	if(recorder != null){
	    recorder.setTransformer(responseTransformer);
	}
    }

    public Transformer getResponseTransformer(){
	return responseTransformer;
    }

    public void init() throws javax.servlet.ServletException{
	super.init();

	// See if the user has specified a DataRecorder to be used (customization and/or recording of
	// user interaction and Web Service response).
	String dataRecorderClassName = null;
	javax.servlet.ServletContext context = getServletContext();
	if(context != null){
	    if(context.getInitParameter(DATARECORDER_CONTEXTPARAM) != null){
		dataRecorderClassName = context.getInitParameter(DATARECORDER_CONTEXTPARAM);
	    }
	}
	javax.servlet.ServletConfig config = getServletConfig();
	if(config != null){
	    if(config.getInitParameter(DATARECORDER_CONTEXTPARAM) != null){
		dataRecorderClassName = config.getInitParameter(DATARECORDER_CONTEXTPARAM);
	    }
	}
	if(dataRecorderClassName != null){
	    DataRecorder dataRecorder = null;

	    try{
		// This line can throw many different exception if you didn't get the class right! 
		Class drClass = getClass().getClassLoader().loadClass(dataRecorderClassName);
		if(drClass == null){
		    throw new ClassNotFoundException("The DataRecorder class to run (" + 
						     dataRecorderClassName +
						     ") was not found, please ensure that the web.xml is up-to-date.");
		}
		dataRecorder = (DataRecorder) drClass.newInstance();
	    } catch(Exception e){
		System.err.println("The DataRecorder implementing class was not specified properly in the web.xml file:");
		e.printStackTrace();
		throw new ServletException("Invalid web.xml, the parameter 'dataRecorder' was not useable");
	    }
	    setRecorder(dataRecorder);
	}
    }

    /**
     * GETs are for form creation
     */
    public void doGet(HttpServletRequest request,
		      HttpServletResponse response)
	throws ServletException, java.io.IOException{

	if(request.getSession(false) == null && recorder != null){
	    recorder.startRecording(request); //should set up a session
	}

	String action = request.getParameter(DataRecorder.PASSTHROUGH_ACTION);
	if(action != null && action.trim().length() > 0){
	    // Delegate to the seahawk action recorder for wrapping
	    recorder.doGet(request, response);
	    return;
	}

	if(recorder != null && recorder.shouldIntercept(request)){
	    recorder.interceptRequest(request, response);
	}
	else{
	    
	    java.io.PrintStream out = null;
	    response.setContentType("text/html");
	    try{
		out = new java.io.PrintStream(response.getOutputStream());
	    }
	    catch(java.io.IOException ioe){
		logger.log(Level.SEVERE, "While getting servlet output stream (for HTML form response to client)", ioe);
		return;
	    }
	    
	    URL url = null;
	    String serviceSpecLoc = request.getParameter(SRC_PARAM);
	    if(serviceSpecLoc != null && serviceSpecLoc.trim().length() > 0){
		try{
		    url = new URL(serviceSpecLoc);
		} catch(Exception e){
		    out.print("<html><head><title>Parsing Error</title>\n"+
			      "<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/cgi_err.css\" />\n" +
			      "</head><body><h2>The URL specified (" + serviceSpecLoc + ") could not be parsed</h2><br><pre>");
		    e.printStackTrace(out);
		    out.print("</pre></body></html>\n");
		    return;
		}
	    }
	    writeServiceForm(request, response, url, out);
	}
    }

     /**
     * Post is for service submission (if the wsdl URL is provided), or the PBERecorder (any other case)
     */
    public void doPost(HttpServletRequest request,
		       HttpServletResponse response){

	java.io.PrintStream out = null;
	try{
	    out = new java.io.PrintStream(response.getOutputStream());
	}
	catch(java.io.IOException ioe){
	    logger.log(Level.SEVERE, "While getting servlet output stream (for HTML form response to client)", ioe);
	    return;
	}

	// Find out the info needed to build the JAX-WS client
	if(recorder != null && recorder.shouldIntercept(request)){
	    recorder.interceptRequest(request, response);
	    return;
	}

	URL url = null;
	String serviceSpecLoc = request.getParameter(SRC_PARAM);
	if(serviceSpecLoc != null && serviceSpecLoc.trim().length() != 0){
	    try{
		url = new URL(serviceSpecLoc);
	    } catch(Exception e){
		out.print("<html><head><title>Error</title>\n"+
			  "<link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/input_ask.css\" />\n"+
			  "</head><body><h2>The URL specified (" + 
			  serviceSpecLoc + ") could not be parsed</h2><br><pre>");
		e.printStackTrace(out);
		out.print("</pre></body></html>\n");
		return;
	    }
	}

	// Note, URL may be null e.g. if form was multipart encoded (the case for CGIServlet)
	callService(request, response, url, out);
    }

   /**
     * Method called when the user is requesting the fill-in form for the service.
     * Should create the CGI's HTML form interface.
     *
     * Should call the following methods [or ...AsDOM() equivalents] if a recorder was set:
     *
     * recorder.getHead(request)
     * recorder.getBody(request)
     * rec() for each form input to populate javascript actions
     * sub() on submit buttons for the same reason
     * 
     * @param request the GET request
     * @param response the response to contain the CGI form
     * @param endpoint the URL of the specification doc for the service to wrap (e.g. CGI form or WSDL doc)
     */
    protected abstract void writeServiceForm(HttpServletRequest request,
					     HttpServletResponse response,
					     URL serviceEndpointURL,
					     PrintStream out);

    /**
     * Should call the following methods if a recorder was set:
     *
     * recorder.setInputSource(request, source);
     * answer = recorder.markupResponse(resultSource, request);
     * 
     */
    protected abstract void callService(HttpServletRequest request,
					HttpServletResponse response,
					URL serviceDefinitionURL,
					PrintStream out);

    /**
     * Links the form inputs to javascript capture actions, if a recorder is available
     */
    protected String rec(){
	if(recorder != null){
	    return recorder.getOnEventText(); 
	}
	return "";
    }

    protected String sub(){
	if(recorder != null){
	    return recorder.getOnSubmitText(); 
	}
	return "";
    }

    /**
     * A mutable NodeList implementation for convenience.
     */
    public static class MyNodeList implements NodeList{
        private Vector<Node> nodes;
        public MyNodeList(){nodes = new Vector<Node>();}
        public int getLength(){return nodes.size();}
        public Node item(int index){return nodes.elementAt(index);}
        public void add(Node n){nodes.add(n);}
        public void add(NodeList n){for(int i=0;i<n.getLength();i++){nodes.add(n.item(i));}}
    };
}
