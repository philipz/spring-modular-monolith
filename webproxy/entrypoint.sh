#!/bin/sh
set -e

# Validate required environment variable
if [ -z "${HYPERDX_API_KEY}" ]; then
  echo "ERROR: HYPERDX_API_KEY environment variable is not set"
  exit 1
fi

# Substitute environment variables in nginx config
envsubst '${HYPERDX_API_KEY}' < /tmp/nginx.conf.template > /etc/nginx/nginx.conf

echo "Environment variables substituted in nginx.conf"
