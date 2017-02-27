#!/bin/sh

CP=build/classes:lib/edu.mssm.crover.cli.jar:lib/htmlparser.jar:lib/xerces.jar:lib/jaxb-api.jar:lib/jaxb-libs.jar:lib/jaxb-ri.jar:lib/mg4j-0.8.2.jar

java -cp $CP textractor.tools.find_terms $*
