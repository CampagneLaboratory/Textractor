#!/usr/bin/perl
#if the extracted expansions are too long, they are separated by line break,
#I)use this script to remove the line break--the divided expansion is like
#"part A"
#"   part B"
#combine them into "part A part B"
#
#II)also to remove something like
#") " from
#") and peptide YY"

open In, $ARGV[0], or die "can not open input";

$all="";
while(<In>){
    $all.=$_;
}

$all=~s/\n\s+/ /g;

$all=~s/(^|\n)\)\s*/\1/g;

open Out, ">".$ARGV[0].".parsed", or die "can not open output";
print Out $all;
