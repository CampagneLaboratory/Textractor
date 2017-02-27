These OTMI files come from

http://www.nature.com/nature/journal/v440/n7083/index.html

Nature 23 March 2006. To generate the list of OTMI files I grabbed the source for the URL
mentioned above, filter out to just the

   <a class="fulltext" ...> ... </a>
   
links then to just the URLs within the HREFs then changed the directory from /full/
to /otmi/otmi-  and changed the extention from .html to .xml.

I write this list of URLs to "all-otmi-docs.lst" and used GetRight to download the
entire list of URLs.

The OTMI file associated with "440439a" ("ION channels") did not exist. Finally I
created a file that was a list of these files with complete path named "otmifiles.txt".

This is now suitable for import using Textractor using the otmi-full-import tag
inside pubmed.xml.

