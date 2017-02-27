
CP=classes:lib/edu.mssm.crover.cli.jar:lib/mg4j-0.9.jar:lib/fastutil-4.3.1.jar:lib/log4j.jar:lib/java-getopt-1.0.9.jar

java -Xmx1999m -classpath $CP textractor.tools.genbank.BuildDocumentIndexFromGenbankFiles $*
