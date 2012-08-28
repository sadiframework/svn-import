<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	request.setAttribute("taxons", new org.sadiframework.utils.blast.TaxonIterator());
%>
<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
  <head>
    <title>SADI BLAST services</title>
    <link rel="icon" href="http://sadiframework.org/favicon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="http://sadiframework.org/style/new.css">
  </head>
  <body>
    <div id='outer-frame'>
      <div id='inner-frame'>
        <div id='header'>
          <h1><a href="http://sadiframework.org/">SADI</a> BLAST services</h1>
        </div>
        <div id='nav'>
          <ul>
            <li class="page_item current_page_item">Services</li>
          </ul>
        </div>
        <div id='content'>
          <h2>SADI Services</h2>
	      <ul>
            <li><a href="./snapdragon">antirrhinum.net BLAST</a></li>
	       <c:forEach var="taxon" items="${taxons}">
	        <li><a href="./${taxon}">NCBI ${taxon} genome BLAST</a></li>
           </c:forEach>
	      </ul>
        </div> <!-- content -->
        <div id='footer'>
        </div> <!-- footer -->
      </div> <!-- inner-frame -->
    </div> <!-- outer-frame -->
  </body>
</html>