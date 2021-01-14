@echo off

REM set JAVA_HOME=C:\jdk1.6.0_14
REM If the JAVA_HOME contains spaces, put quotes around the path above and remove the quotes
REM around the %JAVA_HOME% just below

if "%JAVA_HOME%" == "" goto nojava
goto exec

:exec
echo Using JAVA_HOME=%JAVA_HOME%
%JAVA_HOME%\bin\java -jar jar\totalbackup_@version@.jar
goto end

:nojava
echo JAVA_HOME not set!  Set JAVA_HOME and rerun setup.
pause
goto end

:end
