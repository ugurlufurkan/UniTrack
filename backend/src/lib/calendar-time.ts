const formatters = new Map<string, Intl.DateTimeFormat>();
export function wallTime(date: Date, zone: string) {
  let format = formatters.get(zone);
  if (!format) {
    format = new Intl.DateTimeFormat("en-CA", {
      timeZone: zone,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hourCycle: "h23",
    });
    formatters.set(zone, format);
  }
  const p = Object.fromEntries(
    format.formatToParts(date).map((part) => [part.type, Number(part.value)]),
  );
  return new Date(
    Date.UTC(
      p.year,
      p.month - 1,
      p.day,
      p.hour,
      p.minute,
      p.second,
      date.getUTCMilliseconds(),
    ),
  );
}
export function fromWallTime(wall: Date, zone: string) {
  let estimate = wall.getTime(),
    previous: number | undefined;
  for (let n = 0; n < 5; n++) {
    const next =
      estimate + wall.getTime() - wallTime(new Date(estimate), zone).getTime();
    if (next === estimate) return new Date(next);
    if (next === previous) return new Date(Math.max(estimate, next)); // nonexistent DST wall time: move forward
    previous = estimate;
    estimate = next;
  }
  return new Date(estimate);
}

export function calendarDay(
  now = new Date(),
  zone = process.env.APP_TIMEZONE || "Europe/Istanbul",
) {
  const local = wallTime(now, zone);
  const start = new Date(
    Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate()),
  );
  const end = new Date(start.getTime() + 86400000);
  return {
    weekday: local.getUTCDay(),
    start: fromWallTime(start, zone),
    end: fromWallTime(end, zone),
  };
}
