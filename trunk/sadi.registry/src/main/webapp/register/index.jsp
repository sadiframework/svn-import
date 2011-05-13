<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sadi" uri="/WEB-INF/sadi.tld" %>
<%@ page import="org.apache.log4j.Logger" %>
<%@ page import="ca.wilkinsonlab.sadi.SADIException" %>
<%@ page import="ca.wilkinsonlab.sadi.beans.ServiceBean" %>
<%@ page import="ca.wilkinsonlab.sadi.client.ServiceConnectionException" %>
<%@ page import="ca.wilkinsonlab.sadi.client.ServiceImpl" %>
<%@ page import="ca.wilkinsonlab.sadi.registry.*" %>
<%@ page import="ca.wilkinsonlab.sadi.registry.utils.Twitter" %>
<%@ page import="ca.wilkinsonlab.sadi.service.validation.*" %>
<%
	boolean doValidate = request.getParameter("ignoreWarnings") == null;
	boolean doRegister = true;
	boolean doTweet = Registry.getConfig().getBoolean("twitter.sendTweets", false);
	
	String serviceURI = request.getParameter("serviceURI");
	if (serviceURI != null) {
		Logger log = Logger.getLogger("ca.wilkinsonlab.sadi.registry");
		Registry registry = null;
		try {
			registry = Registry.getRegistry();
			
			ServiceImpl service = null;
			try {
				service = new ServiceImpl(serviceURI);
			} catch (ServiceConnectionException e) {
				if (registry.containsService(serviceURI)) {
					doValidate = false;
					doRegister = false;
					registry.unregisterService(serviceURI);
					
					ServiceBean serviceBean = new ServiceBean();
					serviceBean.setURI(serviceURI);
					request.setAttribute("service", serviceBean);
					request.setAttribute("unregister", true);
				} else {
					throw e;
				}
			} // other exceptions thrown to outer...
			
			
			if (doValidate) {
				// TODO replace with validateService(service) once we update the API...
				ValidationResult result = ServiceValidator.validateService(service.getServiceModel().getResource(serviceURI));
				request.setAttribute("service", result.getService());
				request.setAttribute("warnings", result.getWarnings());
				if (!result.getWarnings().isEmpty()) {
					doRegister = false;
				}
			}
			
			if (doRegister) {
				doTweet &= !registry.containsService(serviceURI); // only tweet new services
				ServiceBean serviceBean = registry.registerService(serviceURI);
				request.setAttribute("service", serviceBean);
				if (doTweet) {
					try {
						Twitter.tweetService(serviceBean);
					} catch (final Exception e) {
						log.error(String.format("error tweeting registration of %s: %s", serviceURI, e));
					}
				}
			}
		} catch (Exception e) {
			log.error(String.format("registration failed for %s: %s", serviceURI, e.getMessage()), e);
			ServiceBean service = new ServiceBean();
			service.setURI(serviceURI);
			request.setAttribute("service", service);
			request.setAttribute("error", e.getMessage() != null ? e.getMessage() : e.toString());
		} finally {
			if (registry != null)
				registry.getModel().close();
		}
	}
%>
<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
 "http://www.w3.org/TR/html4/strict.dtd">
<html>
  <head>
    <title>SADI registry &mdash; register a service</title>
    <link rel="icon" href="/favicon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="../style/sadi.css">
  </head>
  <body>
    <div id='outer-frame'>
      <div id='inner-frame'>
        <div id='header'>
          <h1><a href="http://sadiframework.org/">SADI</a></h1>
          <p class='tagline'>Find. Integrate. Analyze.</p>
        </div>
        <div id='nav'>
          <ul>
            <li class="page_item"><a href="../validate">Validate</a></li>
            <li class="page_item current_page_item"><a href="../register">Register</a></li>
            <li class="page_item"><a href="../services">Services</a></li>
            <li class="page_item"><a href="../sparql">SPARQL</a></li>
          </ul>
        </div>
        <div id='content'>
          <h2>Register a service</h2>
<c:if test='${service != null}'>
  <c:choose>
    <c:when test='${error != null}'>
	      <div id='registration-error'>
	        <h3>Error</h3>
	        <p>There was an error registering the service at <a href='${service.URI}'>${service.URI}</a>:</p>
	        <blockquote>${error}</blockquote>
	      </div>
    </c:when>
	<c:otherwise>
	  <c:choose>   
	    <c:when test='${warnings != null and not empty warnings}'>
	      <jsp:include page="../validate/warnings.jsp"/>
	      <div id='validation-form'>
	        <form method='POST' action='.'>
	          <input type='hidden' name='serviceURI' value='${service.URI}'>
	          <input type='hidden' name='ignoreWarnings' value='true'>
	          <input type='submit' value="click here to ignore the warnings and register anyway">
	        </form>
	      </div> 
	    </c:when>
	    <c:when test='${unregister == true}'>
	      <div id='registration-success'>
	        <h3>Success</h3>
	        <p>Successfully unregistered the service at <a href='${service.URI}'>${service.URI}</a>.</p>
	      </div>
    	</c:when>
	    <c:otherwise>
	      <div id='registration-success'>
	        <h3>Success</h3>
	        <p>Successfully registered the service at <a href='${service.URI}'>${service.URI}</a>.</p>
	        <jsp:include page="../service.jsp"/>
	      </div>
	    </c:otherwise>
	  </c:choose>
	</c:otherwise>
  </c:choose>
</c:if>
	      <div id='registration-form'>
	        <form method='POST' action='.'>
	          <label id='url-label' for='url-input'>Enter the URL of the service you want to register...</label>
	          <input id='url-input' type='text' name='serviceURI' value='<c:if test='${error != null or not empty warnings}'>${service.URI}</c:if>'>
	          <input id='register-submit' type='submit' value='...and click here to register it'>
	        </form>
	      </div> <!-- registration-form -->      
        </div> <!-- content -->
        <div id='footer'>
          <img class="sponsor" style="margin-top: 10px;" src="../images/HSFBCY.gif" alt="HSFBCY logo" height="62" width="134"/>
          <img class="sponsor" style="margin-top: 10px;" src="../images/CANARIE.png" alt="CANARIE logo" height="62" width="242"/>
          <img class="sponsor" style="margin-top: 16px;" src="../images/CIHR.png" alt="CIHR logo" height="62" width="91"/>
          <p>Development of SADI is generously supported by 
            <span class="nobreak">CANARIE</span>,
            <span class="nobreak">the Heart and Stroke Foundation of B.C. and Yukon</span>,
            <span class="nobreak">the Canadian Institutes of Health Research</span>, and 
            <span class="nobreak">Microsoft Research</span>.
          </p>
          <p>Major funding for the 
            <span class="nobreak"><a href="http://gcbioinformatics.ca">Bioinformatics Innovation Center</a></span>
            is provided by the
            <span class="nobreak">Government of Canada</span> through
            <span class="nobreak">Genome Canada</span> and
            <span class="nobreak">Genome Alberta</span>.
          </p>
          <p style="margin-top: 20px;">
            <img class="sponsor" src="../images/GenomeCanada.png" alt="Genome Canada logo" height="116" width="191"/>
            <img class="sponsor" src="../images/GenomeAlberta.png" alt="Genome Alberta logo" height="116" width="185"/>
          </p>
        </div> <!-- footer -->
      </div> <!-- inner-frame -->
    </div> <!-- outer-frame -->
  </body>
</html>
