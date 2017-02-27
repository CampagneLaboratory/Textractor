<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:output omit-xml-declaration="yes" doctype-public="text/gmf"/>
	<xsl:template match="corpus">
	<xsl:text>comment: "editor.ExGraphManager File"
version: "5.0"
creator: "Graph Editor Toolkit for Java"

graphManager: 0
{
	text: 
	topology:
	{
		graph: 1
		{
		topology:
			{</xsl:text>
		<xsl:apply-templates/>
		<xsl:text>
		
		
		</xsl:text>
		<xsl:for-each select="term/associations/term-association">
			<xsl:text>edge: </xsl:text>	<xsl:value-of select="concat(substring-after(../../@id,'term-'),substring-after(@id,'ta-'))"/>99<xsl:text> { source: </xsl:text><xsl:value-of select="substring-after(@term-before,'term-')"/>0<xsl:text> target:  </xsl:text><xsl:value-of select="substring-after(@term-after,'term-')"/>0<xsl:text> 
				ui:
					{
						name: com.tomsawyer.editorx.ui.TSEDashedEdgeUI
						width: 1<!--/xsl:text><xsl:value-of select="@occurence-count"></xsl:value-of><xsl:text-->
					}
				}	
					</xsl:text>
		</xsl:for-each>
				<!--xsl:call-template name="process-term-associations" ></xsl:call-template-->
		<xsl:text>
			}
		}
	}
}
</xsl:text>
	</xsl:template>
	<xsl:template match="term">
		<xsl:text>
		node: </xsl:text><xsl:value-of select="substring-after(@id,'term-')"/>0 <xsl:text>{ text: </xsl:text>
		<xsl:value-of select="@id"></xsl:value-of><xsl:text>
		 } </xsl:text>
		
	</xsl:template>
	<xsl:template name="process-term-associations"/>
</xsl:stylesheet>
