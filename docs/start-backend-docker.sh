#!/usr/bin/env bash

# Start nginx for routing
nginx &

java -jar /home/velcom/velcom.jar server "$1" &
JAVA_PID="$!"

# The shell does not pass on exit signals, so we do it manually
# This ensures java is killed properly and the container can restart.
trap "kill $JAVA_PID" exit INT TERM

# Wait for the subprocesses (& spawned) to finish
wait
