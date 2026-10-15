const rules = [
  [/^auth\/(login|register|google|forgot-password|reset-password|verify-email)$/, ['POST'], true],
  [/^auth\/(me)$/, ['GET', 'DELETE']],
  [/^auth\/(logout|logout-all|change-password|verify-email\/request)$/, ['POST']],
  [/^auth\/devices$/, ['GET']],
  [/^auth\/devices\/[\w-]+$/, ['DELETE']],
  [/^(semesters|courses)$/, ['GET', 'POST']],
  [/^(semesters|courses)\/[\w-]+$/, ['PUT', 'DELETE']],
  [/^(dashboard|statistics|gpa|transcript|export|settings)$/, ['GET']],
  [/^settings$/, ['PATCH']],
  [/^grade-scale\/default$/, ['GET', 'PUT']],
  [/^calendar\/(events|schedule)$/, ['GET', 'POST']],
  [/^calendar\/(events|schedule)\/[\w-]+$/, ['GET', 'PUT', 'DELETE']],
  [/^calendar\/(summary|events\/upcoming)$/, ['GET']],
  [/^attendance\/(overview|courses)$/, ['GET']],
  [/^attendance\/courses\/[\w-]+$/, ['GET']],
  [/^attendance\/courses\/[\w-]+\/records$/, ['POST']],
  [/^attendance\/courses\/[\w-]+\/settings$/, ['PUT']],
  [/^attendance\/records\/[\w-]+$/, ['PUT', 'DELETE']],
  [/^tasks(\/[\w-]+)?$/, ['GET']],
  [/^tasks\/[\w-]+\/checklist$/, ['POST']],
  [/^tasks\/checklist\/[\w-]+$/, ['PUT', 'DELETE']],
  [/^transcript-import\/(parse|import)$/, ['POST']],
  [/^content\/(announcements|faqs|tips)$/, ['GET']],
  [/^content\/pages\/[a-z0-9-]+$/, ['GET'], true],
];

export function resolveEndpoint(path, method) {
  if (!/^[a-zA-Z0-9/-]+$/.test(path) || path.includes('//')) return null;
  const rule = rules.find(([pattern, methods]) => pattern.test(path) && methods.includes(method));
  return rule ? { public: rule[2] === true } : null;
}

export function isSameOriginMutation(request) {
  if (['GET', 'HEAD'].includes(request.method)) return true;
  const origin = request.headers.get('origin');
  const url = new URL(request.url);
  // Next.js may reconstruct request.url with its internal hostname (localhost).
  // Host is the browser's actual target and cannot be set by cross-origin JavaScript.
  const host = request.headers.get('host') || url.host;
  return request.headers.get('x-unitrack-web') === '1' && origin === `${url.protocol}//${host}`;
}
