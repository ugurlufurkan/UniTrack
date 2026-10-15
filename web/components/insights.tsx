'use client';
import { useState } from 'react';
import { api, json } from '@/lib/api';
import { calculateGpa, semesterLabel } from '@/lib/academic.mjs';
import type { Statistics as StatisticsData } from '@/lib/types';
import { usePortal, useResource } from './portal-provider';
import { Metric } from './dashboard';
import {
  AsyncForm,
  Badge,
  Empty,
  ErrorNotice,
  Field,
  Icon,
  Loading,
  Modal,
  number,
  PageTitle,
} from './ui';

export function Statistics() {
  const { courses, scale } = usePortal();
  const { data, error, retry } = useResource<StatisticsData>('statistics');
  if (error) return <ErrorNotice error={error} retry={retry} />;
  if (!data) return <Loading />;
  const letters = [
    ...new Set([
      ...scale.map((b) => b.letter),
      ...courses.map((c) => c.letterGrade).filter((v): v is string => v !== null),
    ]),
  ];
  const distribution = letters.map((letter) => ({
    letter,
    count: courses.filter((c) => c.letterGrade === letter).length,
  }));
  const max = Math.max(...distribution.map((d) => d.count), 1);
  return (
    <>
      <PageTitle
        eyebrow="GELİŞİMİNİ GÖR"
        title="Akademik istatistikler"
        description="Geçmişinden öğren, bir sonraki adımını daha bilinçli at."
      />
      <div className="metric-grid">
        <Metric
          title="Genel ortalaman"
          value={number(calculateGpa(courses))}
          subtitle="Kredi ağırlıklı GANO"
          icon="chart"
          tone="blue"
        />
        <Metric
          title="Toplam kredi"
          value={String(data.totalCredits)}
          subtitle={`${data.totalCourses} dersin toplam yükü`}
          icon="book"
          tone="purple"
        />
        <Metric
          title="Geçilen dersler"
          value={String(data.passedCourses)}
          subtitle={`${data.failedCourses} derste başarısız sonuç`}
          icon="check"
          tone="green"
        />
        <Metric
          title="Not ortalaman"
          value={number(data.overallAverage, 1)}
          subtitle="Notu tamamlanan dersler / 100"
          icon="target"
          tone="amber"
        />
      </div>
      <div className="insights-grid">
        <section className="panel">
          <div className="panel-heading">
            <div>
              <span className="section-kicker">DÖNEM DÖNEM</span>
              <h2>Ortalamanın yolculuğu</h2>
            </div>
            <Badge>GANO</Badge>
          </div>
          {data.semesterGpa.length ? (
            <div className="gpa-chart">
              {data.semesterGpa.map((s) => (
                <div className="gpa-bar-row" key={s.semesterId}>
                  <span>{s.semester}</span>
                  <div className="chart-track">
                    <div style={{ width: `${Math.min((s.gpa / 4.5) * 100, 100)}%` }} />
                  </div>
                  <strong>{number(s.gpa)}</strong>
                </div>
              ))}
            </div>
          ) : (
            <Empty icon="chart" title="Hikâyen henüz başlıyor.">
              Notu tamamlanan dönemlerin ortalamaları burada görünecek.
            </Empty>
          )}
        </section>
        <section className="panel">
          <div className="panel-heading">
            <div>
              <span className="section-kicker">NOTLARININ DAĞILIMI</span>
              <h2>Harf notlarına bakış</h2>
            </div>
          </div>
          {courses.some((c) => c.letterGrade) ? (
            <div className="distribution-chart">
              {distribution.map((item, i) => (
                <div className="distribution-column" key={item.letter}>
                  <strong>{item.count}</strong>
                  <div className="distribution-track">
                    <div
                      className={`color-${i % 3}`}
                      style={{ height: `${(item.count / max) * 100}%` }}
                    />
                  </div>
                  <span>{item.letter}</span>
                </div>
              ))}
            </div>
          ) : (
            <Empty icon="chart" title="Notların eklendikçe şekillenecek.">
              Tamamlanan derslerinin harf notlarını burada karşılaştırabilirsin.
            </Empty>
          )}
        </section>
      </div>
      <div className="notice">
        Devam eden {data.ongoingCourses} ders, not ortalaması ve GANO hesabına dahil edilmez. Harf
        notları dersin veya hesabının skalasına göre hesaplanır.
      </div>
    </>
  );
}

type ImportSemester = {
  year: number;
  term: string;
  academicYearLabel?: string;
  courses: { name: string; credit: number; grade: string; recognized?: boolean }[];
};
function ImportTranscript({ onClose }: { onClose: () => void }) {
  const [preview, setPreview] = useState<ImportSemester[] | null>(null);
  const { reload, notify } = usePortal();
  function updateCourse(si: number, ci: number, field: string, value: string | number) {
    setPreview(
      (current) =>
        current?.map((semester, index) =>
          index === si
            ? {
                ...semester,
                courses: semester.courses.map((course, n) =>
                  n === ci ? { ...course, [field]: value } : course,
                ),
              }
            : semester,
        ) || null,
    );
  }
  return (
    <Modal title="PDF transkriptini içe aktar" onClose={onClose}>
      {!preview ? (
        <AsyncForm
          label="Dosyayı oku ve önizle"
          onSave={async (data) => {
            const file = data.get('file') as File;
            if (!file || file.size > 10 * 1024 * 1024)
              throw new Error('En fazla 10 MB boyutunda bir PDF seç.');
            const result = await api<{ semesters: ImportSemester[] }>('transcript-import/parse', {
              method: 'POST',
              body: data,
            });
            if (!result.semesters.length) throw new Error('Dosyada aktarılacak ders bulunamadı.');
            setPreview(result.semesters);
          }}
        >
          <p className="muted">
            Dosyan önce okunur. Dersler, önizlemeyi kontrol edip onayladığında hesabına eklenir.
          </p>
          <Field label="Transkript dosyası (PDF, en fazla 10 MB)">
            <input name="file" type="file" accept="application/pdf,.pdf" required />
          </Field>
        </AsyncForm>
      ) : (
        <AsyncForm
          label="Kontrol ettim, hesabıma aktar"
          onSave={async () => {
            if (!preview.some((s) => s.courses.length))
              throw new Error('Aktarılacak en az bir ders olmalı.');
            await api('transcript-import/import', {
              method: 'POST',
              body: json({ semesters: preview.filter((s) => s.courses.length) }),
            });
            reload();
            notify('Transkript hesabına aktarıldı.');
            onClose();
          }}
        >
          <p className="notice">
            Ders adlarını, kredileri ve harf notlarını kontrol et. Aynı dersleri yeniden aktarmamaya
            dikkat et; içe aktarma mevcut derslerini değiştirmez.
          </p>
          {preview.map((semester, si) => (
            <section className="import-semester" key={si}>
              <div className="form-grid">
                <Field label="Yıl">
                  <input
                    aria-label={`${si + 1}. dönem yılı`}
                    type="number"
                    min={2000}
                    max={2100}
                    required
                    value={semester.year}
                    onChange={(e) =>
                      setPreview((values) =>
                        values!.map((s, i) =>
                          i === si ? { ...s, year: Number(e.target.value) } : s,
                        ),
                      )
                    }
                  />
                </Field>
                <Field label="Dönem">
                  <select
                    value={semester.term}
                    onChange={(e) =>
                      setPreview((values) =>
                        values!.map((s, i) => (i === si ? { ...s, term: e.target.value } : s)),
                      )
                    }
                  >
                    <option>Güz</option>
                    <option>Bahar</option>
                    <option>Yaz</option>
                  </select>
                </Field>
              </div>
              {semester.courses.map((course, ci) => (
                <div className="import-course" key={ci}>
                  <input
                    aria-label="Ders adı"
                    value={course.name}
                    required
                    maxLength={150}
                    onChange={(e) => updateCourse(si, ci, 'name', e.target.value)}
                  />
                  <input
                    aria-label={`${course.name} kredisi`}
                    type="number"
                    min={0}
                    max={30}
                    step="0.5"
                    required
                    value={course.credit}
                    onChange={(e) => updateCourse(si, ci, 'credit', Number(e.target.value))}
                  />
                  <input
                    aria-label={`${course.name} harf notu`}
                    required
                    maxLength={4}
                    value={course.grade}
                    onChange={(e) =>
                      updateCourse(si, ci, 'grade', e.target.value.toLocaleUpperCase('tr-TR'))
                    }
                  />
                  <button
                    type="button"
                    className="icon-button danger"
                    aria-label={`${course.name} aktarımdan çıkar`}
                    onClick={() =>
                      setPreview((values) =>
                        values!.map((s, i) =>
                          i === si ? { ...s, courses: s.courses.filter((_, j) => j !== ci) } : s,
                        ),
                      )
                    }
                  >
                    <Icon name="close" size={15} />
                  </button>
                </div>
              ))}
            </section>
          ))}
          <button type="button" className="text-button" onClick={() => setPreview(null)}>
            Başka dosya seç
          </button>
        </AsyncForm>
      )}
    </Modal>
  );
}

export function Transcript() {
  const { courses, semesters, user } = usePortal();
  const [importing, setImporting] = useState(false);
  return (
    <>
      <PageTitle
        eyebrow="AKADEMİK GEÇMİŞİN"
        title="Transkriptim"
        description="Tüm dönemlerin ve notların, düzenli bir akademik özet."
        action={
          <div className="row-actions">
            <button className="button secondary" onClick={() => setImporting(true)}>
              <Icon name="file" size={17} />
              PDF içe aktar
            </button>
            <button className="button" onClick={() => window.print()} disabled={!courses.length}>
              <Icon name="download" size={17} />
              Yazdır / PDF
            </button>
          </div>
        }
      />
      <article className="panel transcript-report">
        <div className="transcript-heading">
          <div>
            <span className="eyebrow">UNITRACK · AKADEMİK ÖZET</span>
            <h2>{user.name || user.email}</h2>
            <p>{user.email}</p>
          </div>
          <div className="transcript-gpa">
            <span>GENEL ORTALAMA</span>
            <strong>{number(calculateGpa(courses))}</strong>
          </div>
        </div>
        <p className="transcript-disclaimer">
          Bu belge kişisel takip amacıyla oluşturulmuştur; resmi transkript yerine geçmez.
        </p>
        {!courses.length ? (
          <Empty icon="file" title="Akademik geçmişin burada birikecek.">
            Derslerini elle ekleyebilir veya PDF transkriptini içe aktarabilirsin.
          </Empty>
        ) : (
          semesters.map((semester) => {
            const items = courses.filter((c) => c.semesterId === semester.id);
            if (!items.length) return null;
            return (
              <section className="transcript-semester" key={semester.id}>
                <div className="transcript-semester-title">
                  <h3>{semesterLabel(semester)}</h3>
                  <span>
                    Dönem ortalaması: <strong>{number(calculateGpa(items))}</strong>
                  </span>
                </div>
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Ders</th>
                        <th>Kredi</th>
                        <th>Ortalama</th>
                        <th>Harf notu</th>
                        <th>Katsayı</th>
                      </tr>
                    </thead>
                    <tbody>
                      {items.map((course) => (
                        <tr key={course.id}>
                          <td>{course.name}</td>
                          <td>{course.credit}</td>
                          <td>{course.average === null ? '—' : number(course.average)}</td>
                          <td>{course.letterGrade || 'Devam ediyor'}</td>
                          <td>{course.gradePoint === null ? '—' : number(course.gradePoint)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            );
          })
        )}
      </article>
      {importing && <ImportTranscript onClose={() => setImporting(false)} />}
    </>
  );
}
