#!/usr/bin/perl

open In, $ARGV[0] or die "can not open target file:$!\n";
while (<In>){
	chomp;
	$terms{$_}=1;
}	

open Rank, "author.rank" or die "can not open source file:$!\n";
while (<Rank>){
	chomp;
    @elements=split /\t/;
	$term=lc($elements[0]);
	$authors{$term}=$elements[1];

    
}

open Out, ">author.occurrence" or die "can not open output:$!\n";
foreach $author(sort keys %terms){
    print Out $author,"\t", $authors{$author},"\n";
}