# WhatToEat CloudBase Proxy

This service is the low-cost CloudBase entrypoint for the Mini Program.

It keeps the Mini Program on `wx.cloud.callContainer` while forwarding all API
requests to the existing VPS backend at `38.65.93.54`.

## CloudBase service

Use these settings:

```text
service name: whattoeat-backend
source directory: cloudbase-proxy
Dockerfile: Dockerfile
service port: 8080
```

Environment variables:

```text
PORT=8080
UPSTREAM_BASE_URL=http://38.65.93.54:8080
PROXY_TIMEOUT_MS=30000
```

If the VPS backend is only exposed through HTTPS, set `UPSTREAM_BASE_URL` to the
HTTPS origin instead, for example:

```text
UPSTREAM_BASE_URL=https://38.65.93.54
```

## Local verification

```bash
npm test
PORT=18080 UPSTREAM_BASE_URL=http://38.65.93.54:8080 npm start
curl http://127.0.0.1:18080/health
```
