<xsl:stylesheet version = '1.0'
     xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
     xmlns:moby='http://mobyrdf/'
     xmlns:mobyobj='http://biomoby.org/RESOURCES/MOBY-S/Objects/'
     xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>

<xsl:variable name="uri_prefix">http://biordf.net/moby/</xsl:variable>

<!--
The root rule.  This is where the recursive application of templates
starts.
-->
<xsl:template match="/">
        <xsl:apply-templates/>
</xsl:template>

<xsl:template priority = "1.0" match="String|Integer|Float|Boolean|Object">
    <xsl:element name="moby:has{name(.)}">
    	<rdf:Description>
	    <xsl:if test="(string-length(@namespace) > 0) and (string-length(@id) > 0)">
	        <xsl:attribute name="rdf:about"><xsl:value-of select="$uri_prefix"/><xsl:value-of select="@namespace"/>/<xsl:value-of select="@id"/></xsl:attribute>
  	        <moby:hasNamespace><xsl:value-of select="@namespace"/></moby:hasNamespace>
	        <moby:hasID><xsl:value-of select="@id"/></moby:hasID>
	    </xsl:if>
	    <moby:hasArticleName><xsl:value-of select="@articleName"/></moby:hasArticleName>
	    <moby:hasContent><xsl:value-of select="."/></moby:hasContent>
        </rdf:Description>
    </xsl:element>
</xsl:template>

<!--
Matches the first level (document) tags.  There's 
one of these for every complete moby output.  Normally
there's only one of these in an XML document.
-->

<xsl:template match="/*">
    <rdf:Description>
	<xsl:if test="(string-length(@namespace) > 0) and (string-length(@id) > 0)">
	    <xsl:attribute name="rdf:about"><xsl:value-of select="$uri_prefix"/><xsl:value-of select="@namespace"/>/<xsl:value-of select="@id"/></xsl:attribute>
  	    <moby:hasNamespace><xsl:value-of select="@namespace"/></moby:hasNamespace>
	    <moby:hasID><xsl:value-of select="@id"/></moby:hasID>
	</xsl:if>
	<xsl:element name="rdf:type">
	       <xsl:attribute name="rdf:resource">http://biomoby.org/RESOURCES/MOBY-S/Objects/<xsl:value-of select="name(.)"/></xsl:attribute>
	</xsl:element>
	<moby:hasArticleName><xsl:value-of select="@articleName"/></moby:hasArticleName>
	<xsl:apply-templates/>
    </rdf:Description>
</xsl:template>
	

<!-- 
Matches any tag on the second level or below.  The "//"
means "any number of intermediate nodes". 
-->

<xsl:template match="/*//*">
    <moby:hasComposite>
	 <rdf:Description>
  	     <xsl:if test="(string-length(@namespace) > 0) and (string-length(@id) > 0)">
	        <xsl:attribute name="rdf:about"><xsl:value-of select="$uri_prefix"/><xsl:value-of select="@namespace"/>/<xsl:value-of select="@id"/></xsl:attribute>
  	        <moby:hasNamespace><xsl:value-of select="@namespace"/></moby:hasNamespace>
	        <moby:hasID><xsl:value-of select="@id"/></moby:hasID>
	        </xsl:if>
	     <xsl:element name="rdf:type">
	          <xsl:attribute name="rdf:resource">http://biomoby.org/RESOURCES/MOBY-S/Objects/<xsl:value-of select="name(.)"/></xsl:attribute>
	     </xsl:element>
	 
             <xsl:apply-templates/>
	 </rdf:Description>
    </moby:hasComposite>
</xsl:template>

</xsl:stylesheet>