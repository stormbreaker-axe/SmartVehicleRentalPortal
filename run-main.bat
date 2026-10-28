@echo off
title Smart Vehicle Rental Portal - CLI
cd /d "%~dp0"
echo Starting Smart Vehicle Rental Portal Console...
java -cp "bin;lib\mysql-connector-j.jar;src\resources" com.svrp.Main
pause
