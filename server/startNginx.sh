#!/usr/bin/env bash

echo "Starting Nginx server on 18443 and use the certificates in certs/server/*"

SERVICE="nginx-app"
CONF_DIR=$(pwd)/conf
CERT_DIR=$(pwd)/../certs

echo "Conf dir : ${CONF_DIR}"
echo "Cert dir : ${CERT_DIR}"

if [[ "$(docker inspect -f {{.State.Running}} ${SERVICE} 2> /dev/null)" == "true" ]]; then
  docker stop "${SERVICE}"
fi
if [[ "$(docker inspect -f {{.State.Running}} ${SERVICE} 2> /dev/null)" == "false" ]]; then
  docker rm "${SERVICE}"
fi

docker run \
    --detach \
    --publish 18443:443 \
    --volume "${CONF_DIR}":/etc/nginx/conf.d/:ro \
    --volume "${CERT_DIR}/server/server.pem":/etc/nginx/certs/server.pem:ro \
    --volume "${CERT_DIR}/server/server.key":/etc/nginx/certs/server.key:ro \
    --volume "${CERT_DIR}/appRootCA.pem":/etc/nginx/client_certs/appRootCA.pem:ro \
    --name "${SERVICE}" \
    nginx
