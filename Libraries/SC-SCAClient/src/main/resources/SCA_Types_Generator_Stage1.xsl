<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="xml" version="1.0" encoding="utf-8"  media-type="text/xml" indent="no" />
    <xsl:template match="/">
 
<types>
    
    <xsl:for-each select="jel/jelclass[@type='SCASoap']/methods/method[@visibility='public']">
        <xsl:variable name="out" select="@type"/>
        <xsl:variable name="in" select="params/param/@type"/>
        
        <type><xsl:value-of select="$in"/></type>
        <type><xsl:value-of select="$out"/></type>
            
    </xsl:for-each>
        
</types>
 
    </xsl:template>
</xsl:stylesheet>