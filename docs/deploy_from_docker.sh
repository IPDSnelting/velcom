#!/usr/bin/env bash

# fail on first error
set -e

cd "/home/pse_test/velcom/"

ls -lah

if [[ -d .docker ]]; then
    echo "Docker folder found"
else
    echo "Docker folder does not exist"
    exit 1
fi

if [[ -f Dockerfile ]]; then
    echo "Dockerfile found"
else
    echo "Dockerfile not found"
    exit 1
fi

cp Dockerfile .docker

cd .docker

docker build -t velcom-server:latest --build-arg USER_ID=1003 .

# Clean up old images
sudo docker image prune --filter "until=10m" --filter "label=velcom-server" -f
