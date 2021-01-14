@echo off


rem --------------------------------------
rem 
rem ANT ENVIRONMENT :
rem 
rem set JAVA_HOME=C:\jdk1.6.0_14
set JAVA_HOME=@javaHome@

set ANT_HOME=..\3rdparties\apache-ant-1.9.2
rem 
rem --------------------------------------


echo ==============================
echo Starting Ant build file
echo ==============================

echo Logging output to %cd%\totalbackup.log

if exist "%JAVA_HOME%" goto startAnt
echo JAVA_HOME not found. Please set it first.
goto exit

:startAnt
%ANT_HOME%\bin\ant.bat -buildfile build.xml Distribute 2> totalbackup.log 1>&2
goto exit

:exit
pause