#!/usr/bin/env bash

# This script deploys velcom to the installation at speedcenter.informatik.kit.edu/velcom.
# To do this, it pings a small server running there with the current credentials

wget --ca-certificate=docs/kit-deployment.crt.pem \
     --quiet \
     --output-document=- \
     --content-on-error \
     --header "X-Deploy-Secret: ${DEPLOY_SECRET}" \
     --header "X-Github-Name: ${GITHUB_NAME}" \
     --header "X-Github-Token: ${GITHUB_TOKEN}" \
     ${ENDPOINT}
