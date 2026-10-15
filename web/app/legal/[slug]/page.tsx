import { notFound } from 'next/navigation';
import { apiBase } from '@/lib/server-session';
import { markdownToHtml } from '@/lib/markdown';

export const dynamic = 'force-dynamic';
export default async function Page({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  if (!/^[a-z0-9-]{1,100}$/.test(slug)) notFound();
  let response: Response;
  try {
    response = await fetch(`${apiBase()}/content/pages/${slug}`, {
      cache: 'no-store',
      signal: AbortSignal.timeout(15000),
    });
  } catch {
    throw new Error('Yasal içerik servisine ulaşılamıyor.');
  }
  if (response.status === 404) notFound();
  if (!response.ok) throw new Error('Yasal içerik yüklenemedi.');
  const page: { title: string; content: string; updatedAt: string } = await response.json();
  return (
    <article className="panel legal-article">
      <span className="eyebrow">UNITRACK · YASAL BİLGİLENDİRME</span>
      <h1>{page.title}</h1>
      <p>Son güncelleme: {new Date(page.updatedAt).toLocaleDateString('tr-TR')}</p>
      <div
        className="legal-content"
        dangerouslySetInnerHTML={{ __html: markdownToHtml(page.content) }}
      />
    </article>
  );
}
