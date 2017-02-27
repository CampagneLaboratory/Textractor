#!/usr/bin/perl
open(M,$ARGV[0]) || die();

$l=<M>;
if(($l=<M>) != 0) { die("Not linear Kernel!\n"); }
$l=<M>;
$l=<M>;
$l=<M>;
$l=<M>;
$l=<M>;
$l=<M>;
$l=<M>;
$l=<M>;
$l=<M>;

while($l=<M>) {
    ($alpha,@f)=split(/ /,$l);
    for $p (@f) {
	($a,$v)=split(/:/,$p);
	$w[$a]+=$alpha*$v;
    }
}

# copen(WORD,$ARGV[2]) || die("could not open $ARGV[1]\n");
# while($l=<WORD>) {
#     chop $l;
#     $wcount++;
#     $word[$wcound]=$l;
# }

for($i=1;$i<=$#w;$i++) {
    print "$i:$w[$i]\n";
}
