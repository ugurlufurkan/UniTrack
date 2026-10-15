'use client';
import Link from 'next/link';
import { useState } from 'react';
import { api, json } from '@/lib/api';
import { localDate } from '@/lib/academic.mjs';
import type { Event, Schedule } from '@/lib/types';
import { usePortal, useResource } from './portal-provider';
import {
  AsyncForm,
  Badge,
  ConfirmButton,
  dateTime,
  Empty,
  ErrorNotice,
  Field,
  Icon,
  Loading,
  Modal,
  PageTitle,
} from './ui';

const days = ['Pazar', 'Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi'];
const types: Record<string, string> = {
  lesson: 'Ders',
  exam: 'Sınav',
  quiz: 'Kısa sınav',
  assignment: 'Ödev',
  project: 'Proje',
  presentation: 'Sunum',
  other: 'Diğer',
};
const statuses: Record<string, string> = {
  pending: 'Yapılacak',
  in_progress: 'Devam ediyor',
  completed: 'Tamamlandı',
  cancelled: 'İptal edildi',
};

function localInput(value?: string) {
  const date = value ? new Date(value) : new Date();
  return `${localDate(date)}T${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

function EventEditor({
  event,
  task,
  onClose,
}: {
  event: Event | null;
  task: boolean;
  onClose: () => void;
}) {
  const { courses, reload, notify } = usePortal();
  return (
    <Modal
      title={event ? 'Kaydı düzenle' : task ? 'Yeni görev oluştur' : 'Yeni etkinlik oluştur'}
      onClose={onClose}
    >
      <AsyncForm
        onSave={async (form) => {
          const start = new Date(String(form.get('startAt')));
          const endValue = String(form.get('endAt') || '');
          const end = endValue ? new Date(endValue) : null;
          if (end && end < start) throw new Error('Bitiş zamanı başlangıçtan önce olamaz.');
          await api(event ? `calendar/events/${event.id}` : 'calendar/events', {
            method: event ? 'PUT' : 'POST',
            body: json({
              title: form.get('title'),
              description: form.get('description') || null,
              type: form.get('type'),
              courseId: form.get('courseId') || null,
              startAt: start.toISOString(),
              endAt: end?.toISOString() || null,
              location: form.get('location') || null,
              priority: form.get('priority'),
              status: form.get('status'),
            }),
          });
          reload();
          notify('Planın kaydedildi.');
          onClose();
        }}
      >
        <Field label="Başlık">
          <input
            name="title"
            required
            maxLength={200}
            defaultValue={event?.title}
            placeholder={task ? 'Örn. Dönem projesi teslimi' : 'Örn. Matematik vize sınavı'}
          />
        </Field>
        <div className="form-grid">
          <Field label="Tür">
            <select name="type" defaultValue={event?.type || (task ? 'assignment' : 'exam')}>
              {Object.entries(types)
                .filter(([key]) => !task || ['assignment', 'project', 'presentation'].includes(key))
                .map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
            </select>
          </Field>
          <Field label="İlgili ders">
            <select name="courseId" defaultValue={event?.courseId || ''}>
              <option value="">Bir derse bağlı değil</option>
              {courses.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </Field>
        </div>
        <div className="form-grid">
          <Field label={task ? 'Teslim zamanı' : 'Başlangıç'}>
            <input
              name="startAt"
              type="datetime-local"
              required
              defaultValue={localInput(event?.startAt)}
            />
          </Field>
          <Field label="Bitiş (isteğe bağlı)">
            <input
              name="endAt"
              type="datetime-local"
              defaultValue={event?.endAt ? localInput(event.endAt) : ''}
            />
          </Field>
        </div>
        <div className="form-grid">
          <Field label="Öncelik">
            <select name="priority" defaultValue={event?.priority || 'medium'}>
              <option value="low">Düşük</option>
              <option value="medium">Normal</option>
              <option value="high">Yüksek</option>
            </select>
          </Field>
          <Field label="Durum">
            <select name="status" defaultValue={event?.status || 'pending'}>
              {Object.entries(statuses).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </Field>
        </div>
        <Field label="Konum">
          <input
            name="location"
            maxLength={255}
            defaultValue={event?.location || ''}
            placeholder="Örn. Mühendislik fakültesi, A-201"
          />
        </Field>
        <Field label="Açıklama">
          <textarea
            name="description"
            rows={3}
            maxLength={2000}
            defaultValue={event?.description || ''}
            placeholder="Bilmen gereken diğer detaylar…"
          />
        </Field>
      </AsyncForm>
    </Modal>
  );
}

export function WeeklySchedule() {
  const { courses, semesterId, reload, notify } = usePortal();
  const { data, error, retry } = useResource<Schedule[]>('calendar/schedule');
  const [edit, setEdit] = useState<Schedule | 'new' | null>(null);
  const currentCourses = courses.filter((c) => c.semesterId === semesterId);
  const entries = data?.filter((s) => currentCourses.some((c) => c.id === s.courseId));
  return (
    <>
      <PageTitle
        eyebrow="HAFTANA YER AÇ"
        title="Ders programı"
        description="Haftalık ders akışın. Nerede, ne zaman; hepsi bir bakışta."
        action={
          <button
            className="button"
            disabled={!currentCourses.length}
            onClick={() => setEdit('new')}
          >
            <Icon name="plus" size={17} />
            Programa ders ekle
          </button>
        }
      />
      {error ? (
        <ErrorNotice error={error} retry={retry} />
      ) : !entries ? (
        <Loading />
      ) : !currentCourses.length ? (
        <section className="panel">
          <Empty
            icon="calendar"
            title="Önce derslerini ekle."
            action={
              <Link className="button" href="/courses">
                Derslere git
              </Link>
            }
          >
            Seçili dönemde bir ders oluşturduktan sonra haftalık programını hazırlayabilirsin.
          </Empty>
        </section>
      ) : (
        <div className="weekly-grid">
          {[1, 2, 3, 4, 5, 6, 0].map((day) => (
            <section className={`week-day ${day === new Date().getDay() ? 'today' : ''}`} key={day}>
              <header>
                <h2>{days[day]}</h2>
                {day === new Date().getDay() && <span>Bugün</span>}
              </header>
              <div className="week-day-content">
                {entries
                  .filter((e) => e.dayOfWeek === day)
                  .sort((a, b) => a.startTime.localeCompare(b.startTime))
                  .map((entry, i) => (
                    <article className={`schedule-entry color-${i % 3}`} key={entry.id}>
                      <span className="schedule-time">
                        {entry.startTime} – {entry.endTime}
                      </span>
                      <h3>{entry.courseName}</h3>
                      <p>{entry.location || 'Konum belirtilmedi'}</p>
                      <div className="row-actions">
                        <button
                          className="icon-button"
                          onClick={() => setEdit(entry)}
                          aria-label={`${entry.courseName} saatini düzenle`}
                        >
                          <Icon name="edit" size={15} />
                        </button>
                        <ConfirmButton
                          message="Bu ders saati haftalık programından kaldırılacak. Dersin ve notların korunur."
                          onConfirm={async () => {
                            await api(`calendar/schedule/${entry.id}`, { method: 'DELETE' });
                            reload();
                            notify('Ders saati kaldırıldı.');
                          }}
                        >
                          <Icon name="trash" size={15} />
                        </ConfirmButton>
                      </div>
                    </article>
                  ))}
                {!entries.some((e) => e.dayOfWeek === day) && <p className="free-day">Ders yok</p>}
              </div>
            </section>
          ))}
        </div>
      )}
      {edit && (
        <Modal
          title={edit === 'new' ? 'Programa ders ekle' : 'Ders saatini düzenle'}
          onClose={() => setEdit(null)}
        >
          <AsyncForm
            onSave={async (form) => {
              const startTime = String(form.get('startTime'));
              const endTime = String(form.get('endTime'));
              if (endTime <= startTime) throw new Error('Bitiş saati başlangıçtan sonra olmalı.');
              await api(edit === 'new' ? 'calendar/schedule' : `calendar/schedule/${edit.id}`, {
                method: edit === 'new' ? 'POST' : 'PUT',
                body: json({
                  courseId: form.get('courseId'),
                  dayOfWeek: Number(form.get('dayOfWeek')),
                  startTime,
                  endTime,
                  location: form.get('location') || null,
                }),
              });
              reload();
              notify('Ders programın güncellendi.');
              setEdit(null);
            }}
          >
            <Field label="Ders">
              <select
                name="courseId"
                defaultValue={edit === 'new' ? currentCourses[0]?.id : edit.courseId}
              >
                {currentCourses.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Gün">
              <select name="dayOfWeek" defaultValue={edit === 'new' ? 1 : edit.dayOfWeek}>
                {[1, 2, 3, 4, 5, 6, 0].map((d) => (
                  <option key={d} value={d}>
                    {days[d]}
                  </option>
                ))}
              </select>
            </Field>
            <div className="form-grid">
              <Field label="Başlangıç saati">
                <input
                  name="startTime"
                  type="time"
                  required
                  defaultValue={edit === 'new' ? '09:00' : edit.startTime}
                />
              </Field>
              <Field label="Bitiş saati">
                <input
                  name="endTime"
                  type="time"
                  required
                  defaultValue={edit === 'new' ? '10:00' : edit.endTime}
                />
              </Field>
            </div>
            <Field label="Konum">
              <input
                name="location"
                maxLength={255}
                defaultValue={edit === 'new' ? '' : edit.location || ''}
              />
            </Field>
          </AsyncForm>
        </Modal>
      )}
    </>
  );
}

function ChecklistPanel({ task, onClose }: { task: Event; onClose: () => void }) {
  const { data, error, retry } = useResource<Event>(`tasks/${task.id}`);
  const { reload, notify } = usePortal();
  const [actionError, setActionError] = useState('');
  const [busy, setBusy] = useState(false);
  return (
    <Modal title={task.title} onClose={onClose}>
      {error ? (
        <ErrorNotice error={error} retry={retry} />
      ) : !data ? (
        <Loading />
      ) : (
        <div className="checklist-content">
          <p className="muted">{data.description || 'Görevini küçük adımlara böl.'}</p>
          <div className="checklist-items">
            {data.checklist?.map((item) => (
              <div className="checklist-item" key={item.id}>
                <label>
                  <input
                    type="checkbox"
                    checked={item.isDone}
                    disabled={busy}
                    onChange={async (e) => {
                      setBusy(true);
                      setActionError('');
                      try {
                        await api(`tasks/checklist/${item.id}`, {
                          method: 'PUT',
                          body: json({ isDone: e.target.checked }),
                        });
                        reload();
                      } catch (e) {
                        setActionError(e instanceof Error ? e.message : 'İşlem tamamlanamadı.');
                      } finally {
                        setBusy(false);
                      }
                    }}
                  />
                  <span className={item.isDone ? 'done' : ''}>{item.title}</span>
                </label>
                <ConfirmButton
                  message="Bu alt görev silinecek."
                  onConfirm={async () => {
                    await api(`tasks/checklist/${item.id}`, { method: 'DELETE' });
                    reload();
                  }}
                >
                  <Icon name="trash" size={15} />
                </ConfirmButton>
              </div>
            ))}
          </div>
          {actionError && <ErrorNotice error={actionError} />}
          <AsyncForm
            label="Alt görev ekle"
            onSave={async (form) => {
              await api(`tasks/${task.id}/checklist`, {
                method: 'POST',
                body: json({ title: form.get('title') }),
              });
              reload();
              notify('Alt görev eklendi.');
            }}
          >
            <Field label="Yeni adım">
              <input name="title" required maxLength={200} placeholder="İlk adım ne olacak?" />
            </Field>
          </AsyncForm>
        </div>
      )}
    </Modal>
  );
}

export function Tasks() {
  const { data, error, retry } = useResource<Event[]>('tasks');
  const { reload, notify } = usePortal();
  const [edit, setEdit] = useState<Event | 'new' | null>(null);
  const [checklist, setChecklist] = useState<Event | null>(null);
  const [filter, setFilter] = useState('active');
  const items = data?.filter(
    (t) =>
      filter === 'all' ||
      (filter === 'active' ? !['completed', 'cancelled'].includes(t.status) : t.status === filter),
  );
  return (
    <>
      <PageTitle
        eyebrow="ODAĞINI KORU"
        title="Görevlerim"
        description="Ödevlerin, projelerin, sunumların. Her şeyi adım adım tamamla."
        action={
          <button className="button" onClick={() => setEdit('new')}>
            <Icon name="plus" size={17} />
            Görev ekle
          </button>
        }
      />
      <div className="tab-bar" role="group" aria-label="Görev filtresi">
        {[
          ['active', 'Aktif görevler'],
          ['completed', 'Tamamlanan'],
          ['all', 'Tümü'],
        ].map(([value, label]) => (
          <button
            key={value}
            className={filter === value ? 'selected' : ''}
            onClick={() => setFilter(value)}
            aria-pressed={filter === value}
          >
            {label}
            {value === 'active' && data && (
              <span>
                {data.filter((t) => !['completed', 'cancelled'].includes(t.status)).length}
              </span>
            )}
          </button>
        ))}
      </div>
      {error ? (
        <ErrorNotice error={error} retry={retry} />
      ) : !items ? (
        <Loading />
      ) : !items.length ? (
        <section className="panel">
          <Empty icon="check" title="Burada her şey yolunda.">
            Bu görünümde henüz bir görev yok. Yeni bir hedef ekleyerek başlayabilirsin.
          </Empty>
        </section>
      ) : (
        <div className="task-grid">
          {items.map((task) => (
            <article key={task.id} className="panel task-card">
              <div className="task-card-top">
                <Badge tone={task.type === 'project' ? 'purple' : 'blue'}>{types[task.type]}</Badge>
                <span className={`task-priority ${task.priority}`}>
                  <i />
                  {task.priority === 'high'
                    ? 'Yüksek öncelik'
                    : task.priority === 'low'
                      ? 'Düşük öncelik'
                      : 'Normal öncelik'}
                </span>
              </div>
              <h2>{task.title}</h2>
              <p className="task-description">
                {task.description || 'Bu görev için açıklama eklenmemiş.'}
              </p>
              <div className="task-due">
                <Icon name="clock" size={16} />
                {dateTime(task.startAt)}
                {new Date(task.startAt) < new Date() &&
                  !['completed', 'cancelled'].includes(task.status) && (
                    <Badge tone="red">Gecikti</Badge>
                  )}
              </div>
              <div className="task-card-footer">
                <button className="text-button" onClick={() => setChecklist(task)}>
                  <Icon name="check" size={16} />
                  Alt görevler ({task.checklist?.filter((c) => c.isDone).length || 0}/
                  {task.checklist?.length || 0})
                </button>
                <Badge tone={task.status === 'completed' ? 'green' : ''}>
                  {statuses[task.status]}
                </Badge>
              </div>
              <div className="task-card-actions">
                <button className="button secondary compact" onClick={() => setEdit(task)}>
                  <Icon name="edit" size={15} />
                  Düzenle
                </button>
                <ConfirmButton
                  className="icon-button danger"
                  message={`“${task.title}” ve alt görevleri silinecek.`}
                  onConfirm={async () => {
                    await api(`calendar/events/${task.id}`, { method: 'DELETE' });
                    reload();
                    notify('Görev silindi.');
                  }}
                >
                  <Icon name="trash" size={16} />
                </ConfirmButton>
              </div>
            </article>
          ))}
        </div>
      )}
      {edit && (
        <EventEditor task event={edit === 'new' ? null : edit} onClose={() => setEdit(null)} />
      )}
      {checklist && <ChecklistPanel task={checklist} onClose={() => setChecklist(null)} />}
    </>
  );
}

export function AcademicCalendar() {
  const [month, setMonth] = useState(() => {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1);
  });
  const [selected, setSelected] = useState(localDate());
  const { data, error, retry } = useResource<Event[]>('calendar/events');
  const { reload, notify } = usePortal();
  const [edit, setEdit] = useState<Event | 'new' | null>(null);
  const firstDay = (month.getDay() + 6) % 7;
  const count = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  const selectedEvents = data?.filter((e) => localDate(e.startAt) === selected);
  function shift(amount: number) {
    setMonth((current) => new Date(current.getFullYear(), current.getMonth() + amount, 1));
  }
  return (
    <>
      <PageTitle
        eyebrow="ZAMANINI TASARLA"
        title="Akademik takvim"
        description="Sınavlarını, teslim tarihlerini ve önemli günlerini bir arada gör."
        action={
          <button className="button" onClick={() => setEdit('new')}>
            <Icon name="plus" size={17} />
            Etkinlik ekle
          </button>
        }
      />
      {error ? (
        <ErrorNotice error={error} retry={retry} />
      ) : !data ? (
        <Loading />
      ) : (
        <div className="calendar-layout">
          <section className="panel month-panel">
            <div className="panel-heading">
              <h2>{month.toLocaleDateString('tr-TR', { month: 'long', year: 'numeric' })}</h2>
              <div className="row-actions">
                <button className="icon-button" onClick={() => shift(-1)} aria-label="Önceki ay">
                  ←
                </button>
                <button
                  className="text-button"
                  onClick={() => {
                    const d = new Date();
                    setMonth(new Date(d.getFullYear(), d.getMonth(), 1));
                    setSelected(localDate(d));
                  }}
                >
                  Bugün
                </button>
                <button className="icon-button" onClick={() => shift(1)} aria-label="Sonraki ay">
                  →
                </button>
              </div>
            </div>
            <div className="month-grid">
              {['Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt', 'Paz'].map((day) => (
                <span className="month-weekday" key={day}>
                  {day}
                </span>
              ))}
              {Array.from({ length: firstDay }, (_, i) => (
                <span key={`space-${i}`} className="calendar-space" />
              ))}
              {Array.from({ length: count }, (_, i) => {
                const date = localDate(new Date(month.getFullYear(), month.getMonth(), i + 1));
                const events = data.filter((e) => localDate(e.startAt) === date);
                return (
                  <button
                    key={date}
                    className={`calendar-cell ${date === selected ? 'selected' : ''} ${date === localDate() ? 'is-today' : ''}`}
                    aria-pressed={date === selected}
                    aria-label={`${date}, ${events.length} etkinlik`}
                    onClick={() => setSelected(date)}
                  >
                    <span className="calendar-day-number">{i + 1}</span>
                    <div className="calendar-event-titles">
                      {events.slice(0, 2).map((event) => (
                        <span key={event.id}>{event.title}</span>
                      ))}
                      {events.length > 2 && <small>+{events.length - 2} etkinlik</small>}
                    </div>
                    {!!events.length && <i className="calendar-mobile-dot" />}
                  </button>
                );
              })}
            </div>
          </section>
          <section className="panel selected-day">
            <div className="panel-heading">
              <h2>
                {new Date(`${selected}T12:00:00`).toLocaleDateString('tr-TR', {
                  day: 'numeric',
                  month: 'long',
                })}
              </h2>
              <Badge>{selectedEvents?.length || 0} etkinlik</Badge>
            </div>
            {selectedEvents?.length ? (
              selectedEvents.map((event) => (
                <article className="agenda-event" key={event.id}>
                  <Badge tone="blue">{types[event.type]}</Badge>
                  <h3>{event.title}</h3>
                  <p>
                    <Icon name="clock" size={15} />
                    {dateTime(event.startAt)}
                  </p>
                  {event.location && <p>{event.location}</p>}
                  <div className="row-actions">
                    <button className="button secondary compact" onClick={() => setEdit(event)}>
                      Düzenle
                    </button>
                    <ConfirmButton
                      message={`“${event.title}” etkinliği silinecek.`}
                      onConfirm={async () => {
                        await api(`calendar/events/${event.id}`, { method: 'DELETE' });
                        reload();
                        notify('Etkinlik silindi.');
                      }}
                    >
                      <Icon name="trash" size={16} />
                    </ConfirmButton>
                  </div>
                </article>
              ))
            ) : (
              <Empty icon="calendar" title="Planlamak için güzel bir gün.">
                Bu güne kayıtlı etkinliğin yok.
              </Empty>
            )}
          </section>
        </div>
      )}
      {edit && (
        <EventEditor
          task={false}
          event={edit === 'new' ? null : edit}
          onClose={() => setEdit(null)}
        />
      )}
    </>
  );
}
