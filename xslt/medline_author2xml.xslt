<?xml version="1.0" encoding="UTF-8"?>
<!-- edited with XMLSpy v2005 U (http://www.xmlspy.com) by Lei Shi (Weill Cornell Medical College) -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:template match="MedlineCitationSet">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="MedlineCitation">
		<authors>
			<pmid>
				<xsl:value-of select="PMID"/>
			</pmid>
			<lastnames>
				<xsl:apply-templates select="Article//LastName"/>
			</lastnames>
		</authors>
	</xsl:template>
	<xsl:template match="LastName">
		<lastname>
			<xsl:value-of select="."/>
		</lastname>
	</xsl:template>
</xsl:stylesheet>