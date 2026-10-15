'use client';

import { useEffect, useId, useRef, useState, type FormEvent, type ReactNode } from 'react';

const paths: Record<string, ReactNode> = {
  grid: (
    <>
      <rect x="3" y="3" width="7" height="7" rx="1.5" />
      <rect x="14" y="3" width="7" height="7" rx="1.5" />
      <rect x="3" y="14" width="7" height="7" rx="1.5" />
      <rect x="14" y="14" width="7" height="7" rx="1.5" />
    </>
  ),
  book: (
    <>
      <path d="M12 6c-3-3-7-3-10-2v15c3-1 7-1 10 2 3-3 7-3 10-2V4c-3-1-7-1-10 2Z" />
      <path d="M12 6v15" />
    </>
  ),
  calendar: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="3" />
      <path d="M7 3v4m10-4v4M3 11h18m-13 4h.01M12 15h.01M16 15h.01" />
    </>
  ),
  check: (
    <>
      <rect x="3" y="3" width="18" height="18" rx="4" />
      <path d="m7 12 3 3 7-7" />
    </>
  ),
  chart: (
    <>
      <path d="M4 3v17h17M8 15l4-5 4 2 5-8" />
    </>
  ),
  clock: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  cap: (
    <>
      <path d="m2 9 10-5 10 5-10 5L2 9Zm4 3v5c4 3 8 3 12 0v-5M22 9v7" />
    </>
  ),
  settings: (
    <>
      <path d="m9 3-1 3-3 1-2 4 2 3 1 4 4 3 3-1 4-1 3-4-1-3-1-4-4-3-3 1Z" />
      <circle cx="12" cy="12" r="3" />
    </>
  ),
  arrow: <path d="M4 12h15m-6-6 6 6-6 6" />,
  plus: <path d="M12 5v14M5 12h14" />,
  close: <path d="m6 6 12 12M6 18 18 6" />,
  search: (
    <>
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="m16 16 5 5" />
    </>
  ),
  bell: (
    <>
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
    </>
  ),
  logout: (
    <>
      <path d="M9 4H4v16h5m5-12 4 4-4 4m-5-4h12" />
    </>
  ),
  menu: <path d="M4 6h16M4 12h16M4 18h16" />,
  file: (
    <>
      <path d="M14 3H5v18h14V8l-5-5Zm0 0v5h5M8 12h8m-8 4h8" />
    </>
  ),
  download: (
    <>
      <path d="M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5" />
    </>
  ),
  trash: (
    <>
      <path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7" />
    </>
  ),
  edit: (
    <>
      <path d="m15 4 5 5M4 20l5-1L21 7l-5-5L4 14v6Z" />
    </>
  ),
  target: (
    <>
      <circle cx="12" cy="12" r="9" />
      <circle cx="12" cy="12" r="5" />
      <circle cx="12" cy="12" r="1" />
    </>
  ),
  shield: (
    <>
      <path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Z" />
      <path d="m8 12 3 3 5-6" />
    </>
  ),
  sun: (
    <>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1 1m12 12 1 1M5 19l1-1M18 6l1-1" />
    </>
  ),
};
export function Icon({
  name,
  size = 20,
  ...props
}: {
  name: string;
  size?: number;
  className?: string;
}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      {paths[name] || paths.book}
    </svg>
  );
}
export function Brand({ light = false }: { light?: boolean }) {
  return (
    <span className={`brand ${light ? 'brand-light' : ''}`}>
      <span className="brand-mark">
        <Icon name="cap" size={25} />
      </span>
      <span>
        Uni<span className="brand-accent">Track</span>
        <small>ÖĞRENCİ PORTALI</small>
      </span>
    </span>
  );
}
export function Empty({
  icon = 'book',
  title,
  children,
  action,
}: {
  icon?: string;
  title: string;
  children: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="empty">
      <span className="empty-icon">
        <Icon name={icon} size={28} />
      </span>
      <h3>{title}</h3>
      <p>{children}</p>
      {action}
    </div>
  );
}
export function Loading() {
  return (
    <div className="loading" role="status">
      <span className="spinner" />
      <span>Bilgilerin yükleniyor…</span>
    </div>
  );
}
export function ErrorNotice({ error, retry }: { error: string; retry?: () => void }) {
  return (
    <div className="notice error" role="alert">
      <span>{error}</span>
      {retry && (
        <button className="text-button" onClick={retry}>
          Tekrar dene
        </button>
      )}
    </div>
  );
}
export function PageTitle({
  eyebrow = 'ÖĞRENCİ PORTALI',
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="page-title">
      <div>
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action}
    </div>
  );
}
export function Modal({
  title,
  children,
  onClose,
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const id = useId();
  useEffect(() => {
    const dialog = ref.current;
    dialog?.showModal();
    return () => dialog?.close();
  }, []);
  return (
    <dialog
      ref={ref}
      className="modal"
      aria-labelledby={id}
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      onClick={(e) => {
        if (
          e.target === e.currentTarget &&
          (e.clientX < e.currentTarget.getBoundingClientRect().left ||
            e.clientX > e.currentTarget.getBoundingClientRect().right ||
            e.clientY < e.currentTarget.getBoundingClientRect().top ||
            e.clientY > e.currentTarget.getBoundingClientRect().bottom)
        )
          onClose();
      }}
    >
      <div className="modal-header">
        <h2 id={id}>{title}</h2>
        <button className="icon-button" onClick={onClose} aria-label="Pencereyi kapat">
          <Icon name="close" />
        </button>
      </div>
      {children}
    </dialog>
  );
}
export function AsyncForm({
  children,
  onSave,
  label = 'Kaydet',
  className = '',
}: {
  children: ReactNode;
  onSave: (data: FormData) => Promise<void>;
  label?: string;
  className?: string;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (busy) return;
    const data = new FormData(e.currentTarget);
    setBusy(true);
    setError('');
    try {
      await onSave(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'İşlem tamamlanamadı.');
    } finally {
      setBusy(false);
    }
  }
  return (
    <form className={`form-stack ${className}`} onSubmit={submit} aria-busy={busy}>
      <fieldset disabled={busy}>{children}</fieldset>
      {error && <ErrorNotice error={error} />}
      <div className="form-footer">
        <button className="button" disabled={busy} type="submit">
          {busy ? (
            <>
              <span className="spinner" />
              Kaydediliyor…
            </>
          ) : (
            label
          )}
        </button>
      </div>
    </form>
  );
}
export function Field({
  label,
  children,
  hint,
}: {
  label: string;
  children: ReactNode;
  hint?: string;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}
export function ConfirmButton({
  onConfirm,
  children,
  message,
  className = 'icon-button danger',
  disabled = false,
}: {
  onConfirm: () => Promise<void>;
  children: ReactNode;
  message: string;
  className?: string;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  return (
    <>
      <button
        className={className}
        disabled={disabled}
        onClick={() => setOpen(true)}
        aria-label={typeof children === 'string' ? children : 'Sil'}
      >
        {children}
      </button>
      {open && (
        <Modal
          title="İşlemi onayla"
          onClose={() => {
            if (!busy) setOpen(false);
          }}
        >
          <p className="confirm-copy">{message}</p>
          <AsyncForm
            label="Evet, devam et"
            onSave={async () => {
              setBusy(true);
              try {
                await onConfirm();
                setOpen(false);
              } finally {
                setBusy(false);
              }
            }}
          >
            <p className="muted">Bu işlem geri alınamaz.</p>
          </AsyncForm>
        </Modal>
      )}
    </>
  );
}
export function Badge({ children, tone = '' }: { children: ReactNode; tone?: string }) {
  return <span className={`badge ${tone}`}>{children}</span>;
}
export const number = (value: number, digits = 2) =>
  value.toLocaleString('tr-TR', { minimumFractionDigits: digits, maximumFractionDigits: digits });
export const dateTime = (value: string) =>
  new Date(value).toLocaleString('tr-TR', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
