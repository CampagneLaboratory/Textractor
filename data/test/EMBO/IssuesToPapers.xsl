<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
<xsl:template match="links/link">
<xsl:apply-templates mode="link" select="document(@href)">
</xsl:apply-templates>
</xsl:template>
<xsl:template mode="link" match="*">
<xsl:element name="paper">
<xsl:attribute select="/html/body/table/tbody/tr/td/a/@href"></xsl:attribute>
</xsl:element>
</xsl:template>
</xsl:stylesheet>
