@echo off

setlocal enabledelayedexpansion
set LogFile=C:\API\folders-mstt-synchro-cost-codes-2\CostCodes2\log\test.log
set FolderRoot=C:\API\folders-mstt-synchro-cost-codes-2\CostCodes2
set FolderRequest=%FolderRoot%\requests
set FolderResult=%FolderRoot%\log
set FolderError=%FolderRoot%\errors
set Mask=*.csv


echo Getting system time ...
for /f "tokens=1-9" %%a in ('wmic Path Win32_LocalTime Get Day^,DayOfWeek^,Hour^,Minute^,Month^,Quarter^,Second^,WeekInMonth^,Year ^| find /v ""') do (
	set /a Line += 1
	if "!Line!"=="1" (set VarA=%%a&set VarB=%%b&set VarC=%%c&set VarD=%%d&set VarE=%%e&set VarF=%%f&set VarG=%%g&set VarH=%%h&set VarI=%%i)
	if "!Line!"=="2" (set !VarA!=%%a&set !VarB!=%%b&set !VarC!=%%c&set !VarD!=%%d&set !VarE!=%%e&set !VarF!=%%f&set !VarG!=%%g&set !VarH!=%%h&set !VarI!=%%i)
)
for %%a in (Month Day Hour Minute Second) do (if !%%a! LSS 10 set %%a=0!%%a!)
set TimeStampExecution=%Year%%Month%%Day%-%Hour%%Minute%%Second%

echo Request folder: '%FolderRequest%'.
for %%a in ("%FolderRequest%\%Mask%") do (
	set FullFileName=%%~fa
	echo - '%%~nxa' ...
	if exist "%FolderResult%\%%~nxa" (
		for /f "tokens=2 delims==" %%t in ('wmic.exe DATAFILE WHERE Name^="!FullFileName:\=\\!" GET LastModified /value ^| find /i "LastModified"') do set TimeStampRequest=%%t
		REM Example format: 20140624100709.051399+000
		set TimeStampRequest=!TimeStampRequest:~0,8!-!TimeStampRequest:~8,6!
		set NewFileName=%%~na_!TimeStampExecution!_!TimeStampRequest!%%~xa
		echo ... ERROR; moving to '%FolderError%' as '!NewFileName!'.
		>>"%LogFile%" echo ERROR file '%%~nxa' has been moved to '%FolderError%' folder as '!NewFileName!'.
		 move "!FullFileName!" "%FolderError%\!NewFileName!"
	) else (
		echo ... OK.
	)
)

rem
rem Copyright (c) 2008 SciForma. All right reserved.
rem 
rem mstt-synchro-cost-codes.jar
rem

set JAVA_ARGS=-showversion -Dlog4j.overwrite=true -Xmx1024m -Duse_description=true

java %JAVA_ARGS% -jar "..\lib\mstt-synchro-cost-codes-2.jar" "..\conf\psconnect.properties" C:\API\folders-mstt-synchro-cost-codes-2\CostCodes2\

Exit