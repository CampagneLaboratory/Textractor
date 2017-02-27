#!/usr/bin/perl

$targetRecords=$ARGV[0];
$targetCount=$ARGV[1];
open Records, $targetRecords or die "can not open records\n";

while (<Records>){
    $positive[$positiveCount++]=$_, if /^1/;
    $negative[$negativeCount++]=$_, if /^-1/;
}

open RR, ">$targetRecords.r" or die "can not open random\n";
$outFileHandle=*RR;
srand(time|$$);

($count,$ref_source)=outRan(0, $targetCount, \@positive,$outFileHandle,0);
outRan($count, $targetCount*2, \@negative,$outFileHandle,0);


sub outRan{
	my ($count, $targetCount, $ref_source,$out,$negative)=@_;
	my @source=@{$ref_source};
	$bucket=1;
	
	while ($count<$targetCount/$bucket){
	
	   $randm=int(rand($#source/$bucket));    
	   $count++;
	   
	   my %outputed;
	   for $i(0..$bucket-1){
	   	   $outputNumber=$randm+$i;
	   	   $source[$outputNumber]=~s/^1/-1/, if $negative==1;
		   print $out $source[$outputNumber];		   
		   $outputed{$outputNumber}=1;
	   }
	   
	   my @shrinked;
	   for $i(0..$#source){
	       push(@shrinked, $source[$i]) unless $outputed{$i}==1;
	   }	
	   @source=@shrinked;
	   #print $count, "\t", $#source, "\n";
	}
	
	return $count, \@source;
	
}
	



