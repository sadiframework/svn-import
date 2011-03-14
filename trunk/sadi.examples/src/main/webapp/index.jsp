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
          <!--
	      <ul>
	       <c:forEach var="service" items="${services}" varStatus="status">
	        <li><a href='${service.serviceURI}'><c:out value='${service.name}' default='${service.serviceURI}'/></a></li>
	       </c:forEach>
	      </ul>
	       -->
	    <ul>
          <li><a href="echo">An example service that simply returns a copy of its input.</a></li>
          <li><a href="hello">A simple "Hello, World" service that reads a name and attaches a greeting.</a></li>
          <li><a href="hello-param">A parameterized "Hello, World" service that reads a name and attaches a greeting in a variety of languages.</a></li>
          <li><a href="linear">Fits a least-squares regression line and returns results synchronously.</a></li>
          <li><a href="linear-async">Fits a least-squares regression line and returns results asynchronously.</a></li>
          <li><a href="uniprotInfo">Return basic information associated with a UniProt record.</a></li>
          <li><a href="uniprot2go">Returns GO terms associated with a UniProt record.</a></li>
          <li><a href="uniprot2pdb">Returns PDB ids associated with a UniProt record.</a></li>
          <li><a href="uniprot2pubmed">Returns PubMed ids associated with a UniProt record.</a></li>
          <li><a href="uniprot2prosite">Returns Prosite ids associated with a UniProt record.</a></li>
          <li><a href="uniprot2kegg">Returns Kegg gene ids associated with a UniProt record.</a></li>
          <li><a href="uniprot2omim">Returns OMIM ids associated with a UniProt record.</a></li>
          <li><a href="uniprot2EntrezGene">Returns Entrez Gene ids associated with a UniProt record.</a></li>
          <li><a href="blastUniprot">Issues a BLAST query against the UniProt database, using BLASTP, similarity matrix BLOSUM_62, and an expect threshold of 10. A maximum 500 BLAST hits are returned, if the expectation cutoff is not reached. All organisms are included in the search.  </a></li>
          <li><a href="blastUniprotById">Issues a BLAST query against the UniProt database, using the "canonical" sequence of the input UniProt ID. Uses BLASTP, similarity matrix BLOSUM_62, and an expect threshold of 10. A maximum 500 BLAST hits are returned, if the expectation cutoff is not reached. All organisms are included in the search.  </a></li>
          <li><a href="ermineJgo">ErmineJ</a></li>
          <li><a href="calculateBMI">Calculates Body mass Index.</a></li>
          <li><a href="keggPathway2Gene">Retrieves the KEGG genes that participate in the given KEGG pathway(s)</a></li>
          <li><a href="keggGene2Pathway">Retrieves the KEGG pathways that contain the given KEGG gene(s)</a></li>
          <li><a href="keggPathway2Compound">Retrieves the KEGG compounds that are involved in the given KEGG pathway(s)</a></li>
          <li><a href="keggCompound2PubChem">Maps KEGG compounds to PubChem substances</a></li>
          <li><a href="pdb2uniprot">Maps PDB structures to UniProt proteins</a></li>
          <li><a href="entrezGene2Uniprot">Maps Entrez Gene IDs to UniProt proteins</a></li>
          <li><a href="entrezGene2Kegg">Maps Entrez Gene IDs to KEGG gene IDs</a></li>
          <li><a href="keggGene2EntrezGene">Maps KEGG gene IDs to Entrez Gene IDs</a></li>
        </ul>
        <p>updated Mon 14 Mar 2011 11:22:48 PDT</p>
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