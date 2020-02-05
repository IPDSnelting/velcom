#!/usr/bin/env bash

# This is our Continous Deployment deploy script. You may use it as a baseline,
# but it is likely
#  a) Way too specific
#  b) Full of »totally best practices«™

# Fail if any command inside fails
set -e

function copyToServer() {
    echo "Copying $1 to $CD_URL at location $2"
    scp -P "$CD_PORT" "$1" "$CD_USER@$CD_URL:$2"
}

function executeOnServer() {
    echo "Executing on server: '$1'"
    ssh -p "$CD_PORT" "$CD_USER@$CD_URL" "$1"
}

DOCKER_SERVER_DIR="/home/pse_test/velcom/.docker"

executeOnServer "rm -rf $DOCKER_SERVER_DIR"
executeOnServer "mkdir -p $DOCKER_SERVER_DIR"

copyToServer "backend/backend/target/backend.jar" "$DOCKER_SERVER_DIR"
copyToServer "backend/runner/target/runner.jar" "$DOCKER_SERVER_DIR"

copyToServer "docs/nginx-site" "$DOCKER_SERVER_DIR"
copyToServer "docs/nginx-site-single-port" "$DOCKER_SERVER_DIR"
copyToServer "docs/nginx.conf" "$DOCKER_SERVER_DIR"
copyToServer "docs/start-backend-docker.sh" "$DOCKER_SERVER_DIR"

tar -cf dist.tar "frontend/dist"
copyToServer "dist.tar" "$DOCKER_SERVER_DIR"

executeOnServer "tar -xf $DOCKER_SERVER_DIR/dist.tar --directory $DOCKER_SERVER_DIR"
executeOnServer "mv $DOCKER_SERVER_DIR/frontend/dist $DOCKER_SERVER_DIR/dist"
executeOnServer "rmdir $DOCKER_SERVER_DIR/frontend"

# Build it
executeOnServer "sudo /home/pse_test/velcom/deploy_from_docker.sh"

# Restart the docker container :)
executeOnServer "sudo systemctl restart pse-test-velcom.service"
