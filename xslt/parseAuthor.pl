#!/usr/bin/perl
#extract the author last names from pubmed source files
#run in parent dirctory of the pubmed source files.

for $i($ARGV[0]..$ARGV[1]){
	$number=substr(10000+$i,-4,4);
	$target="medline04n$number";
	`gunzip -c -d medlease/$target.xml.gz >temp.xml`;
	#Xalan's memory usage is problematic.
	#`java -Xmx1400m org.apache.xalan.xslt.Process -IN temp.xml -XSL medline_author2text.xslt -OUT author/$target.lastname`;
	`java -Xmx1400m -jar ../saxonb8-1-1/saxon8.jar temp.xml medline_author2text.xslt > author/$target.lastname`;
}	
