@echo off
title Smart Vehicle Rental Portal - Web Server
cd /d "%~dp0"
echo Starting Smart Vehicle Rental Portal Web Server...
java -cp "bin;lib\mysql-connector-j.jar;src\resources" com.svrp.web.ApiServer
pause
