@echo OFF
echo Building...

set JAVA_HOME=C:\opt\java\jdk1.6.0_26
set ANT_HOME=C:\opt\ant\apache-ant-1.7.1

call %ANT_HOME%\bin\ant.bat -buildfile build_package.xml
pause