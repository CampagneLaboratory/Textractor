#!/usr/bin/perl
#create new source name list during the grepTrainingName,
#to avoid grep same term for different lists.

open (In, "trainingJBC1999.term") or die "can not open: $!";

$count=0;
while (<In>){
	$entry[$count]=$_;
	@elements=split /\t/;
	$id[$count]=$elements[0];
	$id[$count]=~ s/\*//g;
	#print $id[$count], "\n";
	$count++;
}

$dir=".";
opendir(BIN, $dir) or die " can not open $dir: $!";
while (defined ($file=readdir BIN)){
	next, if $file !~ /^Names2/;
	open (Re, $file) or die "can not open $file: $!";
	#print "parsing ", $file,"\n";
	while (<Re>){
		@elements=split /\t/;
		$reid=$elements[0];
		$reid=~ s/\*//g;
		$markDelete[$reid]=1;

	}
	close Re;
}

open (Out, ">rest.term") or die "can not open: $!";

for $i(0..$count-1){
	if ($markDelete[$id[$i]]!=1){
		print Out $entry[$i];
	}
}