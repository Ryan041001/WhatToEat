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
UPSTREAM_BASE_URL=https://38.65.93.54
PROXY_TIMEOUT_MS=120000
```

The VPS only exposes the backend through Nginx on HTTPS. Do not point this
proxy at `http://38.65.93.54:8080`; that port is bound to `127.0.0.1` on the
VPS and is not reachable from CloudBase.

## Local verification

```bash
npm test
PORT=18080 UPSTREAM_BASE_URL=http://38.65.93.54:8080 npm start
curl http://127.0.0.1:18080/health
```
