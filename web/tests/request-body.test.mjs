import { test } from 'node:test';
import assert from 'node:assert/strict';
import { BodyLimitError, readBoundedBody } from '../lib/request-body.mjs';

test('gateway reads valid multipart/JSON bytes without modifying them', async () => {
  const request = new Request('http://example.invalid/', { method: 'POST', body: 'hello' });
  assert.equal(new TextDecoder().decode(await readBoundedBody(request, 5)), 'hello');
});

test('gateway cancels an oversized chunked stream before buffering the remaining body', async () => {
  let pulled = 0,
    cancelled = false;
  const body = new ReadableStream(
    {
      pull(controller) {
        pulled++;
        controller.enqueue(new Uint8Array(3));
        if (pulled === 100) controller.close();
      },
      cancel() {
        cancelled = true;
      },
    },
    { highWaterMark: 0 },
  );
  const request = new Request('http://example.invalid/', { method: 'POST', body, duplex: 'half' });
  await assert.rejects(readBoundedBody(request, 5), BodyLimitError);
  assert.ok(cancelled);
  assert.ok(pulled < 100);
  await assert.rejects(
    readBoundedBody(
      new Request('http://example.invalid/', {
        method: 'POST',
        body: 'hello',
        headers: { 'Content-Length': '999' },
      }),
      5,
    ),
    BodyLimitError,
  );
});
