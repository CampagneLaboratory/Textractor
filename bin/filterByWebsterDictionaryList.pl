#!/usr/bin/perl

$target=$ARGV[0];

$forProtein=1;
if ($ARGV[1] eq "-notForProtein"){
    $forProtein=0;
}

if ($forProtein==1){
    $exc="dictionary/webster_dictionary/webster-gcide-0.46.dic.exclusion";
    open (Exc, $exc) or die "can not open $exc:$!\n";
    while (<Exc>){
        chomp;
        $exclusion{$_}=1;
    }

    ($negative_filename)=$target=~/(^.*)\.w_expansion\.filtered$/;
    $negative_filename.=".negative";
    open (TargetN, $negative_filename) or die "can not open $negative_filename:$!\n";
    while (<TargetN>){
        chomp;
        $negative{$_}=1;
    }
}

$dic="dictionary/webster_dictionary/webster-gcide-0.46.dic.uniq";
open (In, $dic) or die "can not open $dic:$!\n";

while (<In>){
    chomp;
    @elements=split /\t/;
    $term=$elements[0];
    next, if $exclusion{$term}>0;
    $property=$elements[1];
    
    $terms{$term}=1;
    $properties{$term}.=":".$property;
    
 
	if ($term=~/s$/){
		$newterm=$term."es";
	}elsif($term=~/ty$/){
	    $newterm=substr($term,0,length($term)-1)."ies";	
	}else{
		$newterm=$term."s";
	}
	
	if ($terms{$newterm}<1){
	    $terms{$newterm}=1;
	    $properties{$newterm}.=":v.", if $property=~/^v/;
	}


    if ($property=~/^v/){			#verb		
		if ($term=~/te$/){
		    chop($term);              #remove e
			$terms{$term."ing"}=1; #ing
		}else{
			$terms{$term."ing"}=1; #ing
		}
		
	}
	
}

close In;

open (TargetP, $target) or die "can not open $target:$!\n";
open (OutI, ">".$target.".in_dic") or die "can not write:$!\n";
open (OutO, ">".$target.".out_dic") or die "can not write:$!\n";

while (<TargetP>){
	chomp;
	#the terms from the splitting "* and *" may contain the ones already excluded by svm.
	next, if $negative{$_}>0;
	$term=$_;
	$term=~ s/^the //g;

	if ($terms{$_}>0 || $terms{$term}>0){
		print OutI $_,"\t", $properties{$_},"\n";
		next;
	}

	#simple stemming
	if ($term =~ /^(anti|co|de|dis|in|mis|non|photo|re|un)-?(.+)/){
		if ($terms{$2}>0 && length($2)>5){
			print OutI $_,"\t", $properties{$_},"ROOT\n";
			next;
		}	
	}

	if ($forProtein){
        if (/\s|-/){
            @elements=split /\s|-/;
            if ($elements[$#elements] !~ /^(\d+)|(fab)$/ &&
                $properties {$elements[$#elements]} !~ /:n/ &&
                $properties {$elements[$#elements]} =~ /:(a|imp|v)/
                ){
                #print $elements[$#elements],"\n";
                print OutI $_,"\t", $properties {$elements[$#elements]},"POST\n";
                next;
            }
            if ($elements[$#elements] =~ /^(binding|cell|interaction|muscle|sequence)e?s?$/){
                #print $elements[$#elements],"\n";
                print OutI $_,"\t", $properties {$elements[$#elements]},"POST\n";
                next;
            }
        }

        if ($term =~ /(nucleotide|virus)e?s?$/){
            print OutI $_,"\t", $properties {$_},"POST\n";
            next;
        }

        #if the plural forms of obvious protein names are not in, add them
        if ($term=~/^(.*(enzyme|factor|protein|receptor|[a-zA-Z]+ase))$/){
            $outTerms{$term."s"}=1;
        }

        #if the single forms of obvious protein names are not in, add them
        if ($term=~/^(.*(enzyme|factor|protein|receptor|[a-zA-Z]+ase))s?$/){
            $outTerms{$1}=1;
        }
	}

    $outTerms{$term}=1;
    $outTerms{$_}=1;
}

foreach $term(sort keys%outTerms){
    print OutO $term, "\n";
}