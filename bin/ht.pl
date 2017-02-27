#!/usr/bin/perl
#usage: bin/ht.pl databasename journal startYear endYear y/n
#e.g.  "bin/ht.pl 0 JBC 1995 1995 n"  to load JBC 1995

$startTime=time;

$htDatabaseNumber=$ARGV[0];
$journal=$ARGV[1];
$start=$ARGV[2];
$end=$ARGV[3];

if ($ARGV[4] !~/y|n/i){
   print "To build the existing index again?(Y/N)\n";
   $line=<STDIN>;
}else{
   $line=$ARGV[4];
}

opendir indexDir, "index/" or die "can not open index dir\n";
my $indexDone=join (':', readdir indexDir);

$schemaCreated=0;

for $i($start..$end){
	system("ant -f build/build_config.xml -DhtDatabaseNumber=$htDatabaseNumber -Djournal=$journal -Dyear=$i config_unix_ht");
    $index=1;
    if ($schemaCreated==0){
        system("ant -f build/build.xml -DpropertyFilename=textractor.properties.ht$htDatabaseNumber jdo_createschema_fastobjects");
        $schemaCreated=1;
    }

    if ($indexDone =~ /$i_basename/){
        $index=0, if $line=~/n/i;
    }
    if ($index==1){
	    system("ant -f build/build.xml -DbuildIndex=1 -DpropertyFilename=textractor.properties.ht$htDatabaseNumber ht") && die "can not run";
    }else{
        system("ant -f build/build.xml -DpropertyFilename=textractor.properties.ht$htDatabaseNumber ht") && die "can not run";
    }
    
    system("ant -f build/build.xml -DpropertyFilename=textractor.properties.ht$htDatabaseNumber -DtargetClass-1=interaction classify") && die "can not run";
    
    system("ant -f build/build.xml -DpropertyFilename=textractor.properties.ht$htDatabaseNumber -DtargetClass-1=process classify") && die "can not run";
}

$endTime=time;
$span=($endTime-$startTime)/60;
printf "$journal $start $end took %8.3f min\n", $span;
