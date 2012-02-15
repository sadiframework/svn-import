<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Simple SADI client</title>
  <link rel="stylesheet" href="http://sadiframework.org/style/2011-03-14/style.css" type="text/css" media="screen" />
  <link rel="stylesheet" href="style/style.css" type="text/css" media="screen" />
</head>
<body>
  <div id='outer-frame'>
    <div id='inner-frame'>
      <div id='header'>
        <h1>SADI</h1>
        <p class='tagline'>Find. Integrate. Analyze.</p>
      </div>
      <div id='nav'></div>
      <div id='content'>
        <h2>Simple SADI client</h2>
        <form id='client-form' action='get-proxy'>
          <label id='serviceURL-label' for='serviceURL'>Service URL:</label>
          <input id='serviceURL' type='text' name='serviceURL'/>
          <label id='input-label' for='input'>Input RDF:</label>
          <textarea id='input' name='input' cols='80' rows='25'></textarea>
          <label id='inputURL-label' for='inputURL'>or input RDF URL:</label>
          <input id='inputURL' type='text' name='inputURL'/>
          <label id='format-label' for='format'>Serialization content-type:</label>
          <fieldset id='format'>
            <input id='xml' type="radio" name="format" value="application/rdf+xml" checked="checked"/>
            <label for='xml'>RDF/XML</label>
            <input id='n3' type="radio" name="format" value="text/rdf+n3"/>
            <label for='n3'>N3</label>
          </fieldset>
          <input id='submit' type='submit' value='Call service'/>
        </form>
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
        <!--
        <p>Major funding for the 
          <span class="nobreak"><a href="http://gcbioinformatics.ca">Bioinformatics Innovation Center</a></span>
          is provided by the
          <span class="nobreak">Government of Canada</span> through
          <span class="nobreak">Genome Canada</span> and
          <span class="nobreak">Genome Alberta</span>.
        </p>
        <p style="margin-top: 20px;">
          <img class="sponsor" src="<?php get_theme_root_uri() ?><?php echo get_template_directory_uri() ?>/images/GenomeCanada.png" alt="Genome Canada logo" height="116" width="191"/>
          <img class="sponsor" src="<?php get_theme_root_uri() ?><?php echo get_template_directory_uri() ?>/images/GenomeAlberta.png" alt="Genome Alberta logo" height="116" width="185"/>
        </p>
        -->
      </div> <!-- footer -->
    </div> <!-- inner-frame -->
  </div> <!-- outer-frame -->
</body>
</html>