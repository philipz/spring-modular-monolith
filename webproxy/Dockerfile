FROM nginx:alpine
RUN rm /etc/nginx/conf.d/*
COPY 10-generate-proxy-conf.sh /docker-entrypoint.d/10-generate-proxy-conf.sh
