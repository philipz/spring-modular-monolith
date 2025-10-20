# Webproxy Dockerfile - Nginx reverse proxy for frontend and backend
FROM nginx:1.29.2-alpine-otel

# Remove default nginx config
RUN rm /etc/nginx/nginx.conf

# Copy custom nginx configuration as template (for envsubst)
COPY webproxy/nginx.conf /tmp/nginx.conf.template

# Copy entrypoint script
COPY webproxy/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Expose HTTP port
EXPOSE 80

# Use custom entrypoint
ENTRYPOINT ["/entrypoint.sh"]

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
