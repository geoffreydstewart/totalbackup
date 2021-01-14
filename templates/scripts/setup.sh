#!/bin/sh

#export JAVA_HOME=/opt/java/jdk1.6.0_14

if [[ -z $JAVA_HOME ]] ; then
	echo JAVA_HOME not set!  Set JAVA_HOME and rerun setup.
	exit 1
fi

if [[ -z $DISPLAY ]] ; then
	echo DISPLAY not set!
	exit 1
fi

echo Using JAVA_HOME=$JAVA_HOME
$JAVA_HOME/bin/java -jar jar/totalbackup_@version@.jar

