<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sadi" uri="/WEB-INF/sadi.tld" %>

<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
 "http://www.w3.org/TR/html4/strict.dtd">
<html>
  <head>
    <title>SADI registry</title>
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
            <li class="page_item current_page_item">Registry</li>
            <!--
            <li class="page_item page-item-5"><a href="http://sadiframework.org/content/about-sadi/" title="About SADI">About SADI</a></li>
            <li class="page_item page-item-6"><a href="http://sadiframework.org/content/how-sadi-works/" title="How SADI works">How SADI works</a></li>
            <li class="page_item page-item-7"><a href="http://sadiframework.org/content/show-me/" title="Show me">Show me</a></li>
            <li class="page_item page-item-8"><a href="http://sadiframework.org/content/getting-involved/" title="Getting involved">Getting involved</a></li>
            <li class="page_item page-item-9"><a href="http://sadiframework.org/content/links-and-docs/" title="Links and Docs">Links and Docs</a></li>
            <li class="page_item"><a href="http://sadiframework.org/content" title="News">News</a></li>
            -->
          </ul>
        </div>
        <div id='content'>
          <h2>SADI Registry</h2>
          
   <c:if test='${registered != null}'>
    <c:choose>
     <c:when test='${error != null}'>
      <div id='registration-error'>
        <h3>Error</h3>
        <p>There was an error registering the service at <a href='${registered.serviceURI}'>${registered.serviceURI}</a>:</p>
        <blockquote>${error}</blockquote>
      </div>
     </c:when>
     <c:otherwise>
       <div id='registration-success'>
         <h3>Success</h3>
         <p>Successfully registered the service at <a href='${registered.serviceURI}'>${registered.serviceURI}</a>.</p>
       </div>
     </c:otherwise>
    </c:choose>
   </c:if>

      <div id='registration-form'>
        <form method='POST' action='.'>
          <label id='url-label' for='url'>Enter the URL of the service you want to register...</label>
          <input id='url-input' type='text' name='url'>
          <input id='register-submit' type='submit' value='...and click here to register it'>
        </form>
      </div> <!-- registration-form -->
      
      <div id='services-table-div'>
          <table id='services-table'>
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
      </div> <!-- services-table-div -->
      
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
