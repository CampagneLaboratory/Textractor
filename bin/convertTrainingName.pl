#!/usr/bin/perl
#convert the training name to final format

$source=$ARGV[0];

open (In, $source) or die "can not open: $!";

$count=0;
$termCount=0;
while (<In>){
	$entry[$count]=$_;
	@elements=split /\t/;
	$id[$count]=$elements[0];
	$id[$count]=~ s/\*//g;
	$term[$count]=$elements[$#elements];
	$termCount+=$elements[$#elements-1];
	#print $id[$count], "\t", $term[$count], "\n";
	$count++;
}

#die;
open (Out, ">../namesFinal/".$source.".final") or die "can not open: $!";

for $i(0..$count-1){
	print Out $term[$i];
}

print $termCount,"\n";