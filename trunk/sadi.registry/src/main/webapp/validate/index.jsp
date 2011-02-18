<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sadi" uri="/WEB-INF/sadi.tld" %>
<%@ page import="org.apache.log4j.Logger" %>
<%@ page import="ca.wilkinsonlab.sadi.beans.*" %>
<%@ page import="ca.wilkinsonlab.sadi.registry.*" %>
<%@ page import="ca.wilkinsonlab.sadi.service.validation.*" %>
<%
	String serviceURI = request.getParameter("serviceURI");
	if (serviceURI != null) {
		Logger log = Logger.getLogger("ca.wilkinsonlab.sadi.registry");
		Registry registry = null;
		try {
			registry = Registry.getRegistry();
			ValidationResult result = ServiceValidator.validateService(serviceURI);
			request.setAttribute("service", result.getService());
			request.setAttribute("warnings", result.getWarnings());
		} catch (Exception e) {
			log.error(String.format("validation failed for %s: %s", serviceURI, e.getMessage()), e);
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
    <title>SADI registry &mdash; validate a service</title>
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
            <li class="page_item current_page_item"><a href="../validate">Validate</a></li>
            <li class="page_item"><a href="../register">Register</a></li>
            <li class="page_item"><a href="../services">Services</a></li>
            <li class="page_item"><a href="../sparql">SPARQL</a></li>
          </ul>
        </div>
        <div id='content'>
          <h2>Validate a service</h2>         
<c:if test='${service != null}'>
  <c:choose>
    <c:when test='${error != null}'>
	      <div id='registration-error'>
	        <h3>Error</h3>
	        <p>There was an error validating the service at <a href='${service.URI}'>${service.URI}</a>:</p>
	        <blockquote>${error}</blockquote>
	      </div>
    </c:when>
    <c:otherwise>
      <c:choose>
	    <c:when test='${not empty warnings}'>
	      <jsp:include page="warnings.jsp"/>
	    </c:when>
        <c:otherwise>
	      <div id='registration-success'>
	        <h3>Success</h3>
	        <p>Successfully validated the service at <a href='${service.URI}'>${service.URI}</a>.</p>
	        <jsp:include page="../service.jsp"/>
	      </div>
          <div id='validation-form'>
            <form method='POST' action='../register/'>
              <input type='hidden' name='serviceURI' value='${service.URI}'>
              <input type='hidden' name='ignoreWarnings' value='true'>
              <input type='submit' value="click here to register this service!">
            </form>
          </div>
	    </c:otherwise>
	  </c:choose>
    </c:otherwise>
  </c:choose>
</c:if>
          <div id='registration-form'>
            <form method='POST' action='.'>
              <label id='url-label' for='url-input'>Enter the URL of the service you want to validate...</label>
              <input id='url-input' type='text' name='serviceURI' value='<c:if test='${error != null or not empty warnings}'>${service.URI}</c:if>'>
              <input id='register-submit' type='submit' value='...and click here to validate it'>
            </form>
          </div> <!-- registration-form -->
        </div> <!-- content -->
        <div id='footer'>
          <img class="sponsor" style="margin-top: 10px;" src="../images/HSFBCY.gif" alt="HSFBCY logo" height="62" width="134"/>
          <img class="sponsor" style="margin-top: 16px;" src="../images/CIHR.png" alt="CIHR logo" height="62" width="91"/>
          <p>Development of SADI is generously supported by 
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
