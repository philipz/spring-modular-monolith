#!/bin/sh
set -eu

ORDERS_SERVICE_PERCENT=${ORDERS_SERVICE_PERCENT:-0}

case "$ORDERS_SERVICE_PERCENT" in
    ''|*[!0-9]*)
        echo "ORDERS_SERVICE_PERCENT must be an integer between 0 and 100 (was '$ORDERS_SERVICE_PERCENT')" >&2
        exit 1
        ;;
esac

if [ "$ORDERS_SERVICE_PERCENT" -gt 100 ]; then
    echo "ORDERS_SERVICE_PERCENT must be <= 100 (was '$ORDERS_SERVICE_PERCENT')" >&2
    exit 1
fi

config_file="/etc/nginx/conf.d/proxy.conf"

cat <<'NGINX_BASE' > "$config_file"
log_format orders_rollout '$remote_addr - $remote_user [$time_local] "$request" '
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
NGINX_BASE

case "$ORDERS_SERVICE_PERCENT" in
    0)
        cat <<'NGINX_ZERO' >> "$config_file"
map "$orders_forced_backend" $orders_backend {
    ~*orders orders;
    ~*monolith monolith;
    default monolith;
}
NGINX_ZERO
        ;;
    100)
        cat <<'NGINX_HUNDRED' >> "$config_file"
map "$orders_forced_backend" $orders_backend {
    ~*orders orders;
    ~*monolith monolith;
    default orders;
}
NGINX_HUNDRED
        ;;
    *)
        cat <<'NGINX_SPLIT_HEAD' >> "$config_file"
split_clients "${remote_addr}${msec}${request_uri}" $orders_split_bucket {
NGINX_SPLIT_HEAD
        printf '    %s%% orders;\n' "$ORDERS_SERVICE_PERCENT" >> "$config_file"
        cat <<'NGINX_SPLIT_TAIL' >> "$config_file"
    * monolith;
}
map "$orders_forced_backend$orders_split_bucket" $orders_backend {
    ~*orders orders;
    ~*monolith monolith;
    default monolith;
}
NGINX_SPLIT_TAIL
        ;;
esac

cat <<'NGINX_TAIL' >> "$config_file"

map $orders_backend $orders_backend_upstream {
    orders http://orders-service:8091;
    default http://monolith:8080;
}

map $orders_backend $orders_backend_label {
    orders orders-service;
    default monolith;
}

access_log /var/log/nginx/access.log orders_rollout;

server {
    listen 80;

    location ~ ^/(orders|buy|cart) {
        resolver 127.0.0.11 [::11] valid=10s;
        proxy_pass $orders_backend_upstream;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $http_host;
        proxy_set_header X-Orders-Selected-Backend $orders_backend_label;
        add_header X-Orders-Backend $orders_backend_label;
        proxy_redirect off;
    }

    location / {
        resolver 127.0.0.11 [::11] valid=10s;
        proxy_pass http://monolith:8080;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $http_host;
        proxy_redirect off;
    }
}
NGINX_TAIL

printf '[webproxy] Orders rollout percentage set to %s%%\n' "$ORDERS_SERVICE_PERCENT" >&2
