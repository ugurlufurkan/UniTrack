import { NextRequest, NextResponse } from 'next/server';
import { resolveEndpoint, isSameOriginMutation } from '@/lib/api-policy.mjs';
import {
  ACCESS_COOKIE,
  REFRESH_COOKIE,
  apiBase,
  clearSession,
  jsonError,
  setSession,
} from '@/lib/server-session';

export const runtime = 'nodejs';

async function handle(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const { path: segments } = await context.params;
  const path = segments.join('/');
  const policy = resolveEndpoint(path, request.method);
  if (!policy) return jsonError('Bu işlem desteklenmiyor.', 404);
  if (!isSameOriginMutation(request)) return jsonError('İstek kaynağı doğrulanamadı.', 403);
  const access = request.cookies.get(ACCESS_COOKIE)?.value;
  const isLogout = path === 'auth/logout';
  if (!policy.public && !access && !isLogout) return jsonError('Lütfen hesabına giriş yap.', 401);

  try {
    const headers = new Headers();
    if (access && !policy.public) headers.set('Authorization', `Bearer ${access}`);
    headers.set('User-Agent', request.headers.get('user-agent') || 'UniTrack Web');
    let body: BodyInit | undefined;
    if (!['GET', 'HEAD'].includes(request.method)) {
      if (isLogout) {
        headers.set('Content-Type', 'application/json');
        body = JSON.stringify({ refreshToken: request.cookies.get(REFRESH_COOKIE)?.value || '' });
      } else {
        const buffer = await request.arrayBuffer();
        if (buffer.byteLength > 11 * 1024 * 1024)
          return jsonError('Dosya boyutu 10 MB sınırını aşıyor.', 413);
        const contentType = request.headers.get('content-type');
        if (contentType) headers.set('Content-Type', contentType);
        if (buffer.byteLength) body = buffer;
      }
    }
    const upstream = await fetch(`${apiBase()}/${path}${request.nextUrl.search}`, {
      method: request.method,
      headers,
      body,
      cache: 'no-store',
      redirect: 'error',
      signal: AbortSignal.timeout(path === 'transcript-import/parse' ? 90000 : 20000),
    });
    if (upstream.status === 204) {
      const response = new NextResponse(null, { status: 204 });
      if (path === 'auth/me') clearSession(response);
      return response;
    }
    const data = await upstream.json();
    const { accessToken, refreshToken, ...safeData } = data;
    const response = NextResponse.json(Array.isArray(data) ? data : safeData, {
      status: upstream.status,
      headers: { 'Cache-Control': 'no-store' },
    });
    if (upstream.ok && accessToken && refreshToken) setSession(response, accessToken, refreshToken);
    if (isLogout || (upstream.ok && ['auth/change-password', 'auth/logout-all'].includes(path)))
      clearSession(response);
    const disposition = upstream.headers.get('content-disposition');
    if (disposition) response.headers.set('Content-Disposition', disposition);
    return response;
  } catch {
    const response = jsonError('Sunucuya ulaşılamıyor. Biraz sonra tekrar dene.', 503);
    if (isLogout) clearSession(response);
    return response;
  }
}

export { handle as GET, handle as POST, handle as PUT, handle as PATCH, handle as DELETE };
