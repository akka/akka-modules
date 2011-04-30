set MICROKERNEL=%~dp0
java -Xmx512M -Dakka.home=%MICROKERNEL% -jar "%MICROKERNEL%\start.jar"
