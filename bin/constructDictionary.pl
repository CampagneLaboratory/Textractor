#!/usr/bin/perl
# to construct a dictionary from COMBINED svm results in svm_prediction/overlap
#
use Time::localtime;

@journals=("JBC", "PNAS", "EMBO");

$yearStart{"JBC"}=1995;
$yearStart{"PNAS"}=1996;
$yearStart{"EMBO"}=1997;

$yearEnd=2004;

$thresSVM=40;
$thres=0.25;

foreach $journal(@journals){
	for $year($yearStart{$journal}..$yearEnd){
		$input="svm_predictions/overlap/svm.w3.$journal$year.overlap";

		#print "parsing $input\n";
		open (In, $input) or die "can not open:$!\n";
		while (<In>){
			chomp;
			@elements=split /\t/;
			next, if $elements[2] !~/\d/;
			if ($elements[2] <$thresSVM){
				$negative{$elements[0]}++;
				next;
			}
			$terms{$elements[0]}+=$elements[2];
		}
		close In;
    }
}

$date=(localtime->mon+1)."_".(localtime->mday)."_".(localtime->year+1900);
$output="dictionary/svm.w3.dictionary.$date";
print $output;


open (Out, ">".$output) or die "can not open:$!\n";
foreach $term (sort keys %terms){
    next, if ($negative{$term}/($terms{$term}/$thresSVM))>$thres;
	next, if $trash{$term}>0;
	print Out $term,"\n";
}
close Out;

open (Neg, ">".$output.".negative") or die "can not open:$!\n";
foreach $term (sort keys %negative){
	next, if (($terms{$term}/$thresSVM)/$negative{$term})>=(1/$thres);
	print Neg $term,"\n";
}
close Neg;




