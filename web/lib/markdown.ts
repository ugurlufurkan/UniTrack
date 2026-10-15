/**
 * Çok basit, bağımlılıksız bir Markdown -> HTML dönüştürücü.
 *
 * Yasal metinlerde başlık, vurgu, güvenli bağlantı ve listeleri destekler.
 * Önce HTML escape edilir; ham HTML veya çalıştırılabilir URL eklenmez.
 */

function escapeHtml(input: string): string {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function inline(text: string): string {
  let out = escapeHtml(text);

  // [metin](url) — sadece http(s) ve mailto şemalarına izin ver.
  out = out.replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+|mailto:[^\s)]+)\)/g, (_m, label, url) => {
    return `<a href="${url}" class="text-blue-600 underline hover:text-blue-800" rel="noopener noreferrer">${label}</a>`;
  });

  out = out.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  out = out.replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '<em>$1</em>');

  return out;
}

export function markdownToHtml(markdown: string): string {
  const lines = markdown.replace(/\r\n/g, '\n').split('\n');
  const htmlParts: string[] = [];

  let listBuffer: string[] = [];
  let paragraphBuffer: string[] = [];

  const flushList = () => {
    if (listBuffer.length === 0) return;
    htmlParts.push(
      `<ul class="list-disc pl-6 space-y-1 my-3">${listBuffer
        .map((item) => `<li>${inline(item)}</li>`)
        .join('')}</ul>`,
    );
    listBuffer = [];
  };

  const flushParagraph = () => {
    if (paragraphBuffer.length === 0) return;
    htmlParts.push(`<p class="my-3 leading-relaxed">${inline(paragraphBuffer.join(' '))}</p>`);
    paragraphBuffer = [];
  };

  for (const rawLine of lines) {
    const line = rawLine.trim();

    if (line === '') {
      flushParagraph();
      flushList();
      continue;
    }

    const headingMatch = /^(#{1,3})\s+(.*)$/.exec(line);
    if (headingMatch) {
      flushParagraph();
      flushList();
      const level = headingMatch[1].length;
      const sizeClass =
        level === 1
          ? 'text-2xl mt-8 mb-3'
          : level === 2
            ? 'text-xl mt-6 mb-2'
            : 'text-lg mt-4 mb-2';
      htmlParts.push(
        `<h${level} class="font-semibold text-slate-900 ${sizeClass}">${inline(headingMatch[2])}</h${level}>`,
      );
      continue;
    }

    const listMatch = /^[-*]\s+(.*)$/.exec(line);
    if (listMatch) {
      flushParagraph();
      listBuffer.push(listMatch[1]);
      continue;
    }

    flushList();
    paragraphBuffer.push(line);
  }

  flushParagraph();
  flushList();

  return htmlParts.join('\n');
}
