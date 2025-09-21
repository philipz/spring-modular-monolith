FROM opentracing/nginx-opentracing:0.41.0-alpine


RUN rm -rf /etc/nginx/conf.d/*

COPY nginx.conf /etc/nginx/nginx.conf
COPY zipkin.json /etc/nginx/zipkin.json
