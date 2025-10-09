#!/bin/sh
set -e

# Substitute environment variables in nginx config
envsubst '${HYPERDX_API_KEY}' < /tmp/nginx.conf.template > /etc/nginx/nginx.conf

echo "Environment variables substituted in nginx.conf"
