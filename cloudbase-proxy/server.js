const http = require('node:http');
const https = require('node:https');

const DEFAULT_PORT = 8080;
const DEFAULT_UPSTREAM_BASE_URL = 'https://38.65.93.54';
const DEFAULT_PROXY_TIMEOUT_MS = 120000;

const HOP_BY_HOP_HEADERS = new Set([
  'connection',
  'keep-alive',
  'proxy-authenticate',
  'proxy-authorization',
  'te',
  'trailer',
  'transfer-encoding',
  'upgrade'
]);

const CLOUDBASE_ROUTING_HEADERS = new Set([
  'x-wx-service'
]);

function parsePositiveInteger(value, fallback) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function sanitizeRequestHeaders(headers = {}) {
  const sanitized = {};
  Object.entries(headers).forEach(([name, value]) => {
    const lowerName = name.toLowerCase();
    if (HOP_BY_HOP_HEADERS.has(lowerName) || CLOUDBASE_ROUTING_HEADERS.has(lowerName) || lowerName === 'host') {
      return;
    }
    sanitized[name] = value;
  });
  return sanitized;
}

function sanitizeResponseHeaders(headers = {}) {
  const sanitized = {};
  Object.entries(headers).forEach(([name, value]) => {
    const lowerName = name.toLowerCase();
    if (HOP_BY_HOP_HEADERS.has(lowerName)) {
      return;
    }
    sanitized[name] = value;
  });
  return sanitized;
}

function buildTargetUrl(upstreamBaseUrl, requestUrl) {
  return new URL(requestUrl || '/', upstreamBaseUrl);
}

function sendJson(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(statusCode, {
    'content-type': 'application/json; charset=utf-8',
    'content-length': Buffer.byteLength(body)
  });
  res.end(body);
}

function proxyRequest(req, res, options) {
  const targetUrl = buildTargetUrl(options.upstreamBaseUrl, req.url);
  const client = targetUrl.protocol === 'https:' ? https : http;
  let upstreamStarted = false;

  const upstreamReq = client.request({
    protocol: targetUrl.protocol,
    hostname: targetUrl.hostname,
    port: targetUrl.port || (targetUrl.protocol === 'https:' ? 443 : 80),
    method: req.method,
    path: `${targetUrl.pathname}${targetUrl.search}`,
    headers: sanitizeRequestHeaders(req.headers),
    timeout: options.timeoutMs
  }, (upstreamRes) => {
    upstreamStarted = true;
    res.writeHead(
      upstreamRes.statusCode || 502,
      sanitizeResponseHeaders(upstreamRes.headers)
    );
    upstreamRes.pipe(res);
  });

  upstreamReq.on('timeout', () => {
    upstreamReq.destroy(new Error('upstream timeout'));
  });

  upstreamReq.on('error', () => {
    if (!upstreamStarted && !res.headersSent) {
      sendJson(res, 502, {
        code: 9000,
        message: '后端服务暂时不可用',
        data: null
      });
      return;
    }
    res.destroy();
  });

  req.pipe(upstreamReq);
}

function createProxyServer(config = {}) {
  const upstreamBaseUrl = config.upstreamBaseUrl || process.env.UPSTREAM_BASE_URL || DEFAULT_UPSTREAM_BASE_URL;
  const timeoutMs = parsePositiveInteger(config.timeoutMs || process.env.PROXY_TIMEOUT_MS, DEFAULT_PROXY_TIMEOUT_MS);

  return http.createServer((req, res) => {
    if (req.url === '/health' || req.url === '/health/') {
      sendJson(res, 200, {
        status: 'UP',
        service: 'whattoeat-cloudbase-proxy'
      });
      return;
    }

    proxyRequest(req, res, {
      upstreamBaseUrl,
      timeoutMs
    });
  });
}

function main() {
  const port = parsePositiveInteger(process.env.PORT, DEFAULT_PORT);
  const server = createProxyServer();
  server.listen(port, '0.0.0.0', () => {
    console.log(`whattoeat-cloudbase-proxy listening on ${port}`);
    console.log(`proxying requests to ${process.env.UPSTREAM_BASE_URL || DEFAULT_UPSTREAM_BASE_URL}`);
  });
}

if (require.main === module) {
  main();
}

module.exports = {
  buildTargetUrl,
  createProxyServer,
  sanitizeRequestHeaders,
  sanitizeResponseHeaders
};
