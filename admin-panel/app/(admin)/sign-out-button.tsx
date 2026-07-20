"use client";

import { useRouter } from "next/navigation";

import { signOut } from "@/lib/auth-client";

export default function SignOutButton() {
  const router = useRouter();

  return (
    <button
      onClick={async () => {
        await signOut();
        router.replace("/login");
        router.refresh();
      }}
      className="rounded-md border border-slate-300 px-2.5 py-1 text-xs hover:bg-slate-100"
    >
      Çıkış
    </button>
  );
}
