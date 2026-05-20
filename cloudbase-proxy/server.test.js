const assert = require('node:assert/strict');
const http = require('node:http');
const test = require('node:test');

const { createProxyServer } = require('./server');

function listen(server) {
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      server.off('error', reject);
      resolve(server.address().port);
    });
  });
}

function close(server) {
  return new Promise((resolve, reject) => {
    server.close((error) => (error ? reject(error) : resolve()));
  });
}

function request(port, path, options = {}) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      host: '127.0.0.1',
      port,
      path,
      method: options.method || 'GET',
      headers: options.headers || {}
    }, (res) => {
      const chunks = [];
      res.on('data', (chunk) => chunks.push(chunk));
      res.on('end', () => {
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body: Buffer.concat(chunks).toString('utf8')
        });
      });
    });
    req.on('error', reject);
    if (options.body) {
      req.write(options.body);
    }
    req.end();
  });
}

test('health endpoint reports proxy readiness without calling upstream', async () => {
  const proxy = createProxyServer({ upstreamBaseUrl: 'http://127.0.0.1:1' });
  const proxyPort = await listen(proxy);

  try {
    const response = await request(proxyPort, '/health');

    assert.equal(response.statusCode, 200);
    assert.deepEqual(JSON.parse(response.body), {
      status: 'UP',
      service: 'whattoeat-cloudbase-proxy'
    });
  } finally {
    await close(proxy);
  }
});

test('proxies method, path, query, body, and useful headers to upstream', async () => {
  let captured;
  const upstream = http.createServer((req, res) => {
    const chunks = [];
    req.on('data', (chunk) => chunks.push(chunk));
    req.on('end', () => {
      captured = {
        method: req.method,
        url: req.url,
        authorization: req.headers.authorization,
        contentType: req.headers['content-type'],
        serviceHeader: req.headers['x-wx-service'],
        body: Buffer.concat(chunks).toString('utf8')
      };
      res.writeHead(201, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ code: 0, data: { ok: true } }));
    });
  });
  const upstreamPort = await listen(upstream);
  const proxy = createProxyServer({ upstreamBaseUrl: `http://127.0.0.1:${upstreamPort}` });
  const proxyPort = await listen(proxy);

  try {
    const response = await request(proxyPort, '/api/v1/recommendations/ask?debug=1', {
      method: 'POST',
      headers: {
        authorization: 'Bearer token-1',
        'content-type': 'application/json',
        'x-wx-service': 'whattoeat-backend'
      },
      body: '{"question":"吃什么"}'
    });

    assert.equal(response.statusCode, 201);
    assert.deepEqual(JSON.parse(response.body), { code: 0, data: { ok: true } });
    assert.deepEqual(captured, {
      method: 'POST',
      url: '/api/v1/recommendations/ask?debug=1',
      authorization: 'Bearer token-1',
      contentType: 'application/json',
      serviceHeader: undefined,
      body: '{"question":"吃什么"}'
    });
  } finally {
    await close(proxy);
    await close(upstream);
  }
});

test('returns a unified 502 response when upstream is unavailable', async () => {
  const proxy = createProxyServer({ upstreamBaseUrl: 'http://127.0.0.1:1' });
  const proxyPort = await listen(proxy);

  try {
    const response = await request(proxyPort, '/api/v1/restaurants/nearby');

    assert.equal(response.statusCode, 502);
    assert.deepEqual(JSON.parse(response.body), {
      code: 9000,
      message: '后端服务暂时不可用',
      data: null
    });
  } finally {
    await close(proxy);
  }
});
