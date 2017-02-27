#!/usr/bin/perl

$in=$ARGV[0];

open (In, $in) or die "can not open $in:$!\n";
@targets=("receptor","kinase","protein","cell","binding","phosphorylation","activation","association","activity", "expression", "release");

while (<In>){
	@elements=split /\t/;
	$positive++, if $elements[2]>0;
	$total++, if $elements[2]=~/\d/;
    foreach $target(@targets){
        if (/ ($target)s?\t/){
            $all{$target}++;
            $neg{$target}++, if /\t\-/;
        }
    }
}

print "positive/total", $positive,"/", $total,"\n";
foreach $target(@targets){
	print $target, "\t",$neg{$target}, "/", $all{$target},"\n";
}
