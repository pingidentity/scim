@echo off

rem Copyright 2010-2011 UnboundID Corp.
rem All Rights Reserved.


setlocal
set SCRIPT_DIR=%~dP0

set ANT_HOME=%SCRIPT_DIR%\ant
"%ANT_HOME%\bin\ant" %*

