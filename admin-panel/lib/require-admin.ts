import { headers } from "next/headers";
import { redirect } from "next/navigation";

import { auth } from "@/lib/auth";

/** Sayfa/server action'ın başında çağır: girişi yoksa /login'e atar. */
export async function requireAdmin() {
  const session = await auth.api.getSession({ headers: await headers() });

  if (!session) {
    redirect("/login");
  }

  return session;
}
