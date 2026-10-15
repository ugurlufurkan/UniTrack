'use client';
import { useState } from 'react';
import { api, json } from '@/lib/api';
import { localDate } from '@/lib/academic.mjs';
import type { Attendance as Summary } from '@/lib/types';
import { usePortal, useResource } from './portal-provider';
import {
  AsyncForm,
  Badge,
  ConfirmButton,
  Empty,
  ErrorNotice,
  Field,
  Icon,
  Loading,
  Modal,
  number,
  PageTitle,
} from './ui';

function AttendanceDetail({ courseId, onClose }: { courseId: string; onClose: () => void }) {
  const { data, error, retry } = useResource<Summary>(`attendance/courses/${courseId}`);
  const { reload, notify } = usePortal();
  const [tab, setTab] = useState('records');
  return (
    <Modal title={data?.courseName || 'Devamsızlık ayrıntıları'} onClose={onClose}>
      {error ? (
        <ErrorNotice error={error} retry={retry} />
      ) : !data ? (
        <Loading />
      ) : (
        <>
          <div className="tab-bar">
            <button
              className={tab === 'records' ? 'selected' : ''}
              onClick={() => setTab('records')}
            >
              Haftalık kayıt
            </button>
            <button
              className={tab === 'settings' ? 'selected' : ''}
              onClick={() => setTab('settings')}
            >
              Ders ayarları
            </button>
          </div>
          {tab === 'settings' ? (
            <AsyncForm
              onSave={async (form) => {
                await api(`attendance/courses/${courseId}/settings`, {
                  method: 'PUT',
                  body: json({
                    totalWeeks: Number(form.get('totalWeeks')),
                    weeklyHours: Number(form.get('weeklyHours')),
                    attendanceLimitHours: Number(form.get('attendanceLimitHours')),
                  }),
                });
                reload();
                notify('Devamsızlık ayarları güncellendi.');
              }}
            >
              <Field label="Dönem uzunluğu (hafta)">
                <input
                  name="totalWeeks"
                  type="number"
                  min={1}
                  max={52}
                  required
                  defaultValue={data.totalWeeks}
                />
              </Field>
              <Field label="Haftalık ders saati">
                <input
                  name="weeklyHours"
                  type="number"
                  min={1}
                  max={24}
                  required
                  defaultValue={data.weeklyHours}
                />
              </Field>
              <Field label="Devamsızlık sınırı (saat)" hint="0: Sınır takibi kapalı.">
                <input
                  name="attendanceLimitHours"
                  type="number"
                  min={0}
                  max={500}
                  required
                  defaultValue={data.attendanceLimitHours}
                />
              </Field>
            </AsyncForm>
          ) : (
            <>
              <AsyncForm
                key={`${data.records?.length}-${data.weeklyHours}`}
                onSave={async (form) => {
                  await api(`attendance/courses/${courseId}/records`, {
                    method: 'POST',
                    body: json({
                      weekNumber: Number(form.get('weekNumber')),
                      date: String(form.get('date')),
                      attendedHours: Number(form.get('attendedHours')),
                      absentHours: Number(form.get('absentHours')),
                      excusedHours: Number(form.get('excusedHours')),
                      note: form.get('note') || null,
                    }),
                  });
                  reload();
                  notify('Haftalık kayıt kaydedildi.');
                }}
              >
                <p className="notice">
                  Bir haftayı tekrar kaydetmek o haftanın eski kaydını günceller. Toplam saat{' '}
                  {data.weeklyHours} değerini aşamaz.
                </p>
                <div className="form-grid">
                  <Field label="Hafta">
                    <input
                      name="weekNumber"
                      type="number"
                      min={1}
                      max={data.totalWeeks}
                      required
                      defaultValue={Math.min(
                        (data.records?.reduce((m, r) => Math.max(m, r.weekNumber), 0) || 0) + 1,
                        data.totalWeeks,
                      )}
                    />
                  </Field>
                  <Field label="Tarih">
                    <input name="date" type="date" required defaultValue={localDate()} />
                  </Field>
                </div>
                <div className="form-grid three">
                  <Field label="Katıldığın saat">
                    <input
                      name="attendedHours"
                      type="number"
                      min={0}
                      max={data.weeklyHours}
                      required
                      defaultValue={data.weeklyHours}
                    />
                  </Field>
                  <Field label="Devamsız saat">
                    <input
                      name="absentHours"
                      type="number"
                      min={0}
                      max={data.weeklyHours}
                      required
                      defaultValue={0}
                    />
                  </Field>
                  <Field label="Mazeretli saat">
                    <input
                      name="excusedHours"
                      type="number"
                      min={0}
                      max={data.weeklyHours}
                      required
                      defaultValue={0}
                    />
                  </Field>
                </div>
                <Field label="Not">
                  <input name="note" maxLength={500} placeholder="İsteğe bağlı açıklama" />
                </Field>
              </AsyncForm>
              <div className="attendance-records">
                <h3>Kaydedilen haftalar</h3>
                {!data.records?.length ? (
                  <p className="muted">Henüz kayıt yok.</p>
                ) : (
                  data.records.map((record) => (
                    <div key={record.id} className="attendance-record">
                      <strong>{record.weekNumber}. hafta</strong>
                      <span>
                        {record.attendedHours} katılım · {record.absentHours} devamsız ·{' '}
                        {record.excusedHours} mazeretli
                      </span>
                      <ConfirmButton
                        message={`${record.weekNumber}. haftanın kaydı silinecek.`}
                        onConfirm={async () => {
                          await api(`attendance/records/${record.id}`, { method: 'DELETE' });
                          reload();
                          notify('Hafta kaydı silindi.');
                        }}
                      >
                        <Icon name="trash" size={15} />
                      </ConfirmButton>
                    </div>
                  ))
                )}
              </div>
            </>
          )}
        </>
      )}
    </Modal>
  );
}

export function Attendance() {
  const { courses, semesterId } = usePortal();
  const { data, error, retry } = useResource<Summary[]>('attendance/courses');
  const [selected, setSelected] = useState<string | null>(null);
  const items = data?.filter((s) =>
    courses.some((c) => c.id === s.courseId && c.semesterId === semesterId),
  );
  return (
    <>
      <PageTitle
        eyebrow="DEVAM ETMEK ÖNEMLİ"
        title="Devamsızlık takibi"
        description="Ders katılımını kaydet, sınırlarını bil ve dönemi güvenle tamamla."
      />
      {error ? (
        <ErrorNotice error={error} retry={retry} />
      ) : !items ? (
        <Loading />
      ) : !items.length ? (
        <section className="panel">
          <Empty icon="clock" title="Takip edilecek bir ders yok.">
            Seçili döneme ders eklediğinde devamsızlıklarını takip edebilirsin.
          </Empty>
        </section>
      ) : (
        <>
          <div className="notice">
            <Icon name="clock" />
            Devamsızlık takibi kişisel kayıtlarına dayanır. Üniversitenin resmi kayıtlarını da
            kontrol et.
          </div>
          <div className="attendance-grid">
            {items.map((item) => (
              <section className="panel attendance-card" key={item.courseId}>
                <div className="panel-heading">
                  <span className={`metric-icon ${item.isAtRisk ? 'amber' : 'green'}`}>
                    <Icon name="clock" />
                  </span>
                  <Badge tone={item.isAtRisk ? 'red' : 'green'}>
                    {item.isAtRisk ? 'Sınırda / sınır aşıldı' : 'Takipte'}
                  </Badge>
                </div>
                <h2>{item.courseName}</h2>
                <div className="attendance-count">
                  <strong>
                    {item.absentHours}
                    <small> saat</small>
                  </strong>
                  <span>devamsızlık</span>
                </div>
                <div className="progress-track">
                  <span
                    className={item.isAtRisk ? 'warning' : ''}
                    style={{
                      width: `${item.attendanceLimitHours > 0 ? Math.min((item.absentHours / item.attendanceLimitHours) * 100, 100) : 0}%`,
                    }}
                  />
                </div>
                <div className="attendance-limit">
                  <span>
                    {item.attendanceLimitHours > 0
                      ? `${item.attendanceLimitHours} saat sınır`
                      : 'Sınır belirlenmedi'}
                  </span>
                  <strong>
                    {item.remainingAllowedHours !== null
                      ? `${item.remainingAllowedHours} saat kaldı`
                      : '—'}
                  </strong>
                </div>
                <div className="attendance-meta">
                  <span>
                    Katılım <b>{item.attendedHours} sa.</b>
                  </span>
                  <span>
                    Mazeretli <b>{item.excusedHours} sa.</b>
                  </span>
                  <span>
                    Devamsızlık <b>%{number(item.absenceRatePercent, 1)}</b>
                  </span>
                </div>
                <button
                  className="button secondary full-width"
                  onClick={() => setSelected(item.courseId)}
                >
                  Kayıtları yönet
                  <Icon name="arrow" size={16} />
                </button>
              </section>
            ))}
          </div>
        </>
      )}
      {selected && <AttendanceDetail courseId={selected} onClose={() => setSelected(null)} />}
    </>
  );
}
