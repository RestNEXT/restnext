@echo off
SETLOCAL enabledelayedexpansion
set DBCLASSPATH=
for /R ./lib %%a in (*.jar) do (
  set DBCLASSPATH=%%a;!DBCLASSPATH!
)

if not defined JAVA_HOME (
  echo Unable to start RestNext, JAVA_HOME must be set
  GOTO :END
) else (
  GOTO :START
)
 
:START

#set DBCLASSPATH=%DBCLASSPATH%;./etc/

"%JAVA_HOME%/bin/java" -cp %DBCLASSPATH% org.restnext.server.Server

:END
