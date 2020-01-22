#!/usr/bin/env bash
function copyToServer() {
    echo "Copying $1 to $CD_URL at location $2"
    scp -P "$CD_PORT" "$1" "$CD_USER@$CD_URL" "$2"
}

function makeDirOnServer() {
    echo "Making dir $1"
    echo "ssh -p $CD_PORT $CD_USER@$CD_URL 'mkdir -p $1'"
    ssh -p "$CD_PORT" "$CD_USER@$CD_URL" "mkdir -p $1"
}

makeDirOnServer "/home/pse_test/velcom"

copyToServer "backend/backend/target/backend.jar" "/home/pse_test/velcom"
copyToServer "backend/runner/target/runner.jar" "/home/pse_test/velcom"
tar -cf dist.tar "frontend/dist"
copyToServer "dist.tar" "/home/pse_test/velcom"
