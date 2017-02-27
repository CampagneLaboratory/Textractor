#!/usr/bin/perl
#the process to build and filter a dictionary
#this file can also be run as a script in textractor root directory

$source="dictionary/organism_names/organismNames.txt";
#export organism names
    `bin/filterByOrganismNameList.pl`;

    $dic_name=$source.".parsed2";

#filter the resulting dictionary with "filter_dictionary" target in build.xml
    `ant -f build/run.xml -Ddictionary=$dic_name -Doptional_keywords=-organism filter`;
    $dic_name.=".filtered";

#filter the resulting dictionary by pubmed last names
    `bin/filterByPubmedAuthor.pl $dic_name`;
    $dic_name.=".out_au";

    $final_name=$dic_name.".uniq";
    `sort $dic_name |uniq > $final_name`;