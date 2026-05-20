FROM node:20-alpine

WORKDIR /app

ENV NODE_ENV=production \
    PORT=8080 \
    UPSTREAM_BASE_URL=http://38.65.93.54:8080 \
    PROXY_TIMEOUT_MS=30000

RUN addgroup -S app && adduser -S app -G app

COPY --chown=app:app package.json server.js ./

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT:-8080}/health" || exit 1

CMD ["node", "server.js"]
