/**
 * Datastar SSE response helpers.
 *
 * Datastar expects text/event-stream responses with specific event types:
 * - datastar-patch-elements  : morph/replace DOM elements
 * - datastar-patch-signals   : update reactive signals
 */

export function sseEvent(eventType: string, dataLines: string[]): string {
  return `event: ${eventType}\n${dataLines.map(l => `data: ${l}`).join('\n')}\n\n`;
}

export function patchElements(
  html: string,
  options: { selector?: string; mode?: string } = {}
): string {
  const lines: string[] = [];
  if (options.selector) lines.push(`selector ${options.selector}`);
  if (options.mode) lines.push(`mode ${options.mode}`);
  // SSE data lines can't contain newlines - collapse to single line
  lines.push(`elements ${html.replace(/\n\s*/g, '')}`);
  return sseEvent('datastar-patch-elements', lines);
}

export function patchSignals(signals: Record<string, unknown>): string {
  return sseEvent('datastar-patch-signals', [`signals ${JSON.stringify(signals)}`]);
}

export function redirect(url: string): string {
  return sseEvent('datastar-patch-elements', [
    'selector body',
    'mode append',
    `elements <script>setTimeout(() => window.location.href = '${url}')</script>`
  ]);
}

export function sseResponse(...eventStrings: string[]): Response {
  return new Response(eventStrings.join(''), {
    status: 200,
    headers: {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive'
    }
  });
}

export async function parseSignals(request: Request): Promise<Record<string, unknown>> {
  const url = new URL(request.url);
  const datastarParam = url.searchParams.get('datastar');

  if (datastarParam) {
    return JSON.parse(datastarParam);
  }

  if (request.method === 'POST') {
    const body = await request.text();
    if (body && body.trim()) {
      return JSON.parse(body);
    }
  }

  return {};
}
