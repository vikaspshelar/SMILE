<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
    <xsl:output method="html" encoding="utf-8" omit-xml-declaration="yes" media-type="text/html" />
    <xsl:template match="/">   
    
    

<html>
<head>
<style type="text/css">

h4 {
	font-size: 80%;
	font-style: italic;
}

.label {
	font-weight: bold;
	padding-bottom: .5em;
	color: darkblue;
	margin: 5px 0px 0px 0px;
}

.value {	
	color: darkblue;
	padding-bottom: .5em;
}

.table {
	border-bottom: 3px dotted navy;
	color: darkblue;
	margin: 0px 0px 0px 0px;
}


</style>
</head>
    
    
    
        
        <xsl:for-each select="//process">
            <xsl:variable name="process" select="@name"/>
            <div class="label">
                <a>
                    <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
                    <xsl:value-of select="@name"/>.bpel
                </a>                    
            </div>            
            <div class="value">
                <xsl:value-of  select="description"/>
            </div>
            <br/>
            <div class="table" style="empty-cells:show;">
                <table border="1">
                    <tr>
                        <th>Platform</th>
                        <th>Operation</th>
                        <th>Also Used By</th>
                    </tr>
                    <xsl:for-each select="operation[@name != 'GetEndPoints']">
                        <xsl:variable name="operation" select="@name"/>
                        <tr>
                            <td>
                                <xsl:value-of select="@platform"/>
                            </td>
                            <td>
                                <xsl:value-of select="@name"/>
                            </td>
                            <td>
                                <xsl:for-each select="//operation[@name=$operation and @process != $process]">
                                    <xsl:value-of select="concat(@process,', ')"/>                                    
                                </xsl:for-each>                                
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
                <a HREF="javascript:history.go(-1)"><h4>back</h4></a>
            </div>
         </xsl:for-each>
         
         
         
</html>
        
    </xsl:template>
</xsl:stylesheet>

