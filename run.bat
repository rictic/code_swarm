@ECHO OFF
REM run.bat : code_swarm launching script
REM need the config file as first parameter

REM TODO : take care of multiple parameters
SET params="%1"
set default_config="data/sample.config"

REM basic command line parameters check
IF %params%=="" (
    REM asking user for a config file
    ECHO Specify a config file, or type
    SET /p params="ENTER for default one [%default_config%] : "
)
IF %params%=="" (
    SET params=%default_config%
)

REM help requested ?
IF "%1"=="/?" goto help
IF "%1"=="/H" goto help
IF "%1"=="-h" goto help
IF "%1"=="--help" goto help

REM checking for code_swarm java binaries
IF NOT EXIST dist\code_swarm.jar (
    echo no code_swarm binaries !
    echo needing to build it with 'ant' and 'javac' java-sdk
    echo auto-trying the ant command...
    call ant
)

REM running
REM echo %params%
echo code_swarm project !
java -Xmx1000m -classpath dist/code_swarm.jar;lib/core.jar;lib/xml.jar;. code_swarm %params%

exit


:help
    REM if help needed, print it and exit
    echo usage: run configfile
    echo    data/sample.config  is the default config file"
   
   