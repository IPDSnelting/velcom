#!/usr/bin/env bash

# This is a simple script to start two programs (backend and frontend) and
# relay signals to both.

echo "Starting backend"
make run-backend &
# Save its PID so we can relay kill signals
BACKEND_PID="$!"


echo "Starting frontend"
make run-frontend &
# Save its PID so we can relay kill signals
FRONTEND_PID="$!"

# The shell does not pass on exit signals, so we do it manually
trap "kill $FRONTEND_PID && kill $BACKEND_PID" exit INT TERM

# Wait for the subprocesses (= spawned by &) to finish
wait
