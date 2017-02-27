#!/usr/bin/perl
#filter the dictionary with the last names collected from pubmed source xml files.
# $threshold_occurrence is the minimum number of last names appearances in pubmed
# $threshold_length is the minimum length of the last names

$target=$ARGV[0];
$dic="dictionary/pubmed_lastnames/author.rank";
$threshold_occurrence=10;
$threshold_length=4;

open (In, $dic) or die "can not open $dic:$!\n";
$term_count=0;
while (<In>){
    chomp;
    @elements=split /\t/;
    if ($elements[1]>=$threshold_occurrence){
        #many ambiguous ones ended with in, e.g. ezrin...
        if ($elements[0]=~ /in$/ && $elements[1]<=200){
            next;
        }

        next, if length($elements[0])<$threshold_length;
        $term=lc($elements[0]);
        $terms{$term}=1;
        $term_count++;
    }
}

close In;

print "filter list size: ", $term_count,"\n";

#foreach $term(sort keys %terms){
#	print $term, "\n";
#}	
#die;



open (Target, $target) or die "can not open $target:$!\n";
open (OutI, ">".$target.".in_au") or die "can not write:$!\n";
open (OutO, ">".$target.".out_au") or die "can not write:$!\n";

while (<Target>){
	chomp;
	
	if ($terms{$_}>0){
		print OutI $_,"\n";
		next;
	}

	print OutO $_, "\n";
}
