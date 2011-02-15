<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sadi" uri="/WEB-INF/sadi.tld" %>

<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
 "http://www.w3.org/TR/html4/strict.dtd">
<html>
  <head>
    <title>SADI registry &mdash; administration</title>
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
            <!-- <li class="page_item"><a href="../validate">Validate</a></li> -->
            <li class="page_item"><a href="../register">Register</a></li>
            <li class="page_item"><a href="../services">Services</a></li>
            <li class="page_item current_page_item"><a href="../sparql">SPARQL</a></li>
          </ul>
        </div>
        <div id='content'>
          <h2>Administration</h2>
          
	   <c:if test='${error != null}'>
          <div id='registration-error'>
            <h3>Error</h3>
            <p>There was an error:</p>
            <blockquote>${error}</blockquote>
          </div>
	   </c:if>

	   <c:if test='${authorizationURL != null}'>
	      <div id='admin-form'>
			<p><a href='${authorizationURL}'>Click here</a> and enter the pin in the box below.</p>
            <form method='POST' action='../admin/'>
              <input id='pin-input' type='text' name='pin'>
              <input id='pin-submit' type='submit' value='Go'>
            </form>
          </div> <!-- sparql-form -->
          </div>
	   </c:if>
          
       <c:if test='${accessToken != null}'>
          <p>Twitter accessToken is <pre>${accessToken.token}</pre>.</p>
       </c:if>
      
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
