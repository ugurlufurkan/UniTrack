import { createHmac } from 'node:crypto';
import { isIP } from 'node:net';
import type { NextRequest } from 'next/server';

export function signGateway(request: NextRequest, headers: Headers, path: string) {
  const secret = process.env.WEB_PROXY_SECRET;
  const ipHeader = process.env.WEB_CLIENT_IP_HEADER;
  if (!secret || !ipHeader) {
    if (process.env.NODE_ENV === 'production')
      throw new Error('Configure WEB_PROXY_SECRET and WEB_CLIENT_IP_HEADER');
    return;
  }
  // The ingress must overwrite this header. For X-Forwarded-For the rightmost
  // address is the direct client appended by our configured ingress.
  const ip = request.headers.get(ipHeader)?.split(',').at(-1)?.trim();
  if (!ip || !isIP(ip)) throw new Error('Trusted client address is unavailable');
  const timestamp = String(Date.now());
  headers.set('x-unitrack-client-ip', ip);
  headers.set('x-unitrack-sent-at', timestamp);
  headers.set(
    'x-unitrack-signature',
    createHmac('sha256', secret)
      .update(`${timestamp}\n${ip}\n${request.method}\n${path}`)
      .digest('hex'),
  );
}
