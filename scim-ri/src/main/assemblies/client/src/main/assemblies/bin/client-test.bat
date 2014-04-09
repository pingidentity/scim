@echo off
REM Copyright 2011-2014 UnboundID Corp.
REM All Rights Reserved.

setlocal ENABLEDELAYEDEXPANSION

REM Get the tools directory.
set TOOLS_DIR=%~dp0

REM Figure out which Java command to invoke.
if DEFINED UNBOUNDID_JAVA_HOME (
  set JAVA_CMD="%UNBOUNDID_JAVA_HOME%/bin/java"
) else (
  if DEFINED JAVA_HOME (
    set JAVA_CMD="%JAVA_HOME%\bin\java"
  ) else (
    set JAVA_CMD="java"
  )
)

REM Set the classpath.
for /R "%TOOLS_DIR%\..\lib" %%J IN (*.jar) do (
  set CLASSPATH=%%J;!CLASSPATH!
)

%JAVA_CMD% %JAVA_ARGS% -cp "%CLASSPATH%" com.unboundid.scim.ri.client.ClientTest %*

endlocal
