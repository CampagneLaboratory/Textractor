<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
<xsl:output doctype-public="txt/xml" />
<xsl:template match="/">

<links><xsl:apply-templates></xsl:apply-templates></links>
</xsl:template>
<xsl:template match="/html/body/table/tbody/tr/td/table/tbody/tr/td/a">
<xsl:element name="link">
<xsl:attribute name="href">
<xsl:value-of select="@href"></xsl:value-of>
</xsl:attribute>
</xsl:element>
</xsl:template>
<xsl:template match="text()">
</xsl:template>
</xsl:stylesheet>
