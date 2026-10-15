'use client';
import Link from 'next/link';
export default function ErrorPage({ reset }: { reset: () => void }) {
  return (
    <main className="standalone">
      <div className="panel error-card">
        <span className="eyebrow">BİR AKSİLİK OLDU</span>
        <h1>Bu sayfa yüklenemedi.</h1>
        <p>Verilerin korunuyor. Sayfayı yeniden yükleyerek devam edebilirsin.</p>
        <button className="button" onClick={reset}>
          Tekrar dene
        </button>
        <Link className="button secondary" href="/">
          Ana sayfaya dön
        </Link>
      </div>
    </main>
  );
}
