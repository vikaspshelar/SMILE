<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet exclude-result-prefixes="bpel" version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:bpel="http://docs.oasis-open.org/wsbpel/2.0/process/executable">
    <xsl:output method="xml" version="1.0" encoding="utf-8" omit-xml-declaration="yes" media-type="text/xml" />
    <xsl:template match="/">
        <process>
            <xsl:attribute name="name">
                <xsl:value-of select="bpel:process/@name"/>
            </xsl:attribute>
            <xsl:variable name="process" select="bpel:process/@name"/>
            <description>
                <xsl:value-of  select="bpel:process/bpel:documentation"/>
            </description>
            <xsl:for-each select="//bpel:invoke">
                <operation>
                    <xsl:attribute name="name">
                        <xsl:value-of select="@operation"/>
                    </xsl:attribute>
                    <xsl:attribute name="platform">
                        <xsl:value-of select="@partnerLink"/>
                    </xsl:attribute>
                    <xsl:attribute name="process">
                        <xsl:value-of select="$process"/>
                    </xsl:attribute>
                </operation>
            </xsl:for-each>
        </process>        
    </xsl:template>
</xsl:stylesheet>

