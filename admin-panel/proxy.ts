import { NextRequest, NextResponse } from "next/server";
import { getSessionCookie } from "better-auth/cookies";

/**
 * Sadece "iyimser" (optimistic) bir kontrol: cookie var mı yok mu bakar,
 * DB'ye gitmez (Edge Runtime'da hızlı çalışsın diye). Asıl yetki kontrolü
 * her korumalı sayfada/server action'da auth.api.getSession ile TEKRAR
 * yapılıyor — bkz. lib/require-admin.ts. Bu middleware sadece "hiç
 * girmemiş birini login'e daha erken yönlendirsin" için var.
 *
 * NOT: `/legal/*` BİLİNÇLİ olarak buraya eklenmedi — Gizlilik Politikası
 * ve Hesap Silme Talebi sayfalarının Google Play'in istediği gibi girişsiz,
 * herkese açık kalması gerekiyor.
 */
export function proxy(request: NextRequest) {
  const sessionCookie = getSessionCookie(request, {
    cookieName: "session_token",
    cookiePrefix: "better-auth",
  });

  if (!sessionCookie) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/announcements/:path*",
    "/faqs/:path*",
    "/tips/:path*",
    "/pages/:path*",
    "/deletion-requests/:path*",
    "/",
  ],
};
