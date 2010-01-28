<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sadi" uri="/WEB-INF/sadi.tld" %>

<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
 "http://www.w3.org/TR/html4/strict.dtd">
<html>
  <head>
    <title>SADI registry &mdash; SPARQL query</title>
    <link rel="icon" href="favicon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="style/sadi.css">
    <script type='text/javascript' src='http://www.google.com/jsapi'></script>
    <script type='text/javascript' src='js/services.js'></script>
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
            <li class="page_item"><a href=".">Services</a></li>
            <li class="page_item current_page_item"><a href="sparql.jsp">SPARQL</a></li>
          </ul>
        </div>
        <div id='content'>
          <h2>SPARQL query</h2>
          
          <c:if test='${error != null}'>
            <div id='registration-error'>
              <h3>Error</h3>
              <p>There was an error executing your query:</p>
              <blockquote>${error}</blockquote>
            </div>
          </c:if>

          <div id='sparql-form'>
            <form method='POST' action=''>
              <label id='sparql-label' for='sparql-input'>Enter a SPARQL query in the box below</label>
              <textarea id='sparql-input' type='text' name='query'><c:if test='${error != null}'>value='${param.query}'</c:if></textarea>
              <label id='canned-label' for='canned-select'>Or pick one from this menu</label>
              <select id='canned-select' name='query'>
              </select>
              <input id='sparql-submit' type='submit' value='Go'>
            </form>
          </div> <!-- sparql-form -->
      
      <!-- 
          <div id='results-table-div'>
            <table id='results-table'>
              <caption>Registered Services</caption>
              <thead>
                <tr>
                  <th>Service URL</th>
                  <th>Input Class</th>
                  <th>Output Class</th>
                </tr>
              </thead>
              <tbody>
               <c:forEach var="service" items="${services}" varStatus="status">
                <tr id='${service.serviceURI}' class='${status.index % 2 == 0 ? "even" : "odd"}'>
                  <td><a href='${service.serviceURI}' title='${service.serviceURI}'>${service.serviceURI}</a></td>
                  <td><a href='${service.inputClassURI}' title='${service.inputClassURI}'><sadi:localName uri="${service.inputClassURI}" withNamespace="false"/></a></td>
                  <td><a href='${service.outputClassURI}' title='${service.outputClassURI}'><sadi:localName uri="${service.outputClassURI}" withNamespace="false"/></a></td>
                </tr>
               </c:forEach>
              </tbody>
            </table>
          </div> <!-- results-table-div -->
      
        </div> <!-- content -->
        <div id='footer'>
          <img class="sponsor" style="margin-top: 10px;" src="images/HSFBCY.gif" alt="HSFBCY logo" height="62" width="134"/>
          <img class="sponsor" style="margin-top: 16px;" src="images/CIHR.png" alt="CIHR logo" height="62" width="91"/>
          <p>Development of SADI is generously supported by 
            <span class="nobreak">the Heart and Stroke Foundation of B.C. and Yukon</span>,
            <span class="nobreak">the Canadian Institutes of Health Research</span>, and 
            <span class="nobreak">Microsoft Research</span>.
          </p>
        </div> <!-- footer -->
      </div> <!-- inner-frame -->
    </div> <!-- outer-frame -->
  </body>
</html>
