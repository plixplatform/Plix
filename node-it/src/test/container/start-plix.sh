#!/bin/bash

trap 'kill -TERM $PID' TERM INT
echo Options: $PLIX_OPTS
java $PLIX_OPTS -cp "/opt/plix/lib/*" com.plixplatform.Application /opt/plix/template.conf &
PID=$!
wait $PID
trap - TERM INT
wait $PID
EXIT_STATUS=$?
