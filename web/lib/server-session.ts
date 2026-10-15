import { NextResponse } from 'next/server';

export const ACCESS_COOKIE = 'unitrack_access';
export const REFRESH_COOKIE = 'unitrack_refresh';

export function apiBase() {
  const base = process.env.API_URL || 'http://127.0.0.1:5000/api/v1';
  const url = new URL(base);
  if (!['http:', 'https:'].includes(url.protocol)) throw new Error('Invalid API_URL');
  return base.replace(/\/$/, '');
}

export function setSession(response: NextResponse, access: string, refresh: string) {
  const options = {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax' as const,
    path: '/',
    maxAge: 60 * 60 * 24 * 30,
  };
  response.cookies.set(ACCESS_COOKIE, access, options);
  response.cookies.set(REFRESH_COOKIE, refresh, options);
}

export function clearSession(response: NextResponse) {
  for (const name of [ACCESS_COOKIE, REFRESH_COOKIE])
    response.cookies.set(name, '', {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      path: '/',
      maxAge: 0,
    });
}

export function jsonError(message: string, status: number) {
  return NextResponse.json(
    { success: false, message },
    { status, headers: { 'Cache-Control': 'no-store' } },
  );
}
