#!/usr/bin/perl

$target=$ARGV[0];

$dic="dictionary/organism_names/organismNames.txt";
open (In, $dic) or die "can not open $dic:$!\n";

while (<In>){
    chomp;

    #replace the "|'|=|/ with space
    s/\'|\"|=|\// /g;

    s/^\s+//g;

    #remove starting "strain";
    s/^strain//;

    next, if /^\d+$/;

    $term=$_;

    if ($term =~ /^\((.+)\)$/){
    	$term=$1;
    }
    
    $term =~ s/^\s+//g;
    $term =~ s/\s{2,}/ /g;

    next, if $term =~ /^[A-Z].{0,2}[A-Z0-9]$/;
    $terms{$term}=1;							#get Escherichia coli

    $lowcase= $term;
    $lowcase =~ tr/A-Z/a-z/;
    $terms{$lowcase}=1;							#get escherichia coli

    @elements=split /\s/, $term, 2;
    if ($#elements > 0){
    	next, if $elements[1] =~ /^\d+$/;
    	next, if length($elements[1])<4;
    
    	$lastName=$elements[0];
    	next, if $lastName !~ /^[A-Z]/;
    	
    	if ($lastName=~/[a-z]+$/){     			
			$lastName=~ tr/A-Z/a-z/;
			$terms{$lastName}=1;				#get escherichia
		}	
    	    	
		$lastName=~ tr/a-z/A-Z/;
		$lastName=substr($lastName, 0, 1);
		$terms{$lastName.". ".$elements[1]}=1;	#get E. coli
		
		next, if $elements[1] !~ /^[a-z]+$/;
		$terms{$elements[1]}=1;					#get coli
	}
	
}

close In;

open (Out, ">".$dic.".parsed") or die "can not open $dic.parsed:$!\n";
open (Out2, ">".$dic.".parsed2") or die "can not open $dic.parsed:$!\n";
foreach $term(sort keys %terms){
	print Out $term, "\n";
	if ($term!~/\(|\)|(et al\.)|( and )/){
	    print Out2 $term, "\n";
	}
}

if (defined($target)){
    open (Target, $target) or die "can not open $target:$!\n";
    open (OutI, ">".$target.".in_org") or die "can not write:$!\n";
    open (OutO, ">".$target.".out_org") or die "can not write:$!\n";

    while (<Target>){
        chomp;

        $term=$_;
        $term=~ s/^the //g;

        if ($terms{$term}>0){
            print OutI $_,"\n";
            next;
        }

        print OutO $_, "\n";
    }
}
