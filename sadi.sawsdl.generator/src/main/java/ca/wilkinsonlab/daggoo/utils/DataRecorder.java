package ca.wilkinsonlab.daggoo.utils;

/**
 * This interface allows implementing classes to intercept data
 * as it is proxied between the HTML interface user and the remote Web Service in 
 * ca.ucalgary.services.SoapServlet or the remote CGIs in
 * ca.ucalgary.services.CGIServlet
.*  This interception allows the DataRecorder
 * to save and/or modify the on-the-wire messages to the service.
 *
 * @author Paul Gordon
 */
public interface DataRecorder{

    /** If this variable is passed in a GET request to WrappingServlet, WrappingServlet 
	will forward the GET request as-is to the data recorder. */
    public static final String PASSTHROUGH_ACTION = "_dataAction"; 

    /**
     * Specify a XML transformation to be applied to the service results.
     * For example, SoapServlet tries to indent the results nicely.
     */
    public void setTransformer(javax.xml.transform.Transformer transformer);

    /**
     * Tells the data recorder that a new Web Service proxying has been requested.
     * The data recorder might want to set a cookie or something of the sort to track
     * further callbacks.
     *
     * @param initialRequest the GET request that specifies the WSDL to be proxied
     */
    public void startRecording(javax.servlet.http.HttpServletRequest initialRequest);

    /**
     * The WrappingServlet may delegate the whole request to the data recorder 
     * (if a parameter named after PASSTHROUGH_ACTION is in the GET request).  
     * This is the method called if that parameter exists.
     */
    public void doGet(javax.servlet.http.HttpServletRequest request, 
		      javax.servlet.http.HttpServletResponse response);

    /**
     * Lets the WrappingServlet know if the HTTP GET request should be handles by interceptRequest
     * or not.  This will generally be determined by special request parameters the
     * data recorder inserted into the request form.
     */
    public boolean shouldIntercept(javax.servlet.http.HttpServletRequest request);

    public void interceptRequest(javax.servlet.http.HttpServletRequest request,
				 javax.servlet.http.HttpServletResponse response);

    public String writeWrapperForm(javax.servlet.http.HttpServletRequest request);

    /**
     * Lets the data recorder know what the input XML to the Web Service looked like.
     */
    public void setInputSource(javax.servlet.http.HttpServletRequest submissionRequest, 
			       SourceMap source);

    /**
     * Lets the data recorder know what the input XML to the Web Service looked like.
     */
    public void setInputParams(javax.servlet.http.HttpServletRequest submissionRequest, 
			       java.util.Map<String,byte[]> httpParams,
			       java.util.List<String> hiddenParams);

    /**
     * Gives the data recorder an opportunity to record or modify the Web Service response
     * before WrappingServlet returns it to the HTTP client.
     *
     * @return the XML serialization of the resultSource, potentially modified
     */
    public String markupResponse(javax.xml.transform.Source resultSource, 
				 javax.servlet.http.HttpServletRequest submissionRequest) throws Exception;

    /**
     * Gives the data recorder an opportunity to record or modify the CGI response
     * before WrappingServlet returns it to the HTTP client.
     *
     * @return responseBody, potentially modified
     */
    public byte[] markupResponse(byte[] responseBody, String contentType, String charSetEncoding,
				 javax.servlet.http.HttpServletRequest submissionRequest) throws Exception;

    
    /**
     * Gives the data recorder an opportunity to insert extra HTML into the head element of the HTML
     * the WrappingServlet generates for the Web Service's CGI form.
     *
     * @return recorder-specific HTML code for the CGI form header
     */
    public String getHead(javax.servlet.http.HttpServletRequest formRequest);

    /**
     * Gives the data recorder an opportunity to insert extra HTML into the head element of the HTML
     * the WrappingServlet generates for the Web Service's CGI form.
     *
     * @param owner the Document that should be used to create the DOM elements returned
     *
     * @return recorder-specific HTML code for the CGI form header
     */
    public org.w3c.dom.Node[] getHeadAsDOM(javax.servlet.http.HttpServletRequest formRequest, 
					   org.w3c.dom.Document owner);

    /**
     * Gives the data recorder an opportunity to insert extra HTML element attributes 
     * into the body element the WrappingServlet generates for the Web Service's CGI form.
     *
     * @return recorder-specific HTML body-tag attributes for the CGI form header
     */
    public String getBodyAttrs(javax.servlet.http.HttpServletRequest formRequest);

    public org.w3c.dom.Attr[] getBodyAttrsAsDOM(javax.servlet.http.HttpServletRequest formRequest, 
						org.w3c.dom.Document owner);

    /**
     * Gives the data recorder an opportunity to insert extra HTML elements
     * into the body (at the start) of the WrappingServlet generates for the user-facing CGI form.
     *
     * @return recorder-specific HTML body-tag attributes for the CGI form header
     */
    public String getBody(javax.servlet.http.HttpServletRequest formRequest);
    public org.w3c.dom.Node[] getBodyAsDOM(javax.servlet.http.HttpServletRequest formRequest, 
					   org.w3c.dom.Document owner);

    /**
     * Gives the data recorder an opportunity to insert a javascript event handler
     * into every form input element the WrappingServlet generates for the Web Service's CGI form.
     *
     * @return recorder-specific CGI form attributes for input elements
     */
    public String getOnEventText();
    public org.w3c.dom.Attr getOnEventAsDOM(org.w3c.dom.Document owner);

    /**
     * Gives the data recorder an opportunity to insert a javascript event handler
     * into the form submission tags the WrappingServlet generates for the Web Service's CGI form.
     *
     * @return recorder-specific CGI form attributes for form submission tags
     */
    public String getOnSubmitText();
    public org.w3c.dom.Attr getOnSubmitAsDOM(org.w3c.dom.Document owner);

    /**
     * Allows a WrappingServlet to set the equivalent of HTTP request parameters (which are normally immutable).
     * Useful in case salient parameters such as WrappingServlet.SERVICE_SPEC_PARAM or WrappingServlet.SRC_PARAM
     * are passed around by aletrnate means (e.g. multipart encoded form like in CGIServlet)
     */
    public void setParameter(javax.servlet.http.HttpSession session, String paramName, String paramValue);
}
