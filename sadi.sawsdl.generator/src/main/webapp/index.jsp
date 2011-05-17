<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	pageContext.setAttribute("services", application.getAttribute("sawsdl-service-map"));
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
  <head>
    <title>SADI &#8212; Find. Integrate. Analyze.</title>
    <link rel="icon" href="http://sadiframework.org/favicon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="http://sadiframework.org/content/wp-content/themes/SADI/style.css">
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
            <li class="page_item"><a href="WSDL2SAWSDL">DAGGOO</a></li>
            <li class="page_item current_page_item">SAWSDL services</li>
          </ul>
        </div>
        <div id='content'>
          <h2>SAWSDL Services</h2>
	      <ul>
	       <c:forEach var="entry" items="${services}" varStatus="status">
	        <li><a href='${entry.key}'><c:out value='${entry.key}'/></a></li>
	       </c:forEach>
	      </ul>
	      <p>Or you can <a href="WSDL2SAWSDL">add a new service</a>.</p>
        </div> <!-- content -->
        <div id='footer'>
        <p style="margin-top: 10px;">
          <img class="sponsor" src="http://sadiframework.org/style/2011-03-14/images/HSFBCY.gif" alt="HSFBCY logo" height="62" width="134"/>
          <img class="sponsor" src="http://sadiframework.org/style/2011-03-14/images/CANARIE.png" alt="CANARIE logo" height="62" width="242"/>
          <img class="sponsor" src="http://sadiframework.org/style/2011-03-14/images/CIHR.png" alt="CIHR logo" height="62" width="91"/>
        </p>
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
          <img class="sponsor" src="http://sadiframework.org/style/2011-03-14/images/GenomeCanada.png" alt="Genome Canada logo" height="116" width="191"/>
          <img class="sponsor" src="http://sadiframework.org/style/2011-03-14/images/GenomeAlberta.png" alt="Genome Alberta logo" height="116" width="185"/>
        </p>
      </div> <!-- footer -->
      </div> <!-- inner-frame -->
    </div> <!-- outer-frame -->
  </body>
</html>