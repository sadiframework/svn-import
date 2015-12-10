<?xml version="1.0" encoding="windows-1252"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#">
  <!ENTITY rdfs "http://www.w3.org/2000/01/rdf-schema#">
  <!ENTITY owl "http://www.w3.org/2002/07/owl#">
  <!ENTITY mygrid "http://www.mygrid.org.uk/mygrid-moby-service#">
  <!ENTITY mygrid_dc "http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#">
  <!ENTITY raquo "&#187;">
]>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:mygrid="http://www.mygrid.org.uk/mygrid-moby-service#" xmlns:mygrid_dc="http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#">

<xsl:template match="mygrid:serviceDescription">

<html>
  <head>
    <title>SADI service » <xsl:value-of select="mygrid:hasServiceNameText"/></title>
    <link rel="stylesheet" type="text/css" href="http://sadiframework.org/style/2011-03-14/style.css"/>
  </head>
  <body>
    <div id="outer-frame">
      <div id="inner-frame">
        <div id="header">
          <h1>SADI</h1>
          <p class="tagline">Find. Integrate. Analyze.</p>
        </div>
        <div id="nav">
          <ul>
            <li class="page_item"><a href="http://sadiframework.org/" title="About SADI">About SADI</a></li>
            <li class="current_page_item">
              <xsl:element name="a">
                <xsl:attribute name="href">
                  <xsl:value-of select="@rdf:about"/>
                </xsl:attribute>
                <xsl:value-of select="mygrid:hasServiceNameText"/>
              </xsl:element>
            </li>
          </ul>
        </div>
        <div id="content">
          <h2 class="service-name">
            <xsl:element name="a">
              <xsl:attribute name="href">
                <xsl:value-of select="@rdf:about"/>
              </xsl:attribute>
              <xsl:value-of select="mygrid:hasServiceNameText"/>
            </xsl:element>
          </h2>
          <p class="service-description"><xsl:value-of select="mygrid:hasServiceDescriptionText"/></p>
          <p class="service-io">
            This service consumes instances of 
            <xsl:element name="a">
              <xsl:attribute name="href">
                <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:inputParameter/mygrid:parameter/mygrid:objectType/@rdf:resource"/>
              </xsl:attribute>
              <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:inputParameter/mygrid:parameter/mygrid:objectType/@rdf:resource"/>
            </xsl:element>
            and produces instances of
            <xsl:element name="a">
              <xsl:attribute name="href">
                <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:outputParameter/mygrid:parameter/mygrid:objectType/@rdf:resource"/>
              </xsl:attribute>
              <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:outputParameter/mygrid:parameter/mygrid:objectType/@rdf:resource"/>
            </xsl:element>.
            <xsl:choose>
              <xsl:when test="not(mygrid:hasOperation/mygrid:operation/mygrid:inputParameter/mygrid:secondaryParameter/mygrid:objectType/@rdf:resource)">
                It has no secondary parameters.
              </xsl:when>
              <xsl:otherwise>
                It can optionally consume an instance of
                <xsl:element name="a">
                  <xsl:attribute name="href">
                    <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:inputParameter/mygrid:secondaryParameter/mygrid:objectType/@rdf:resource"/>
                  </xsl:attribute>
                    <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:inputParameter/mygrid:secondaryParameter/mygrid:objectType/@rdf:resource"/>
                </xsl:element>
                to set secondary parameters.
              </xsl:otherwise>
            </xsl:choose>
          </p>
          <p class="service-authoritative">
            This service <xsl:apply-templates select="mygrid:providedBy/mygrid:organisation/mygrid:authoritative"/>.
          </p>
          <p class="service-contact">
            Contact
            <xsl:choose>
              <xsl:when test="not(mygrid:providedBy/mygrid:organisation/mygrid_dc:creator)">
                <span class="service-missing-info">no email address</span>
              </xsl:when>
              <xsl:otherwise>
                <xsl:element name="a">
                  <xsl:attribute name="href">
                    mailto:<xsl:value-of select="mygrid:providedBy/mygrid:organisation/mygrid_dc:creator"/>
                  </xsl:attribute>
                  <xsl:value-of select="mygrid:providedBy/mygrid:organisation/mygrid_dc:creator"/>
                </xsl:element>
              </xsl:otherwise>
            </xsl:choose>
            with any questions about this service.
          </p>
          <!-- TODO; need to POST this raw and probably use jQuery to do it...
          <form method="POST" action="." class='sadi-service-input'>
            <label for="rdf">Type or paste RDF below...</label>
            <textarea id="rdf"></textarea>
            <input type='submit' value='...and click here to submit it to this service'/>
          </form>
           -->
          <table class="service-definition">
           <tbody>
            <tr>
              <th>name</th>
              <td><xsl:value-of select="mygrid:hasServiceNameText"/></td>
            </tr>
            <tr>
              <th>description</th>
              <td><xsl:value-of select="mygrid:hasServiceDescriptionText"/></td>
            </tr>
            <tr>
              <th>authoritative</th>
              <td><xsl:value-of select="mygrid:providedBy/mygrid:organisation/mygrid:authoritative"/></td>
            </tr>
            <tr>
              <th>contact email</th>
              <td>
                <xsl:element name="a">
                  <xsl:attribute name="href">
                    mailto:<xsl:value-of select="mygrid:providedBy/mygrid:organisation/mygrid_dc:creator"/>
                  </xsl:attribute>
                  <xsl:value-of select="mygrid:providedBy/mygrid:organisation/mygrid_dc:creator"/>
                </xsl:element>
              </td>
            </tr>
            <tr>
              <th>input class</th>
              <td>
                <xsl:element name="a">
                  <xsl:attribute name="href">
                    <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:inputParameter/mygrid:parameter/mygrid:objectType/@rdf:resource"/>
                  </xsl:attribute>
                  <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:inputParameter/mygrid:parameter/mygrid:objectType/@rdf:resource"/>
                </xsl:element>
              </td>
            </tr>
            <tr>
              <th>output class</th>
              <td>
                <xsl:element name="a">
                 <xsl:attribute name="href">
                   <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:outputParameter/mygrid:parameter/mygrid:objectType/@rdf:resource"/>
                 </xsl:attribute>
                 <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:outputParameter/mygrid:parameter/mygrid:objectType/@rdf:resource"/>
               </xsl:element>
              </td>
            </tr>
            <tr>
              <th>parameter class</th>
              <td>
                <xsl:element name="a">
                  <xsl:attribute name="href">
                    <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:inputParameter/mygrid:secondaryParameter/mygrid:objectType/@rdf:resource"/>
                  </xsl:attribute>
                    <xsl:value-of select="mygrid:hasOperation/mygrid:operation/mygrid:inputParameter/mygrid:secondaryParameter/mygrid:objectType/@rdf:resource"/>
                </xsl:element>
              </td>
            </tr>
           </tbody>
          </table>
        </div> <!-- content -->
        <div id="footer">
        </div> <!-- footer -->
      </div> <!-- inner-frame -->
    </div> <!-- outer-frame -->
</body>
</html>

</xsl:template>

<xsl:template match="mygrid:authoritative">
  <xsl:choose>
    <xsl:when test=".='true'">is authoritative</xsl:when>
    <xsl:when test=".='false'">is not authoritative</xsl:when>
    <xsl:otherwise>is probably not authoritative</xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="text()|@*">
</xsl:template>

</xsl:stylesheet>