#!/usr/bin/env bash
function copyToServer() {
    echo "Copying $1 to $CD_URL at location $2"
    scp -P "$CD_PORT" "$0" "$CD_USER@$CD_URL" "$1"
}

function makeDirOnServer() {
    echo "Making dir $1"
    ssh -p "$CD_PORT" "$CD_USER@$CD_URL" "mkdir -p $0"
}

ls .
ls -lah .
for file in *; do
    echo "$file:"
    ls -lah $file
done
ls -lah backend/backend || ls -lah backend
ls -lah backend/backend/target

makeDirOnServer "/home/pse_test/velcom"

copyToServer "backend/backend/target/backend.jar" "/home/pse_test/velcom"
copyToServer "backend/runner/target/backend.jar" "/home/pse_test/velcom"
tar -cf dist.tar "frontend/dist"
copyToServer "dist.tar" "/home/pse_test/velcom"
