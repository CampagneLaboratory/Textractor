#!/usr/bin/perl

$in=$ARGV[0];

open (In, "statistics/$in") or die "can not open $in:$!\n";
open (Out,">sum/$in.sum") or die "can not write $in.sum:$!\n";

$currentTerm="";
while (<In>){
    chomp;
    @elements=split /\t/;
    if (($currentTerm eq $elements[0]) && ($currentPMID==$elements[3])){
    	$currentSum+=$elements[2];
    	$count++;
    }else{
    	if ($currentTerm ne ""){
        	print Out $currentTerm,"\t",$currentPMID,"\t",$currentSum,"\t",$count,"\n";

        }
        $currentSum=$elements[2];
        $currentTerm=$elements[0];
        $currentPMID=$elements[3];
        $count=1;
    }
}
print Out $currentTerm,"\t",$currentPMID,"\t",$currentSum,"\t",$count,"\n";

close Out;
close In;

system ("../bin/reportFinal.pl sum/$in.sum");

