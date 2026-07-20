import Link from "next/link";

import DeleteAccountForm from "./delete-account-form";

export const metadata = {
  title: "Hesap Silme Talebi — UniTrack",
};

export default function DeleteAccountPage() {
  return (
    <div className="min-h-screen bg-white">
      <div className="mx-auto max-w-xl px-6 py-12">
        <Link href="/legal" className="text-sm text-slate-400 hover:text-slate-600">
          ← Tüm sayfalar
        </Link>
        <h1 className="mt-4 text-2xl font-semibold text-slate-900">Hesap Silme Talebi</h1>
        <p className="mt-3 text-sm leading-relaxed text-slate-600">
          UniTrack hesabını uygulamayı silmeden veya telefonun olmadan da kapatabilirsin.
          Aşağıya hesabınla ilişkili e-posta adresini yaz — talebini aldıktan sonra hesabınla
          ilişkili tüm verileri (ders/not/GANO kayıtları, devamsızlık, görev ve takvim
          verileri dahil) makul bir süre içinde sileriz. Bu süreç hakkında ayrıntı için{" "}
          <Link href="/legal/privacy" className="text-blue-600 underline hover:text-blue-800">
            Gizlilik Politikası
          </Link>{" "}
          sayfasına bakabilirsin.
        </p>
        <p className="mt-2 text-sm leading-relaxed text-slate-600">
          Uygulama hâlâ telefonunda kuruluysa, aynı işlemi Ayarlar → Hesabımı Sil üzerinden de
          yapabilirsin.
        </p>

        <DeleteAccountForm />
      </div>
    </div>
  );
}
