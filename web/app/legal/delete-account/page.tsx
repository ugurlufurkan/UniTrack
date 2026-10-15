import Link from 'next/link';
export default function Page() {
  return (
    <article className="panel legal-article">
      <span className="eyebrow">HESAP YÖNETİMİ</span>
      <h1>UniTrack hesabını sil</h1>
      <p>Uygulamayı yüklemeden hesabını ve ilişkili verilerini web üzerinden silebilirsin.</p>
      <div className="legal-content">
        <ol className="list-decimal pl-5 space-y-3">
          <li>UniTrack mobil uygulamasında kullandığın hesabınla web portalına giriş yap.</li>
          <li>
            Menüden <strong>Ayarlar</strong> sayfasını aç.
          </li>
          <li>
            <strong>Hesabımı kalıcı olarak sil</strong> seçeneğini kullan ve e-posta adresini
            yazarak onayla.
          </li>
        </ol>
        <h2>Hangi veriler silinir?</h2>
        <p>
          Hesabın ile ilişkili dönemler, dersler, notlar, devamsızlık kayıtları, takvim
          etkinlikleri, görevler ve oturum kayıtları silinir. Kullanıcıyla bağlantısı kaldırılmış
          güvenlik denetim kayıtları saklanabilir. Bu işlem geri alınamaz.
        </p>
        <p>
          Hesabına erişemiyorsan şifreni sıfırlayabilirsin. Google hesabıyla giriş yaptıysan web
          portalındaki Google girişini kullan.
        </p>
      </div>
      <div className="legal-actions">
        <Link className="button" href="/settings">
          Hesap ayarlarıma git
        </Link>
        <Link className="button secondary" href="/forgot-password">
          Şifremi sıfırla
        </Link>
      </div>
    </article>
  );
}
