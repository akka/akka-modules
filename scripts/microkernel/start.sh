microkernel=`dirname $0`
java -Xmx512M -Dakka.home=$microkernel -jar $microkernel/start.jar
