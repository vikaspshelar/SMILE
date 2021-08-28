<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
    <xsl:output method="html" version="1.0" encoding="utf-8"  media-type="text/html" />
    <xsl:template match="/">
        <html>
            <head>
                <style type="text/css">

body {
    font-size: 2pt;
    word-wrap: break-word;
}
table 
{
    font-size: 8pt;
    table-layout: automatic;
} 

table tr.head {
    background-color: grey;
}

table td.narrow {
    background-color: grey;
}

table tr.even {
    background-color: #E4ECC3;
}

table tr.odd {
    background-color: #fff;
}
table td.marked {
    background-color: red;
}
table td.head {
    background-color: grey;
}

                </style>
            </head>
            <body>
                <table border="1" style="empty-cells:show;" width="100%">
                    <!-- Platforms & operations -->
                    <tr class="head">
                        <td> </td>
                        <xsl:for-each select="//operation[not(name=following::operation/name)]">
                        <!-- loop through unique operations -->
                            <td class="narrow">
                                <xsl:value-of select="concat(platform,' ', name)"/>
                            </td>
                        </xsl:for-each>
                    </tr>
                    
                    <xsl:for-each select="//operation[not(process=following::operation/process)]">
                        <xsl:sort select="process"/>
                    <!-- loop through unique processes -->
                        <xsl:variable name="process_row_name" select="process"/>
                        <tr>
                            <xsl:attribute name="class">
                                <xsl:choose>
                                    <xsl:when test="position() mod 2">even
                                    </xsl:when>
                                    <xsl:otherwise>odd</xsl:otherwise>
                                </xsl:choose>
                            </xsl:attribute>
                            <td class="head">
                                <xsl:value-of select="$process_row_name"/>
                            </td>
                             <!-- loop through the unique operations -->
                            <xsl:for-each select="//operation[not(name=following::operation/name)]">
                                <td>
                                    <xsl:variable name="operation_column_name" select="name"/>
                                    <xsl:for-each select="//operation[process=$process_row_name and name=$operation_column_name]">
                                    <xsl:attribute name="class">
                                        marked
                                    </xsl:attribute>
                                        X
                                    </xsl:for-each>
                                </td>
                            </xsl:for-each>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>

