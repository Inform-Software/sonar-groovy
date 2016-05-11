#!/bin/bash
set -euo pipefail
echo "Running $TEST with SQ=$SQ_VERSION"

case "$TEST" in
  plugin|ruling)

  mvn package -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  cd its/$TEST
  mvn -DjavaVersion="LATEST_RELEASE" -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false test
  ;;

  *)
  echo "Unexpected TEST mode: $TEST"
  exit 1
  ;;
esac
