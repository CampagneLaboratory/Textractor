<?xml version="1.0" encoding="UTF-8"?>
<!-- edited with XMLSpy v2005 U (http://www.xmlspy.com) by Lei Shi (Weill Cornell Medical College) -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text" version="1.0" encoding="UTF-8" indent="no"/>
	<xsl:template match="acronyms">
		<xsl:apply-templates select="//expansion"/>
	</xsl:template>
	<xsl:template match="expansion">
		<xsl:value-of select="."/>
		<!-- <xsl:value-of select="@frequency"/>-->
		<xsl:text>
</xsl:text>
	</xsl:template>	
</xsl:stylesheet>
