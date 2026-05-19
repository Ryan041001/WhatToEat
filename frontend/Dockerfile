FROM node:20-alpine AS build
WORKDIR /app
COPY . .

FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY --from=build /app /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null || exit 1
