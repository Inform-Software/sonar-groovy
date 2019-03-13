#!/bin/sh

add=

if [ "$SONAR_VERSION" ]
then
  add="$add -Dsonar.version=$SONAR_VERSION"
fi

# Workaround until https://jira.sonarsource.com/browse/TRAVIS-19 is fixed
# See also https://community.sonarsource.com/t/travis-plugin-is-failing-on-external-pull-request/807/7
if [ -n "$SONAR_SCANNER_HOME" ]
then
  add="sonar:sonar $add"
fi

mvn -B -V -e verify $add

