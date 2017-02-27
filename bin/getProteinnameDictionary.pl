#!/usr/bin/perl
#the process to build and filter a dictionary
#this file can also be run as a script in textractor root directory

#use bin/constructDictionary.pl to construct dictionary from three classification results:
#	protein-cell
#	protein-process
#	protein-interaction
    $dic_name=`bin/constructDictionary.pl`;

#add the acronym expansions to the dictionary
    $dic_name_old=$dic_name;
    $dic_name.=".w_expansion";
    `cat dictionary/acronym_expansions/acronym_expansions.txt.parsed $dic_name_old | sort |uniq >$dic_name`;

#filter the resulting dictionary with "filter_dictionary" target in build.xml
    `ant -f build/run.xml -Ddictionary=$dic_name filter_dictionary`;
    $dic_name.=".filtered";

#filter the resulting dictionary by webster dictionary
    `bin/filterByWebsterDictionaryList.pl $dic_name`;
    $dic_name.=".out_dic";

#filter the resulting dictionary by organism names
    `bin/filterByOrganismNameList.pl $dic_name`;
    $dic_name.=".out_org";

#filter the resulting dictionary by pubmed last names
    `bin/filterByPubmedAuthor.pl $dic_name`;
    $dic_name.=".out_au";

    print "The final dictionary name is: ", $dic_name, "\n";
    `rm dictionary/currentDictionary`;
    `cp $dic_name dictionary/currentDictionary`;