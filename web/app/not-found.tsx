import Link from 'next/link';
export default function NotFound() {
  return (
    <main className="standalone">
      <div className="panel error-card">
        <span className="eyebrow">404</span>
        <h1>Bu sayfayı bulamadık.</h1>
        <p>Bağlantı değişmiş olabilir. Öğrenci portalından devam edebilirsin.</p>
        <Link className="button" href="/">
          Portala dön
        </Link>
      </div>
    </main>
  );
}
