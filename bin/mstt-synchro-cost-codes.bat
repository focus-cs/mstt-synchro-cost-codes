@echo off
setlocal

rem
rem Copyright (c) 2008 SciForma. All right reserved.
rem 
rem mstt-synchro-cost-codes.jar
rem

set JAVA_ARGS=-showversion -Dlog4j.overwrite=true -Xmx1024m -Duse_description=true

java %JAVA_ARGS% -jar "..\lib\mstt-synchro-cost-codes-2.jar" "..\conf\psconnect.properties" "C:\POWER_FRANCE\"

pause