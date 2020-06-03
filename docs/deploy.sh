#!/usr/bin/env bash

# This is our Continous Deployment deploy script.
# It just calls out to the server and requests the docker image to be pulled
# again and restarts the service.

# Fail if any command inside fails
set -e

function executeOnServer() {
    echo "Executing on server: '$1'"
    ssh -p "$CD_PORT" "$CD_USER@$CD_URL" "$1"
}

function setup() {
    ##
    ## Create the SSH directory and give it the right permissions
    ##
    mkdir -p ~/.ssh
    chmod 700 ~/.ssh

    ##
    ## Install ssh-agent if not already installed, it is required by Docker.
    ## (change apt-get to yum if you use an RPM-based image)
    ##
    which ssh-agent || ( apt update -y && apt install openssh-client -y )

    ##
    ## Run ssh-agent (inside the build environment)
    ##
    eval $(ssh-agent -s)

    ##
    ## Add the SSH key stored in SSH_PRIVATE_KEY variable to the agent store
    ## We're using tr to fix line endings which makes ed25519 keys work
    ## without extra base64 encoding.
    ## https://gitlab.com/gitlab-examples/ssh-private-key/issues/1#note_48526556
    ##
    echo "$SSH_PRIVATE_KEY" | tr -d '\r' | ssh-add -

    ## Setup host key verification
    echo "$SSH_KNOWN_HOSTS" > ~/.ssh/known_hosts
    chmod 644 ~/.ssh/known_hosts
}

if [ -z "$1" -o -z "$2" ]; then
    echo "Usage: $0 <github actor name> <github packes access token>"
    exit 1
fi

GITHUB_NAME="$1"
GITHUB_TOKEN="$2"

setup

# Login to github registry
executeOnServer "echo "$GITHUB_TOKEN" | sudo docker login docker.pkg.github.com -u "$GITHUB_NAME" --password-stdin"

# Build it
executeOnServer "sudo /home/velcom/update-docker-image.sh"

# Restart the docker container :)
executeOnServer "sudo systemctl restart velcom.service"
