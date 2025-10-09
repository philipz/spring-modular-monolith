FROM nginx:1.29.2-alpine-otel

RUN rm -rf /etc/nginx/conf.d/*

# Create a startup script to substitute environment variables
COPY nginx.conf /tmp/nginx.conf.template
COPY entrypoint.sh /docker-entrypoint.d/40-envsubst-nginx-conf.sh
RUN chmod +x /docker-entrypoint.d/40-envsubst-nginx-conf.sh
