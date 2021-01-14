#!/bin/sh

#---------------------------------------------------
#
# ANT ENVIRONMENT
#
#JAVA_HOME=/usr/local/java/jdk1.5.0_14
JAVA_HOME=@javaHome@
ANT_HOME=../3rdparties/apache-ant-1.9.2
#
export JAVA_HOME ANT_HOME

chmod 755 $ANT_HOME/bin/ant

echo ==========================================
echo Starting Ant build file
echo ==========================================

echo Logging output to $PWD/totalbackup.log

$ANT_HOME/bin/ant -buildfile build.xml 2> totalbackup.log 1>&2 &

