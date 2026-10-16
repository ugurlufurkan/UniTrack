'use client';
import Link from 'next/link';
import { useState } from 'react';
import { api, json } from '@/lib/api';
import { calculateGpa, semesterLabel } from '@/lib/academic.mjs';
import type { Component, Course, Semester } from '@/lib/types';
import { usePortal } from './portal-provider';
import {
  AsyncForm,
  Badge,
  ConfirmButton,
  Empty,
  Field,
  Icon,
  Modal,
  number,
  PageTitle,
} from './ui';

export function Semesters() {
  const { semesters, courses, reload, notify, setSemesterId } = usePortal();
  const [edit, setEdit] = useState<Semester | 'new' | null>(null);
  return (
    <>
      <PageTitle
        eyebrow="AKADEMİK YOLCULUĞUN"
        title="Dönemlerim"
        description="Her dönem yeni bir sayfa. Akademik geçmişini düzenli tut."
        action={
          <button className="button" onClick={() => setEdit('new')}>
            <Icon name="plus" size={17} />
            Dönem ekle
          </button>
        }
      />
      {!semesters.length ? (
        <div className="panel">
          <Empty
            icon="cap"
            title="İlk dönemini oluşturalım."
            action={
              <button className="button" onClick={() => setEdit('new')}>
                Dönem oluştur
              </button>
            }
          >
            Ders ve notlarını takip etmek için bir akademik dönem ekle.
          </Empty>
        </div>
      ) : (
        <div className="semester-grid">
          {semesters.map((semester, index) => {
            const items = courses.filter((c) => c.semesterId === semester.id);
            return (
              <section key={semester.id} className="panel semester-card">
                <div className="semester-card-top">
                  <span className={`metric-icon ${index % 2 ? 'purple' : 'blue'}`}>
                    <Icon name="cap" size={24} />
                  </span>
                  <div className="row-actions">
                    <button
                      className="icon-button"
                      onClick={() => setEdit(semester)}
                      aria-label={`${semesterLabel(semester)} düzenle`}
                    >
                      <Icon name="edit" size={17} />
                    </button>
                    <ConfirmButton
                      message={`${semesterLabel(semester)} dönemi ve bu döneme bağlı tüm ders, not ve devamsızlık kayıtları silinecek.`}
                      onConfirm={async () => {
                        await api(`semesters/${semester.id}`, { method: 'DELETE' });
                        reload();
                        notify('Dönem silindi.');
                      }}
                    >
                      <Icon name="trash" size={17} />
                    </ConfirmButton>
                  </div>
                </div>
                <span className="section-kicker">
                  {semester.year}–{semester.year + 1} AKADEMİK YILI
                </span>
                <h2>{semester.term} dönemi</h2>
                <div className="semester-stats">
                  <div>
                    <span>Ders</span>
                    <strong>{items.length}</strong>
                  </div>
                  <div>
                    <span>Kredi</span>
                    <strong>{items.reduce((s, c) => s + c.credit, 0)}</strong>
                  </div>
                  <div>
                    <span>Dönem ort.</span>
                    <strong>
                      {items.some((c) => c.gradePoint !== null) ? number(calculateGpa(items)) : '—'}
                    </strong>
                  </div>
                </div>
                <Link
                  className="semester-link"
                  href="/courses"
                  onClick={() => setSemesterId(semester.id)}
                >
                  Dersleri görüntüle
                  <Icon name="arrow" size={18} />
                </Link>
              </section>
            );
          })}
        </div>
      )}
      {edit && (
        <Modal
          title={edit === 'new' ? 'Yeni dönem oluştur' : 'Dönemi düzenle'}
          onClose={() => setEdit(null)}
        >
          <AsyncForm
            onSave={async (form) => {
              await api(edit === 'new' ? 'semesters' : `semesters/${edit.id}`, {
                method: edit === 'new' ? 'POST' : 'PUT',
                body: json({ year: Number(form.get('year')), term: form.get('term') }),
              });
              reload();
              notify('Dönem kaydedildi.');
              setEdit(null);
            }}
          >
            <Field label="Akademik yıl başlangıcı">
              <input
                name="year"
                type="number"
                required
                min={2020}
                max={2100}
                defaultValue={edit === 'new' ? new Date().getFullYear() : edit.year}
              />
            </Field>
            <Field label="Dönem">
              <select name="term" defaultValue={edit === 'new' ? 'Güz' : edit.term}>
                <option>Güz</option>
                <option>Bahar</option>
                <option>Yaz</option>
              </select>
            </Field>
          </AsyncForm>
        </Modal>
      )}
    </>
  );
}

function CourseEditor({ course, onClose }: { course: Course | null; onClose: () => void }) {
  const { semesterId, reload, notify, scale } = usePortal();
  const [components, setComponents] = useState<Component[]>(
    course?.components || [
      { name: 'Vize', weight: 40, score: null },
      { name: 'Final', weight: 60, score: null },
    ],
  );
  const [customScale, setCustomScale] = useState(
    course?.gradeScale ? JSON.stringify(course.gradeScale, null, 2) : '',
  );
  const update = (index: number, patch: Partial<Component>) =>
    setComponents((values) => values.map((c, i) => (i === index ? { ...c, ...patch } : c)));
  const weight = components.reduce((s, c) => s + c.weight, 0);
  return (
    <Modal title={course ? 'Dersi ve notları düzenle' : 'Yeni ders ekle'} onClose={onClose}>
      <AsyncForm
        onSave={async (form) => {
          if (Math.abs(weight - 100) >= 0.01)
            throw new Error('Not ağırlıklarının toplamı %100 olmalı.');
          let gradeScale;
          try {
            gradeScale = customScale.trim() ? JSON.parse(customScale) : null;
          } catch {
            throw new Error('Harf notu skalası geçerli bir JSON olmalı.');
          }
          await api(course ? `courses/${course.id}` : 'courses', {
            method: course ? 'PUT' : 'POST',
            body: json({
              ...(course ? {} : { semesterId }),
              name: form.get('name'),
              credit: Number(form.get('credit')),
              components,
              gradeScale,
            }),
          });
          reload();
          notify('Ders ve notlar kaydedildi.');
          onClose();
        }}
      >
        <div className="form-grid">
          <Field label="Ders adı">
            <input
              name="name"
              defaultValue={course?.name}
              required
              minLength={2}
              maxLength={150}
              placeholder="Örn. Veri Yapıları"
            />
          </Field>
          <Field label="Kredi">
            <input
              name="credit"
              type="number"
              min={1}
              max={30}
              step="1"
              defaultValue={course?.credit || 3}
              required
            />
          </Field>
        </div>
        <div className="form-section-heading">
          <h3>Not bileşenleri</h3>
          <Badge tone={weight === 100 ? 'green' : 'amber'}>Toplam %{weight}</Badge>
        </div>
        <p className="muted small-copy">
          Henüz açıklanmayan notları boş bırak. Notları tamamlanmayan dersler GANO hesabına
          katılmaz.
        </p>
        <div className="component-labels">
          <span>Bileşen</span>
          <span>Ağırlık %</span>
          <span>Not / 100</span>
          <span />
        </div>
        {components.map((component, i) => (
          <div key={i} className="component-row">
            <input
              aria-label={`${i + 1}. bileşen adı`}
              value={component.name}
              required
              maxLength={100}
              onChange={(e) => update(i, { name: e.target.value })}
            />
            <input
              aria-label={`${component.name} ağırlığı`}
              value={component.weight}
              type="number"
              min={0}
              max={100}
              step="0.01"
              required
              onChange={(e) => update(i, { weight: Number(e.target.value) })}
            />
            <input
              aria-label={`${component.name} notu`}
              value={component.score ?? ''}
              placeholder="—"
              type="number"
              min={0}
              max={100}
              step="0.01"
              onChange={(e) =>
                update(i, { score: e.target.value === '' ? null : Number(e.target.value) })
              }
            />
            <button
              type="button"
              className="icon-button danger"
              aria-label={`${component.name} bileşenini kaldır`}
              disabled={components.length === 1}
              onClick={() => setComponents((c) => c.filter((_, n) => n !== i))}
            >
              <Icon name="close" size={16} />
            </button>
          </div>
        ))}
        <button
          type="button"
          className="text-button"
          onClick={() => setComponents((c) => [...c, { name: '', weight: 0, score: null }])}
        >
          <Icon name="plus" size={16} />
          Bileşen ekle
        </button>
        <details className="advanced">
          <summary>Derse özel harf notu skalası</summary>
          <Field
            label="Harf notu aralıkları (JSON)"
            hint="Boş bırakırsan hesabındaki genel skala kullanılır."
          >
            <textarea
              rows={7}
              value={customScale}
              onChange={(e) => setCustomScale(e.target.value)}
              placeholder={JSON.stringify(scale, null, 2)}
            />
          </Field>
        </details>
      </AsyncForm>
    </Modal>
  );
}

export function Courses() {
  const { courses, semesters, semesterId, reload, notify } = usePortal();
  const [edit, setEdit] = useState<Course | 'new' | null>(null);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('all');
  const semester = semesters.find((s) => s.id === semesterId);
  const items = courses.filter((c) => c.semesterId === semesterId);
  const visible = items.filter(
    (c) =>
      c.name.toLocaleLowerCase('tr-TR').includes(search.toLocaleLowerCase('tr-TR')) &&
      (filter === 'all' ||
        (filter === 'ongoing'
          ? c.average === null
          : filter === 'passed'
            ? c.passed === true
            : c.passed === false)),
  );
  return (
    <>
      <PageTitle
        eyebrow={semester ? semesterLabel(semester).toLocaleUpperCase('tr-TR') : 'AKADEMİK TAKİP'}
        title="Dersler ve notlar"
        description="Ders yükünü gör, notlarını ekle, gelişimini takip et."
        action={
          <button className="button" disabled={!semesterId} onClick={() => setEdit('new')}>
            <Icon name="plus" size={17} />
            Ders ekle
          </button>
        }
      />
      {!semesterId ? (
        <div className="panel">
          <Empty
            title="Önce bir dönem oluşturalım."
            action={
              <Link className="button" href="/semesters">
                Dönemlere git
              </Link>
            }
          >
            Derslerin bir akademik döneme bağlı olarak saklanır.
          </Empty>
        </div>
      ) : (
        <>
          <div className="summary-strip">
            <div>
              <span>Dönem dersi</span>
              <strong>{items.length}</strong>
            </div>
            <div>
              <span>Toplam kredi</span>
              <strong>{items.reduce((s, c) => s + c.credit, 0)}</strong>
            </div>
            <div>
              <span>Dönem ortalaması</span>
              <strong>
                {items.some((c) => c.gradePoint !== null) ? number(calculateGpa(items)) : '—'}
              </strong>
            </div>
            <div>
              <span>Devam eden</span>
              <strong>{items.filter((c) => c.average === null).length}</strong>
            </div>
          </div>
          <section className="panel">
            <div className="table-toolbar">
              <label className="search-field">
                <Icon name="search" size={18} />
                <input
                  aria-label="Ders ara"
                  placeholder="Ders adıyla ara…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </label>
              <select
                aria-label="Ders durumuna göre filtrele"
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
              >
                <option value="all">Tüm dersler</option>
                <option value="ongoing">Devam eden</option>
                <option value="passed">Geçilen</option>
                <option value="failed">Kalınan</option>
              </select>
            </div>
            {visible.length ? (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Ders adı</th>
                      <th>Kredi</th>
                      <th>Not bileşenleri</th>
                      <th>Ortalama</th>
                      <th>Harf</th>
                      <th>Durum</th>
                      <th>
                        <span className="sr-only">İşlemler</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {visible.map((course, i) => (
                      <tr key={course.id}>
                        <td>
                          <div className="course-name">
                            <span className={`course-monogram color-${i % 3}`}>
                              {course.name.slice(0, 2).toLocaleUpperCase('tr-TR')}
                            </span>
                            <strong>{course.name}</strong>
                          </div>
                        </td>
                        <td>{course.credit}</td>
                        <td>
                          <div className="grade-components">
                            {course.components.map((c, n) => (
                              <span key={c.id || n}>
                                {c.name}
                                <b>{c.score ?? '—'}</b>
                              </span>
                            ))}
                          </div>
                        </td>
                        <td className="tabular">
                          {course.average === null ? '—' : number(course.average, 1)}
                        </td>
                        <td>
                          <Badge
                            tone={course.passed ? 'green' : course.passed === false ? 'red' : ''}
                          >
                            {course.letterGrade || '—'}
                          </Badge>
                        </td>
                        <td>
                          <span
                            className={`course-status ${course.average === null ? 'ongoing' : course.passed ? 'passed' : 'failed'}`}
                          >
                            <i />
                            {course.average === null
                              ? 'Devam ediyor'
                              : course.passed
                                ? 'Geçti'
                                : 'Kaldı'}
                          </span>
                        </td>
                        <td>
                          <div className="row-actions">
                            <button
                              className="icon-button"
                              aria-label={`${course.name} düzenle`}
                              onClick={() => setEdit(course)}
                            >
                              <Icon name="edit" size={17} />
                            </button>
                            <ConfirmButton
                              message={`${course.name} ve ilişkili kayıtları silinecek.`}
                              onConfirm={async () => {
                                await api(`courses/${course.id}`, { method: 'DELETE' });
                                reload();
                                notify('Ders silindi.');
                              }}
                            >
                              <Icon name="trash" size={17} />
                            </ConfirmButton>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <Empty
                title={items.length ? 'Aramana uygun ders yok.' : 'Bu dönemin ilk dersini ekle.'}
              >
                {items.length
                  ? 'Arama veya filtreyi değiştirerek tekrar dene.'
                  : 'Vize, final, proje ve diğer notlarını tek bir yerde takip et.'}
              </Empty>
            )}
          </section>
        </>
      )}
      {edit && <CourseEditor course={edit === 'new' ? null : edit} onClose={() => setEdit(null)} />}
    </>
  );
}

export function GpaSimulator() {
  const { courses, scale, settings } = usePortal();
  const [overrides, setOverrides] = useState<Record<string, number>>({});
  const current = calculateGpa(courses);
  const projected = calculateGpa(courses, overrides);
  return (
    <>
      <PageTitle
        eyebrow="OLASILIKLARI KEŞFET"
        title="GANO simülatörü"
        description="“Bu dersten AA alırsam?” Olası notlarını dene, hedefini planla."
        action={
          <button className="button secondary" onClick={() => setOverrides({})}>
            Seçimleri sıfırla
          </button>
        }
      />
      <div className="simulator-overview">
        <div>
          <span>Mevcut GANO</span>
          <strong>{number(current)}</strong>
        </div>
        <Icon name="arrow" size={30} />
        <div className="projected-gpa">
          <span>Simüle edilen GANO</span>
          <strong>{number(projected)}</strong>
        </div>
        <div>
          <span>Hedefin</span>
          <strong>{settings.targetGpa !== null ? number(settings.targetGpa) : '—'}</strong>
        </div>
      </div>
      <p className="notice">
        <Icon name="shield" />
        Bu alandaki seçimler notlarını değiştirmez. Henüz notlandırılmamış dersler, bir harf
        seçtiğinde simülasyona katılır.
      </p>
      <section className="panel">
        {!courses.length ? (
          <Empty icon="target" title="Simülasyon için derslerini ekle.">
            Derslerin eklendiğinde olası ortalamanı burada hesaplayabilirsin.
          </Empty>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Ders</th>
                  <th>Kredi</th>
                  <th>Mevcut not</th>
                  <th>Beklenen harf notu</th>
                </tr>
              </thead>
              <tbody>
                {courses.map((course) => (
                  <tr key={course.id}>
                    <td>
                      <strong>{course.name}</strong>
                    </td>
                    <td>{course.credit}</td>
                    <td>{course.letterGrade || 'Devam ediyor'}</td>
                    <td>
                      <select
                        aria-label={`${course.name} beklenen not`}
                        value={overrides[course.id] === undefined ? '' : overrides[course.id]}
                        onChange={(e) =>
                          setOverrides((v) => {
                            const next = { ...v };
                            if (e.target.value === '') delete next[course.id];
                            else next[course.id] = Number(e.target.value);
                            return next;
                          })
                        }
                      >
                        <option value="">Mevcut durumu koru</option>
                        {(course.gradeScale || scale).map((b) => (
                          <option key={b.letter} value={b.point}>
                            {b.letter} · {number(b.point)}
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
