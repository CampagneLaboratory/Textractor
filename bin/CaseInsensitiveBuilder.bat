set CP=classes;lib\commons-cli-1.0.jar;lib\log4j-1.2.12.jar;lib\commons-logging.jar;lib\commons-configuration-1.2.jar;lib\commons-lang-2.1.jar;lib\mg4j-1.0.3.1.jar;lib\commons-collections-3.1.jar;lib\fastutil-4.4.3.jar;lib\JSAP-2.0a.jar;lib\colt.jar;

java -classpath %CP% textractor.caseInsensitive.CaseInsensitiveBuilder %*

