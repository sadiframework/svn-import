<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sadi" uri="/WEB-INF/sadi.tld" %>

<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
 "http://www.w3.org/TR/html4/strict.dtd">
<html>
  <head>
    <title>SADI registry &mdash; SPARQL query</title>
    <link rel="icon" href="/favicon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="../style/sadi.css">
    <script type='text/javascript' src='http://www.google.com/jsapi'></script>
    <script type='text/javascript' src='sparql.js'></script>
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
          <h2>SPARQL query</h2>
          
	   <c:if test='${error != null}'>
          <div id='registration-error'>
            <h3>Error</h3>
            <p>There was an error executing the query:</p>
            <blockquote>${error}</blockquote>
          </div>
	   </c:if>

	      <div id='sparql-form'>
            <form method='POST' action='../sparql/'>
              <label id='sparql-label' for='sparql-input'>Enter a SPARQL query in the box below</label>
              <textarea id='sparql-input' type='text' name='query'>${param.query}</textarea>
              <!-- 
              <label id='canned-label' for='canned-select'>Or pick one from this menu</label>
              <select id='canned-select' name='query'>
              </select>
               -->
              <input id='sparql-submit' type='submit' value='Go'>
            </form>
          </div> <!-- sparql-form -->
          
       <c:if test='${variables != null}'>
          <div id='sparql-results'>
        <c:choose>
         <c:when test='${empty bindings}'>
             <p>No results.</p>
         </c:when>
         <c:otherwise>
            <table id='sparql-results-table'>
             <thead>
              <tr>
          <c:forEach var='variable' items='${variables}'>
                <th>${variable}</th>
          </c:forEach>
              </tr>
             </thead>
             <tbody>
          <c:forEach var='binding' items='${bindings}' varStatus='status'>
              <tr class='${status.index % 2 == 0 ? "even" : "odd"}'>
           <c:forEach var='variable' items='${variables}'>
                <td>${binding[variable]}</td>
           </c:forEach> 
              </tr>
             </tbody>
          </c:forEach>
            </table>
         </c:otherwise>
        </c:choose> 
          </div>
       </c:if>
      
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
