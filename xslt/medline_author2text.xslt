<?xml version="1.0" encoding="UTF-8"?>
<!-- edited with XMLSpy v2005 U (http://www.xmlspy.com) by Lei Shi (Weill Cornell Medical College) -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text" version="1.0" encoding="UTF-8" indent="no"/>
	<xsl:template match="MedlineCitationSet">
		<xsl:apply-templates select="//Author"/>
	</xsl:template>
	<xsl:template match="//Author">
		<xsl:value-of select="LastName"/><xsl:text>
</xsl:text>
	</xsl:template>	
</xsl:stylesheet>