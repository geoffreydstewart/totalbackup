#!/bin/sh
echo Building...
ANT_OPTS=-Xmx384m
#ANT_HOME=/usr/share/java/ant-1.8.4
JAVA_HOME=/myopt/java/jdk1.8.0_162
ANT_HOME=/myopt/ant/apache-ant-1.9.2

export ANT_HOME JAVA_HOME
$ANT_HOME/bin/ant -buildfile build_package.xml

#export ANT_HOME
#ant -buildfile build_package.xml
