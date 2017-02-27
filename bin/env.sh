#!/bin/bash
echo Setting up ClassPath 
CP=$PWD:$PWD/classes:$PWD/config:$PWD/test-classes
avoid1="lib/FastObjects_t7_SDK.jar"
avoid2="lib/FastObjects_t7_JDO.jar"
for file in lib/*.jar
do
	if [ $file = $avoid1 ] || [ $file = $avoid2 ]
	then
		echo "skipping $file"
	else
		echo "adding $file"
		CP=$CP:$PWD/$file
	fi
done
