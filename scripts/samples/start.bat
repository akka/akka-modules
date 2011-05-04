set SAMPLE=%~dp0..
set AKKA_HOME=%SAMPLE%\\..\\..
set JAVA_OPTS="-Xmx512M"
set AKKA_CLASSPATH="%AKKA_HOME%\\lib\\*"
set SAMPLE_CLASSPATH="%AKKA_CLASSPATH%;%SAMPLE%\\lib\\*;%SAMPLE%\\config"

java %JAVA_OPTS% -cp "%SAMPLE_CLASSPATH%" -Dakka.home=%SAMPLE% akka.kernel.Main