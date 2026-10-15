'use client';
import Link from 'next/link';
import { usePortal, useResource } from './portal-provider';
import { Badge, Empty, ErrorNotice, Icon, Loading, number, PageTitle, dateTime } from './ui';
import type { Dashboard as DashboardData, Event, Schedule } from '@/lib/types';
import { semesterLabel } from '@/lib/academic.mjs';

export function Dashboard() {
  const { user, semesters, semesterId, courses, settings } = usePortal();
  const { data, error, retry } = useResource<DashboardData>('dashboard');
  const schedule = useResource<Schedule[]>('calendar/schedule');
  const tasks = useResource<Event[]>('tasks');
  const name = (user.name || user.email.split('@')[0]).split(' ')[0];
  const semester = semesters.find((s) => s.id === semesterId);
  const activeCourses = courses.filter((c) => c.semesterId === semesterId);
  const now = new Date();
  const today = schedule.data
    ?.filter((s) => s.dayOfWeek === now.getDay() && activeCourses.some((c) => c.id === s.courseId))
    .sort((a, b) => a.startTime.localeCompare(b.startTime));
  const todo = tasks.data
    ?.filter((t) => !['completed', 'cancelled'].includes(t.status))
    .slice(0, 4);
  if (error) return <ErrorNotice error={error} retry={retry} />;
  if (!data) return <Loading />;
  const target = settings.targetGpa;
  return (
    <>
      <PageTitle
        eyebrow="HER GÜN YENİ BİR BAŞLANGIÇ"
        title={`Merhaba, ${name} 👋`}
        description="Bugün de hedeflerine bir adım daha yaklaşalım."
        action={
          <Link className="button" href="/tasks">
            <Icon name="plus" size={17} />
            Görev ekle
          </Link>
        }
      />
      <div className="dashboard-hero">
        <div className="hero-copy">
          <Badge tone="hero-badge">
            <span className="status-dot" />
            {semester ? semesterLabel(semester) : 'Yolculuğuna başla'}
          </Badge>
          <h2>
            Planın hazır.
            <br />
            <span>Sıra sende.</span>
          </h2>
          <p>
            {semester
              ? 'Derslerin, hedeflerin ve yaklaşan teslimlerin. Hepsi kontrolün altında.'
              : 'İlk dönemini oluştur, derslerini ekle ve kendi akademik alanını kur.'}
          </p>
          <Link href={semester ? '/courses' : '/semesters'} className="hero-link">
            {semester ? 'Derslerime göz at' : 'İlk dönemimi oluştur'}
            <Icon name="arrow" size={18} />
          </Link>
        </div>
        <div className="hero-visual" aria-hidden="true">
          <div className="orbit orbit-one" />
          <div className="orbit orbit-two" />
          <div className="hero-ring">
            <Icon name="cap" size={58} />
          </div>
          <div className="floating-label float-one">
            <span>✦</span> Bir adım ileri
          </div>
          <div className="floating-label float-two">
            <Icon name="check" size={18} />
            Senin yolculuğun
          </div>
          <span className="hero-star star-one">✳</span>
          <span className="hero-star star-two">✦</span>
        </div>
      </div>
      <div className="metric-grid">
        <Metric
          title="Genel ortalaman"
          value={number(data.gpa)}
          subtitle="GANO · kredi ağırlıklı"
          icon="chart"
          tone="blue"
        />
        <Metric
          title="Bu dönemki dersler"
          value={String(activeCourses.length)}
          subtitle={`${activeCourses.reduce((s, c) => s + c.credit, 0)} kredi ders yükü`}
          icon="book"
          tone="purple"
        />
        <Metric
          title="Tamamlanan dersler"
          value={String(data.passedCourses)}
          subtitle={`${data.ongoingCourses} ders devam ediyor`}
          icon="check"
          tone="green"
        />
        <Metric
          title="Devamsızlık durumu"
          value={data.attendanceOverview ? `${data.attendanceOverview.atRiskCourses}` : '—'}
          subtitle={
            data.attendanceOverview ? 'ders devamsızlık sınırında' : 'Bilgi şu anda alınamıyor'
          }
          icon="clock"
          tone="amber"
        />
      </div>
      <div className="dashboard-columns">
        <section className="panel">
          <div className="panel-heading">
            <div>
              <span className="section-kicker">GÜNÜN AKIŞI</span>
              <h2>Bugünkü derslerin</h2>
            </div>
            <Link href="/schedule" className="text-button">
              Programı gör
              <Icon name="arrow" size={16} />
            </Link>
          </div>
          {schedule.error ? (
            <ErrorNotice error={schedule.error} retry={schedule.retry} />
          ) : !today ? (
            <Loading />
          ) : today.length ? (
            <div className="today-list">
              {today.map((lesson, i) => (
                <div className="today-lesson" key={lesson.id}>
                  <div className="lesson-time">
                    <strong>{lesson.startTime}</strong>
                    <span>{lesson.endTime}</span>
                  </div>
                  <span className={`lesson-stripe color-${i % 3}`} />
                  <div className="lesson-info">
                    <h3>{lesson.courseName}</h3>
                    <p>{lesson.location || 'Konum eklenmemiş'}</p>
                  </div>
                  <Badge>DERS</Badge>
                </div>
              ))}
            </div>
          ) : (
            <Empty icon="sun" title="Bugün programın açık.">
              Bu dönem için bugün kayıtlı bir ders yok. Çalışmak ya da dinlenmek için kendine zaman
              ayır.
            </Empty>
          )}
        </section>
        <section className="panel">
          <div className="panel-heading">
            <div>
              <span className="section-kicker">BİR SONRAKİ ADIM</span>
              <h2>Yaklaşan görevlerin</h2>
            </div>
            <Link href="/tasks" className="text-button">
              Tümü
              <Icon name="arrow" size={16} />
            </Link>
          </div>
          {tasks.error ? (
            <ErrorNotice error={tasks.error} retry={tasks.retry} />
          ) : !todo ? (
            <Loading />
          ) : todo.length ? (
            <div className="task-preview-list">
              {todo.map((task) => (
                <Link href="/tasks" key={task.id} className="task-preview">
                  <span className="task-preview-check" />
                  <div>
                    <strong>{task.title}</strong>
                    <span>{dateTime(task.startAt)}</span>
                  </div>
                  <span className={`priority-dot ${task.priority}`} />
                </Link>
              ))}
            </div>
          ) : (
            <Empty icon="check" title="Aklında kalan bir iş yok.">
              Yeni görevlerini eklediğinde burada göreceksin.
            </Empty>
          )}
        </section>
      </div>
      <div className="dashboard-bottom">
        <section className="goal-card">
          <span className="goal-icon">
            <Icon name="target" size={25} />
          </span>
          <div>
            <h3>
              {target !== null
                ? `${number(target)} GANO hedefin için devam et.`
                : 'Bir hedef belirle, yönünü bul.'}
            </h3>
            <p>
              {target !== null
                ? `Şu anki ortalaman ${number(data.gpa)}. Olası notlarını simülatörde dene.`
                : 'Hedef ortalamanı ayarlayarak akademik yolculuğunu planla.'}
            </p>
          </div>
          <Link className="button secondary" href={target !== null ? '/gpa' : '/settings'}>
            {target !== null ? 'Simüle et' : 'Hedef belirle'}
            <Icon name="arrow" size={16} />
          </Link>
        </section>
        <Link href="/transcript" className="transcript-shortcut">
          <Icon name="file" size={25} />
          <div>
            <strong>Akademik geçmişin</strong>
            <span>Transkriptini incele ve yazdır</span>
          </div>
          <Icon name="arrow" />
        </Link>
      </div>
    </>
  );
}
export function Metric({
  title,
  value,
  subtitle,
  icon,
  tone,
}: {
  title: string;
  value: string;
  subtitle: string;
  icon: string;
  tone: string;
}) {
  return (
    <div className="metric">
      <div className="metric-top">
        <span>{title}</span>
        <span className={`metric-icon ${tone}`}>
          <Icon name={icon} />
        </span>
      </div>
      <strong className="metric-value">{value}</strong>
      <p>{subtitle}</p>
    </div>
  );
}
