#!/usr/bin/perl
#count the occurrence of last names in pubmed, 
#based on the extracted last names by parseAuthor.

for $i($ARGV[0]..$ARGV[1]){
	$number=substr(10000+$i,-4,4);
	$target="medline04n$number";
	open Au, "author/$target.lastname" or die "can not open input:$!\n";
	while (<Au>){
		chomp;
		$authors{$_}++;
	}
}	

open Out, ">author.rank" or die "can not open output:$!\n";
foreach $author(sort{$authors{$a}<=>$authors{$b}} keys %authors){
	next, if $authors{$author}<2;
    print Out $author,"\t", $authors{$author},"\n";
}