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

function flushList(blocks, listItems) {
  if (!listItems.length) {
    return;
  }

  const itemsHtml = listItems
    .map((item) => `<li style="margin:0 0 6px;">${renderInline(item)}</li>`)
    .join('');
  blocks.push(`<ul style="margin:0 0 12px 18px;padding:0;line-height:1.75;color:#374151;font-size:15px;">${itemsHtml}</ul>`);
  listItems.length = 0;
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
  const listItems = [];

  lines.forEach((rawLine) => {
    const line = rawLine.trim();
    const listMatch = line.match(/^[-*]\s+(.+)$/);
    const orderedMatch = line.match(/^\d+\.\s+(.+)$/);
    const headingMatch = line.match(/^#{1,3}\s+(.+)$/);

    if (!line) {
      flushParagraph(blocks, paragraphLines);
      flushList(blocks, listItems);
      return;
    }

    if (headingMatch) {
      flushParagraph(blocks, paragraphLines);
      flushList(blocks, listItems);
      blocks.push(
        `<p style="margin:0 0 10px;line-height:1.6;color:#111827;font-size:16px;font-weight:700;">${renderInline(headingMatch[1])}</p>`
      );
      return;
    }

    if (listMatch || orderedMatch) {
      flushParagraph(blocks, paragraphLines);
      listItems.push((listMatch || orderedMatch)[1]);
      return;
    }

    flushList(blocks, listItems);
    paragraphLines.push(line);
  });

  flushParagraph(blocks, paragraphLines);
  flushList(blocks, listItems);

  return blocks.join('');
}
