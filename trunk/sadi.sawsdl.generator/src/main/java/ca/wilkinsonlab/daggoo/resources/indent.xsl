<?xml version="1.0"?>
<!-- Replaces any existing formatting in an XML document with 
     a standard indentation. -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" indent="yes" xmlns:xalan="http://xml.apache.org/xalan" xalan:indent-amount="3"/>
<xsl:strip-space elements="*"/>
<xsl:template match="/">
  <xsl:copy-of select="."/>
</xsl:template>
</xsl:stylesheet>
