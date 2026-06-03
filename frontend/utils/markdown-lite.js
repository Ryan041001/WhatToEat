function escapeHtml(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function renderInline(text) {
  const escaped = escapeHtml(text);

  return escaped
    .replace(/`([^`]+)`/g, '<code style="padding:2px 6px;border-radius:8px;background:rgba(15,23,42,0.08);font-family:monospace;color:#B54708;">$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong style="font-weight:700;color:#111827;">$1</strong>');
}

function flushParagraph(blocks, paragraphLines) {
  if (!paragraphLines.length) {
    return;
  }

  const html = paragraphLines
    .map((line) => renderInline(line))
    .join('<br/>');
  blocks.push(`<p style="margin:0 0 10px;line-height:1.75;color:#374151;font-size:15px;">${html}</p>`);
  paragraphLines.length = 0;
}

function flushList(blocks, listState) {
  if (!listState.items.length) {
    return;
  }

  const tagName = listState.type === 'ol' ? 'ol' : 'ul';
  const itemsHtml = listState.items
    .map((item) => `<li style="margin:0 0 6px;">${renderInline(item)}</li>`)
    .join('');
  blocks.push(`<${tagName} style="margin:0 0 12px 18px;padding:0;line-height:1.75;color:#374151;font-size:15px;">${itemsHtml}</${tagName}>`);
  listState.items.length = 0;
}

export function markdownToRichText(markdown) {
  const normalized = String(markdown || '')
    .replace(/\r\n/g, '\n')
    .trim();

  if (!normalized) {
    return '';
  }

  const lines = normalized.split('\n');
  const blocks = [];
  const paragraphLines = [];
  const listState = {
    type: 'ul',
    items: []
  };

  lines.forEach((rawLine) => {
    const line = rawLine.trim();
    const listMatch = line.match(/^[-*]\s+(.+)$/);
    const orderedMatch = line.match(/^\d+\.\s+(.+)$/);
    const headingMatch = line.match(/^#{1,3}\s+(.+)$/);
    const quoteMatch = line.match(/^>\s+(.+)$/);

    if (!line) {
      flushParagraph(blocks, paragraphLines);
      flushList(blocks, listState);
      return;
    }

    if (headingMatch) {
      flushParagraph(blocks, paragraphLines);
      flushList(blocks, listState);
      blocks.push(
        `<p style="margin:0 0 10px;line-height:1.6;color:#111827;font-size:16px;font-weight:700;">${renderInline(headingMatch[1])}</p>`
      );
      return;
    }

    if (quoteMatch) {
      flushParagraph(blocks, paragraphLines);
      flushList(blocks, listState);
      blocks.push(
        `<p style="margin:0 0 12px;padding-left:10px;border-left:3px solid #fed7aa;line-height:1.7;color:#6b7280;font-size:14px;">${renderInline(quoteMatch[1])}</p>`
      );
      return;
    }

    if (listMatch || orderedMatch) {
      flushParagraph(blocks, paragraphLines);
      const nextType = orderedMatch ? 'ol' : 'ul';
      if (listState.items.length && listState.type !== nextType) {
        flushList(blocks, listState);
      }
      listState.type = nextType;
      listState.items.push((listMatch || orderedMatch)[1]);
      return;
    }

    flushList(blocks, listState);
    paragraphLines.push(line);
  });

  flushParagraph(blocks, paragraphLines);
  flushList(blocks, listState);

  return blocks.join('');
}
