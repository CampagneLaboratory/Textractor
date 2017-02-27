#!/usr/bin/perl

use File::Basename;

opendir sourceDir, "./" or die "can not open index dir\n";
my @source=readdir sourceDir;

foreach $file(@source){
    if ($file=~/(.*)\.htm\.txt\.genes/){
        push (@fileList, $1);
        print $1,"\n";
    }
}

($parent)=`pwd`=~/.*\/([^\/]+)/;

chomp($parent);

open Out, ">$parent.yagi" or die "can not open output:$!";
foreach $PMID(@fileList){
    open In, "$PMID.htm.txt.genes" or die "can not open input:$!";
    my @list = ();
    while (<In>) {
        $a = $_;
        while ( length ($a) > 0 ) {
            push (@list, $b), if ($b, $a) = $a =~ /<GENE>\s*(.*?)\s*<\/GENE>(.*)/;
        }
    }

    foreach $i ( @list ) {
        print Out "$PMID\t$i\n";
    }
}