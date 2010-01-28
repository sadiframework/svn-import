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
            <li class="page_item current_page_item">Examples</li>
          </ul>
        </div>
        <div id='content'>
          <h2>Example Services</h2>
	      <ul>
	       <!--<c:forEach var="service" items="${services}" varStatus="status">
	        <li><a href='${service.serviceURI}'><c:out value='${service.name}' default='${service.serviceURI}'/></a></li>
	       </c:forEach>
	       -->
	      </ul>
	      <ul>
	      	<li><a href="echo">Simply echo input</a></li>
	      	<li><a href="linear">Compute linear regression model (synchronous)</a></li>
	      	<li><a href="linear-async">Compute linear regression model (asynchronous)</a></li>
	      	<li><a href="uniprotInfo">List name, description and organism for a given UniProt ID</a></li>
	      	<li><a href="uniprot2go">Find GO terms associated with a given UniProt ID</a></li>
	      	<li><a href="uniprot2pdb">Find PDB entries corresponding to a given UniProt ID</a></li>
	      	<li><a href="uniprot2pubmed">Find published papers associated with a given UniProt ID</a></li>
	      </ul>
        </div> <!-- content -->
        <div id='footer'>
          <img class="sponsor" style="margin-top: 10px;" src="http://sadiframework.org/images/HSFBCY.gif" alt="HSFBCY logo" height="62" width="134"/>
          <img class="sponsor" style="margin-top: 16px;" src="http://sadiframework.org/images/CIHR.png" alt="CIHR logo" height="62" width="91"/>
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