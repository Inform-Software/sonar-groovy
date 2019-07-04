#!/bin/sh

add=

if [ "$SONAR_VERSION" ]
then
  add="$add -Dsonar.version=$SONAR_VERSION"

# Workaround until https://jira.sonarsource.com/browse/TRAVIS-19 is fixed
# See also https://community.sonarsource.com/t/travis-plugin-is-failing-on-external-pull-request/807/7
elif [ -n "$SONAR_SCANNER_HOME" ]
then
  # Only run SonarQube analysis on one Travis-CI matrix configuration
  # (namely: empty $SONAR_VERSION and Java 8
  java -Xmx32m -version 2>&1 | grep -q 1.8.0 && add="sonar:sonar $add"
fi

mvn -B -V -e -Dstyle.color=always verify $add

