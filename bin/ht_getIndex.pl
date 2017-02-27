#!/usr/bin/perl

#usage: bin/ht_getIndex.pl databasename journal startYear endYear
#e.g.  "bin/ht_getIndex.pl 0 JBC 1995 1995"  to load JBC 1995 into database 0
$startTime=time;

$htDatabaseNumber=$ARGV[0];
$journal=$ARGV[1];
$start=$ARGV[2];
$end=$ARGV[3];


$schemaCreated=0;

for $i($start..$end){
    system("ant -f build/build_config.xml -DhtDatabaseNumber=$htDatabaseNumber -Djournal=$journal -Dyear=$i config_unix_ht");
    if ($schemaCreated==0){
        system("ant -f build/build.xml -DpropertyFilename=textractor.prop.ht$htDatabaseNumber jdo_createschema_fastobjects");
        $schemaCreated=1;
    }
    system("ant -f build/build.xml -DbuildIndex=1 -DpropertyFilename=textractor.properties.ht$htDatabaseNumber boot_forClassify") && die "can not run";
    
}

$endTime=time;
$span=($endTime-$startTime)/60;
printf "$journal $start $end took %8.3f min\n", $span;
