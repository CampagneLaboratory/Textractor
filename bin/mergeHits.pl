#!/usr/bin/perl
# merge the results from
# 1)svm training
# 2)dictionary lookup
# 3)YAGI
# 4)NLProt
# into individual files for a particular article: PMID.all
#
# <A>YAGI results should be preparsed first by preparseYAGI.pl in /bin
# <B>then YAGI and NLProt results should be parsed by corresponding classes in textractor.util
#
# run at root directory of textractor


$job=$ARGV[0]; #e.g. NCB, PMC

$lookupResultsDir="lookup_results";

#read parsed NLprot results
open NLProt, "$lookupResultsDir/$job/$job.nlprot.out.txt.tab"
    or die "can not open NLProt file.\n";
while(<NLProt>){
	chomp;
	@elements=split /\t/;
	$nlprot{$elements[0]}{$elements[2]}+=$elements[3];
	$all{$elements[0]}{$elements[2]}=1;
	if (defined($nlprotOrginalMatch{$elements[0]}{$elements[2]})) {
	    $nlprotOrginalMatch{$elements[0]}{$elements[2]}.="|".$elements[1];
	} else {
	    $nlprotOrginalMatch{$elements[0]}{$elements[2]}=$elements[1];
	}
    $nlprotOrginal{$elements[0]}{$elements[1]}=$elements[3];
}

#read parsed YAGI results
open YAGI, "$lookupResultsDir/$job/$job.yagi.tab"
    or die "can not open YAGI file.\n";
while(<YAGI>){
	chomp;
	@elements=split /\t/;
	$yagi{$elements[0]}{$elements[2]}+=$elements[3];
	$all{$elements[0]}{$elements[2]}=1;
	if (defined($yagiOrginalMatch{$elements[0]}{$elements[2]})) {
	    $yagiOrginalMatch{$elements[0]}{$elements[2]}.="|".$elements[1];
	} else {
	    $yagiOrginalMatch{$elements[0]}{$elements[2]}=$elements[1];
	}
    $yagiOrginal{$elements[0]}{$elements[1]}=$elements[3];
}

#read textractor SVM classification results
open SVM, "svm_predictions/overlap/svm.w3.$job.overlap"
    or die "can not open SVM file.\n";
while(<SVM>){
	chomp;
	@elements=split /\t/;
    next, if $elements[2]<0;
	$svm{$elements[1]}{$elements[0]}=$elements[2];
    $svmcount{$elements[1]}{$elements[0]}=$elements[3];
	$all{$elements[1]}{$elements[0]}=1;
}

foreach $PMID (sort keys %nlprot){
	open Out, ">$lookupResultsDir/$job/$PMID.nlprot" 
	    or die "can write :$!\n";
	open All, ">$lookupResultsDir/$job/$PMID.all"
	    or die "can write :$!\n";
	open Tex, "<$lookupResultsDir/$job/$PMID.hit"
	    or die "can not open textractor $PMID.hit:$!\n";

	while (<Tex>){
		chomp;
		@elements=split /\t/;
		$textractor{$PMID}{$elements[0]}=$elements[1];
		$all{$PMID}{$elements[0]}=1;
	}

	for $term (sort keys%{$nlprotOrginal{$PMID}}){
		print Out $term,"\t", $nlprotOrginal{$PMID}{$term}, "\n";
	}

	for $term (sort keys%{$all{$PMID}}){
		print All $term,"\t";

        if ($svm{$PMID}{$term}>0){
        	print All $term, "\t", $svmcount{$PMID}{$term},"\t";
        }else{
        	print All "\t\t";
        }

        if ($textractor{$PMID}{$term}>0){
        	print All $term, "\t", $textractor{$PMID}{$term},"\t";
        }else{
        	print All "\t\t";
        }

        if ($yagi{$PMID}{$term}>0){
        	print All $term, "\t",
        	    $yagi{$PMID}{$term},"\t",
        	    $yagiOrginalMatch{$PMID}{$term},"\t";
        }else{
        	print All "\t\t\t";
        }

        if ($nlprot{$PMID}{$term}>0){
        	print All $term, "\t",
        	    $nlprot{$PMID}{$term},"\t",
        	    $nlprotOrginalMatch{$PMID}{$term},"\t";
        }else{
        	print All "\t\t\t";
        }
		print All "\n";
	}
	close Out;
	close All;
}