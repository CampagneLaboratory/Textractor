#!/usr/bin/perl

opendir indexDir, "./" or die "can not open current dir\n";
my @source=readdir indexDir;

open (Out, ">"."webster-gcide-0.46.dic") or die "can not write:$!\n";
$outFileHandle=*Out;

open (Exc, ">"."webster-gcide-0.46.dic.exc.raw") or die "can not write:$!\n";
$excFileHandle=*Exc;

foreach $filename(@source){
    next, if $filename!~/^CIDE/;
    open (In, $filename) or die "can not open $filename:$!\n";
    print "parsing $filename\n";
    while (<In>){
    	s/\\\'80/C/g;
    	s/\\\'81/u/g;
    	s/\\\'82/e/g;
    	s/\\\'83/a/g;
    	s/\\\'84/a/g;
    	s/\\\'85/a/g;
    	s/\\\'87/c/g;
    	s/\\\'88/e/g;
    	s/\\\'89/e/g;
    	s/\\\'8a/e/g;
    	s/\\\'8b/i/g;
    	s/\\\'8c/i/g;
    	s/\\\'90/e/g;
        s/\\\'91/ae/g;
        s/\\\'92/AE/g;
        s/\\\'93/o/g;
        s/\\\'94/o/g;
        s/\\\'96/u/g;
        s/\\\'97/u/g;
        s/\\\'a0/a/g;
        s/\\\'a2/o/g;
        s/\\\'a4/n/g;
        s/\\\'d1/OE/g;
        s/\\\'d2/oe/g;
        s/\\\'d8//g;
        s/\\\'ee/a/g;
        

        
        @biglines=split /\<p\>/;
        foreach $bigline(@biglines){
			if ($bigline =~ /enzyme|protei(n|d)/i){
				$bioSign=1;
			}else{
				$bioSign=0;
        	}
			if ($bigline=~/\<mhw\>/) {

				@lines=split /\<((\/pos)|mhw)\>/,$bigline;
				foreach $line (@lines){

					next, if $line !~ /\<pos\>(.*)$/x;         # to match <pos>property</pos>				

					$property=$1;
					@elements=split /\<\/hw\>/, $line;         # to split <hw>term0</hw> <hw>term1</hw>
					$count=0;
					foreach $element(@elements){

					    next, if $element !~ /\<hw\>(.+)$/;

					    $mterm[$count++]=$1;
					}
					
					for $i(0..$count-1){
						printTerm($mterm[$i], $property, $bioSign, $outFileHandle);
					}	
				}
			
			}else{	

				@lines=split /\<((\/pos)|hw)\>/, $bigline; 
				foreach $line (@lines){

					next, if $line !~ /^(.+)\<\/hw\>.*
									   \<pos\>(.*)$/x;         # to match <hw>term</hw>.* <pos>property</pos>


					$term=$1;
					$property=$2;
					printTerm($term, $property, $bioSign, $outFileHandle);
				}
			}

			
			if($bigline=~/\<vmorph\>\[(.+)\]\<\/vmorph\>/){               # to match <vmorph>... </vmorph>
				@lines=split /\<(pos|\/conjf)\>/, $1;
				foreach $line (@lines){

					next, if $line !~ /^(.+)\<\/pos\>.*
									   \<conjf\>(.*)$/x;		# to match <pos>property</pos>.* <conjf>term</conjf>
					$term=$2;
					$property=$1;
					printTerm($term, $property, $bioSign, $outFileHandle);
				}						
						
			}
		}
    }
    close In;
    	
}

sub printTerm (){
	my ($term, $property, $bioSign, $out)=@_;

	$term =~ tr/A-Z/a-z/;
	$term =~ s/\"|\*|\`//g;

	print "$term\n", if $term=~/^\W|\\/;

	if ($term !~/^\W|\\/){
		print $out $term, "\t", $property,"\n";
	}
	
	if ($bioSign==1 && $property=~/n/) {
		print Exc $term, "\n";
	}

}