#!/bin/sh

add=

if [ "$SONAR_VERSION" ]
then
  add="$add -Dsonar.version=$SONAR_VERSION"
else
  # Only run SonarQube analysis on one matrix configuration
  # (namely: empty $SONAR_VERSION and Java 11
  java -Xmx32m -version 2>&1 | grep -q "11\.0" && add="sonar:sonar $add"
fi

export MAVEN_OPTS="-Djansi.force=true"
# shellcheck disable=2086
mvn --batch-mode --no-transfer-progress --errors -Dstyle.color=always verify $add

