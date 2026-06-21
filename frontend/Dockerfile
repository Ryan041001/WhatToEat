FROM node:20-alpine AS build
WORKDIR /app
COPY . .

FROM nginxinc/nginx-unprivileged:1.29-alpine
USER root
RUN apk add --no-cache --upgrade libxml2
USER 101

COPY --from=build /app /usr/share/nginx/html/miniprogram
COPY docker/index.html /usr/share/nginx/html/index.html

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null || exit 1
