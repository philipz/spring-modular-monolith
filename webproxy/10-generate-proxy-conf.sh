#!/bin/sh
set -eu

config_file="/etc/nginx/conf.d/proxy.conf"

cat <<'NGINX_CONF' >"$config_file"
log_format orders_routing '$remote_addr - $remote_user [$time_local] "$request" '
                         '$status $body_bytes_sent "$http_referer" '
                         '"$http_user_agent" backend=$orders_backend_label';

map $http_x_orders_backend $orders_header_override {
    default "";
    ~*^monolith$ monolith;
    ~*^orders$ orders;
    ~*^orders-service$ orders;
}

map $cookie_orders_backend $orders_cookie_override {
    default "";
    ~*^monolith$ monolith;
    ~*^orders$ orders;
    ~*^orders-service$ orders;
}

map "$orders_header_override$orders_cookie_override" $orders_forced_backend {
    ~*orders orders;
    ~*monolith monolith;
    default "";
}

map $request_uri $orders_path_backend {
    ~^/api/orders(/|$) orders;
    ~^/orders(/|$) orders;
    ~^/buy(/|$) orders;
    ~^/cart(/|$) orders;
    default monolith;
}

map "$orders_forced_backend:$orders_path_backend" $orders_backend {
    ~*^orders: orders;
    ~*^monolith: monolith;
    ~*^:orders$ orders;
    ~*^:monolith$ monolith;
    default monolith;
}

map $orders_backend $orders_backend_upstream {
    orders http://orders-service:8091;
    default http://monolith:8080;
}

map $orders_backend $orders_backend_label {
    orders orders-service;
    default monolith;
}

access_log /var/log/nginx/access.log orders_routing;

server {
    listen 80;

    location / {
        resolver 127.0.0.11 [::11] valid=10s;
        proxy_pass $orders_backend_upstream;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $http_host;
        proxy_set_header X-Orders-Backend $orders_backend_label;
        proxy_redirect off;
        add_header X-Orders-Backend $orders_backend_label always;
    }
}
NGINX_CONF

printf '[webproxy] Generated proxy configuration with path-based routing for orders-service.\n' >&2
