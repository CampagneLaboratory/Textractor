<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:output omit-xml-declaration="yes" doctype-public="text/gmf"/>
	<xsl:template match="corpus">
	<xsl:text>
Creator	"yFiles"
Version	2.0
graph
[
	hierarchic	1
	label	""
	directed	1	
	</xsl:text>
		<xsl:apply-templates/>
		<xsl:text>
		</xsl:text>
		<xsl:for-each select="term/associations/term-association">
			<xsl:text>
			edge
			[ </xsl:text>	<!--xsl:value-of select="concat(substring-after(../../@id,'term-'),substring-after(@id,'ta-'))"/>99--><xsl:text> 
			   source </xsl:text><xsl:value-of select="substring-after(@term-before,'term-')"/>0<xsl:text> 
			   target  </xsl:text><xsl:value-of select="substring-after(@term-after,'term-')"/>0<xsl:text> 
			]		</xsl:text>
		</xsl:for-each>
				<!--xsl:call-template name="process-term-associations" ></xsl:call-template-->
		<xsl:text>
]

</xsl:text>
	</xsl:template>
	<xsl:template match="term">
		<xsl:text>
		node
		[ 
			id </xsl:text><xsl:value-of select="substring-after(@id,'term-')"/>0 <xsl:text>
			label  "</xsl:text>
		<xsl:value-of select="@value"></xsl:value-of>"<xsl:text>
		 ] </xsl:text>
		
	</xsl:template>
	<xsl:template name="process-term-associations"/>
</xsl:stylesheet>
