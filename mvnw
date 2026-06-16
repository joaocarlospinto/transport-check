#!/bin/sh
# Apache Maven Wrapper script, version 3.3.2

if [ -z "$JAVA_HOME" ]; then
  JAVA_CMD="java"
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

MAVEN_PROJECTBASEDIR=$(dirname "$0")
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
  WRAPPER_URL=$(grep "wrapperUrl" "$WRAPPER_PROPERTIES" | cut -d'=' -f2)
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL"
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "Cannot download maven-wrapper.jar: curl and wget not found" >&2
    exit 1
  fi
fi

exec "$JAVA_CMD" \
  "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
  -cp "$WRAPPER_JAR" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
