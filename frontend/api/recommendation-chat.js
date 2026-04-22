import { getApiBaseUrl } from './base-url';

function parseEventFrame(frameText) {
  const lines = frameText.split('\n');
  let eventName = '';
  const dataLines = [];

  lines.forEach((line) => {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
      return;
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim());
    }
  });

  if (!eventName) {
    return null;
  }

  const rawData = dataLines.join('\n');
  if (!rawData) {
    return { event: eventName, data: null };
  }

  try {
    return {
      event: eventName,
      data: JSON.parse(rawData)
    };
  } catch (error) {
    return null;
  }
}

function decodeChunk(arrayBuffer) {
  if (typeof TextDecoder !== 'undefined') {
    return new TextDecoder('utf-8').decode(arrayBuffer, { stream: true });
  }

  const bytes = new Uint8Array(arrayBuffer);
  let binary = '';
  const chunkSize = 0x8000;

  for (let index = 0; index < bytes.length; index += chunkSize) {
    binary += String.fromCharCode.apply(null, bytes.subarray(index, index + chunkSize));
  }

  try {
    return decodeURIComponent(escape(binary));
  } catch (error) {
    return binary;
  }
}

function mapNetworkErrorMessage(err, requestUrl) {
  const rawMessage = (err && err.errMsg ? err.errMsg : '') || '';
  const normalized = String(rawMessage).toLowerCase();

  if (normalized.includes('url not in domain list')) {
    return '请求域名未配置：开发阶段请在微信开发者工具关闭域名校验，或在小程序后台配置 https 合法域名';
  }

  if (normalized.includes('ssl') || normalized.includes('certificate')) {
    return 'HTTPS 证书异常，请检查后端证书或改用开发模式调试';
  }

  if (normalized.includes('err_connection_refused')) {
    return `无法连接到 ${requestUrl}。如果是微信开发者工具，请确认后端正在宿主机 8080 监听；如果是真机调试，请把 API 地址改成宿主机局域网 IP。`;
  }

  return rawMessage || '网络异常';
}

export function startRecommendationStream(payload, handlers = {}) {
  const {
    onEvent,
    onError,
    onComplete
  } = handlers;

  const token = wx.getStorageSync('token') || '';
  let rawBuffer = '';
  const requestUrl = `${getApiBaseUrl()}/recommendations/ask/stream`;

  const requestTask = wx.request({
    url: requestUrl,
    method: 'POST',
    enableChunked: true,
    responseType: 'arraybuffer',
    header: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    data: payload,
    success: (res) => {
      if (!(res.statusCode >= 200 && res.statusCode < 300)) {
        const body = res.data || {};
        if (typeof onError === 'function') {
          onError({
            statusCode: res.statusCode,
            code: body.code,
            message: body.message || 'AI 请求失败'
          });
        }
        return;
      }

      if (typeof onComplete === 'function') {
        onComplete();
      }
    },
    fail: (err) => {
      if (typeof onError === 'function') {
        onError({
          statusCode: 0,
          code: 0,
          message: mapNetworkErrorMessage(err, requestUrl)
        });
      }
    }
  });

  requestTask.onChunkReceived((res) => {
    try {
      rawBuffer += decodeChunk(res.data);
      let delimiterIndex = rawBuffer.indexOf('\n\n');

      while (delimiterIndex !== -1) {
        const frameText = rawBuffer.slice(0, delimiterIndex).trim();
        rawBuffer = rawBuffer.slice(delimiterIndex + 2);

        if (frameText) {
          const parsed = parseEventFrame(frameText);
          if (parsed && typeof onEvent === 'function') {
            onEvent(parsed);
          }
        }

        delimiterIndex = rawBuffer.indexOf('\n\n');
      }
    } catch (error) {
      if (typeof onError === 'function') {
        onError({
          statusCode: 0,
          code: 0,
          message: '流式数据解析失败'
        });
      }
    }
  });

  return requestTask;
}
