import { NextRequest, NextResponse } from 'next/server';
import { createHash } from 'node:crypto';
import { isSameOriginMutation } from '@/lib/api-policy.mjs';
import { apiBase, REFRESH_COOKIE, clearSession, jsonError, setSession } from '@/lib/server-session';

type RefreshResult = { status: number; accessToken?: string; refreshToken?: string };
// Coalesce requests within one server; browser Web Locks also serialize across tabs.
const pending = new Map<string, Promise<RefreshResult>>();

export async function POST(request: NextRequest) {
  if (!isSameOriginMutation(request)) return jsonError('İstek kaynağı doğrulanamadı.', 403);
  const token = request.cookies.get(REFRESH_COOKIE)?.value;
  if (!token) return jsonError('Oturumun sona erdi. Tekrar giriş yap.', 401);
  const key = createHash('sha256').update(token).digest('hex');
  try {
    let operation = pending.get(key);
    if (!operation) {
      operation = (async () => {
        const upstream = await fetch(`${apiBase()}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken: token }),
          cache: 'no-store',
          signal: AbortSignal.timeout(20000),
        });
        const data = await upstream.json();
        return {
          status: upstream.status,
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
        };
      })();
      pending.set(key, operation);
      operation
        .finally(() => {
          setTimeout(() => pending.delete(key), 5000);
        })
        .catch(() => {});
    }
    const result = await operation;
    if (result.status >= 500) return jsonError('Oturum servisine ulaşılamıyor. Tekrar dene.', 503);
    if (!result.accessToken || !result.refreshToken || result.status !== 200) {
      const response = jsonError('Oturumun sona erdi. Tekrar giriş yap.', 401);
      clearSession(response);
      return response;
    }
    const response = NextResponse.json(
      { success: true },
      { headers: { 'Cache-Control': 'no-store' } },
    );
    setSession(response, result.accessToken, result.refreshToken);
    return response;
  } catch {
    return jsonError('Oturum servisine ulaşılamıyor. Tekrar dene.', 503);
  }
}
