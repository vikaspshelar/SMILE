<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
    <xsl:output method="xml" version="1.0" encoding="utf-8"  media-type="text/xml" />
    <xsl:template match="/">
        <operations>
            
            <xsl:for-each select="//operation[@name!='GetEndPointReference']">
                <xsl:sort select="@name"/>
                <operation>
                        <name>
                            <xsl:value-of select="@name"/>
                        </name>
                        <platform>
                            <xsl:value-of select="@platform"/>
                        </platform>
                        <process>
                            <xsl:value-of select="@process"/>
                        </process>
                </operation>
            </xsl:for-each>
            
        </operations>        
    </xsl:template>
</xsl:stylesheet>

