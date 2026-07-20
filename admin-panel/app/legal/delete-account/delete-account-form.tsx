"use client";

import { useActionState } from "react";

import { submitDeletionRequest, type DeleteAccountFormState } from "./actions";

const initialState: DeleteAccountFormState = { status: "idle" };

export default function DeleteAccountForm() {
  const [state, formAction, pending] = useActionState(submitDeletionRequest, initialState);

  if (state.status === "success") {
    return (
      <div className="mt-6 rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-800">
        Talebin alındı. Hesabınla ilişkili veriler kısa süre içinde silinecek.
      </div>
    );
  }

  return (
    <form action={formAction} className="mt-6 space-y-3">
      <div>
        <label htmlFor="email" className="mb-1 block text-sm font-medium text-slate-700">
          Hesap e-postası
        </label>
        <input
          id="email"
          name="email"
          type="email"
          required
          placeholder="ornek@universite.edu.tr"
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      </div>
      <div>
        <label htmlFor="reason" className="mb-1 block text-sm font-medium text-slate-700">
          Neden (opsiyonel)
        </label>
        <textarea
          id="reason"
          name="reason"
          rows={3}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      </div>

      {state.status === "error" && (
        <p className="text-sm text-red-600">{state.message}</p>
      )}

      <button
        type="submit"
        disabled={pending}
        className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60"
      >
        {pending ? "Gönderiliyor…" : "Hesabımı Sil"}
      </button>
    </form>
  );
}
