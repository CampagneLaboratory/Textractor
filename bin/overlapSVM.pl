#!/usr/bin/perl

$journal=$ARGV[0];
$yearStart=$ARGV[1];
$yearEnd=$ARGV[2];

@classes=("cell","interaction","process");

for $year($yearStart..$yearEnd){
        if ($year==0){
	    $job=$journal;
        }else{
            $job=$journal.$year;
        }
	my %score;
	foreach $class(@classes){
		$input="svm.w3.$job.protein-$class.statistics.txt";
		
		print "parsing $input\n";
		`../bin/sumSVM.pl $input`;
		
		open (In, "sum/$input.sum") or die "can not open:$!\n";
		while (<In>){
			chomp;
			@elements=split /\t/;
			next, if $elements[2] !~/\d/;
			$elements[2]=$elements[2]*50,if $elements[2]<0;
			$score{"$elements[0]\t$elements[1]"}+=$elements[2];
			$count{"$elements[0]\t$elements[1]"}=$elements[3];
		}
		close In;
	}
	
	$output="overlap/svm.w3.$job.overlap";
	open (Out, ">".$output) or die "can not open:$!\n";
	foreach $term (sort {$score{$a} <=> $score{$b}} keys %score){
		print Out $term,"\t",$score{$term},"\t",$count{$term},"\n";
	}

}	


