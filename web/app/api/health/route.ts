import { NextResponse } from 'next/server';
import { apiBase } from '@/lib/server-session';

export const dynamic = 'force-dynamic';
export async function GET() {
  try {
    if ((process.env.WEB_PROXY_SECRET?.length || 0) < 32 || !process.env.WEB_CLIENT_IP_HEADER)
      throw new Error('Gateway configuration missing');
    const response = await fetch(`${apiBase().replace(/\/api\/v1$/, '')}/api/health`, {
      cache: 'no-store',
      signal: AbortSignal.timeout(4000),
    });
    if (!response.ok) throw new Error('API unavailable');
    return NextResponse.json({ success: true }, { headers: { 'Cache-Control': 'no-store' } });
  } catch {
    return NextResponse.json(
      { success: false },
      { status: 503, headers: { 'Cache-Control': 'no-store' } },
    );
  }
}
