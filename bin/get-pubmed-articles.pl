#!/usr/bin/env perl
#
# Get "non-medline" articles from PubMed.  Adapted from "eutils_example.pl".
# Articles are stored as xml in one or more files on the local disk.
#

use Getopt::Long;        # Command line parsing
use LWP::Simple;         # Simple url processing

# verbose output
my $verbose = '';

# URLs for ncbi eutils and pubmed search and query
my $ncbi_eutils = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
my $pubmed_search_url = "$ncbi_eutils/esearch.fcgi?db=pubmed&tool=Twease&email=icb@med.cornell.edu";
my $pubmed_fetch_url = "$ncbi_eutils/efetch.fcgi?db=pubmed&tool=Twease&email=icb@med.cornell.edu";

#  Equivalent to web query of PubMed with "ahead of print"[Filter] OR "in process"[Filter]
my $query = "ahead+of+print[filter]+OR+in+process[filter]";

# maximum number of results to return in from a single fetch operation
my $retmax = 10000;

# prefix to use for result files
my $prefix = "pubmed";

# parse command line options
if (! GetOptions('verbose' => \$verbose,
		 'query=s' => \$query,
		 'retmax=i' => \$retmax,
		 'prefix=s' => \$prefix)) {
    die "Error processing command line";
}

if ($verbose) {
    print "verbose = $verbose\n";
    print "query = $query\n";
    print "retmax = $retmax\n";
    print "prefix = $prefix\n";
}

# $esearch contains the PATH & parameters for the ESearch call
my $esearch = "$pubmed_search_url" . "&retmax=1&usehistory=y&term=";

# $esearch_result containts the result of the ESearch call
my $esearch_result = get($esearch . $query);
die "Error getting search result from PubMed" unless defined $esearch_result;

# parse result into variables  $Count, $QueryKey, and $WebEnv for later use
$esearch_result =~ 
    m|<Count>(\d+)</Count>.*<QueryKey>(\d+)</QueryKey>.*<WebEnv>(\S+)</WebEnv>|s;

my $Count    = $1;
my $QueryKey = $2;
my $WebEnv   = $3;

if ($verbose) {
    print "Count = $Count; QueryKey = $QueryKey; WebEnv = $WebEnv\n";
}

# this area defines a loop which will store $retmax citation results

my $retstart;
my $filecount = 0;  # number each file in case there needs to be more than 1

for ($retstart = 0; $retstart < $Count; $retstart += $retmax) {
    $filecount = $filecount + 1; 
    
    my $efetch = "$pubmed_fetch_url" .
	"&retmode=xml&retstart=$retstart&retmax=$retmax" .
	"&query_key=$QueryKey&WebEnv=$WebEnv";
    my $file = sprintf("%s_%04d.xml", $prefix, $filecount);
    
    if ($verbose) {
	my $range_start = $retstart + 1;
	my $range_end = ($retstart + $retmax) < $Count ? $retstart + $retmax : $Count;
	
	print "Writing results [$range_start" . ".." . $range_end . "] to $file\n";
    }
    
    # store the results in an a file
    if (is_error(getstore($efetch, $file))) {
	die "Error getting articles";
    }
}

if ($verbose) {
    print "Stored $Count articles in $filecount files\n";
}
