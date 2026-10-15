import Link from 'next/link';
export default function Page() {
  return (
    <article className="panel legal-article">
      <span className="eyebrow">UNITRACK</span>
      <h1>Yasal bilgiler</h1>
      <div className="legal-actions">
        <Link className="button secondary" href="/legal/privacy">
          Gizlilik politikası
        </Link>
        <Link className="button secondary" href="/legal/terms">
          Kullanım koşulları
        </Link>
        <Link className="button secondary" href="/legal/delete-account">
          Hesap silme
        </Link>
      </div>
    </article>
  );
}
