#!/bin/sh

if [ ! -e "${JAVA_HOME}" ]; then
  echo "Unable to stop RestNext, JAVA_HOME must be set"
  exit 1
fi

for i in `ls ./lib/*.jar`
do
  DB_CLASSPATH=${DB_CLASSPATH}:${i}
done

#DB_CLASSPATH=${DB_CLASSPATH}:./etc/

${JAVA_HOME}/bin/java -cp ".:${DB_CLASSPATH}" -Xmx1g org.restnext.server.Server

