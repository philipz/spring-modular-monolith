#!/bin/sh
set -e

# Validate required environment variable
if [ -z "${HYPERDX_API_KEY}" ]; then
  echo "ERROR: HYPERDX_API_KEY environment variable is not set"
  exit 1
fi

# Use sed to replace only HYPERDX_API_KEY, preserving all nginx variables
sed "s|\${HYPERDX_API_KEY}|${HYPERDX_API_KEY}|g" /tmp/nginx.conf.template > /etc/nginx/nginx.conf

# Test nginx configuration
nginx -t

echo "Environment variables substituted in nginx.conf"

# Execute the main command (nginx)
exec "$@"
