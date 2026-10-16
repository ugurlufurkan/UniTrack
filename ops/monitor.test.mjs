import { test } from 'node:test';
import assert from 'node:assert/strict';
import { advance } from './monitor.mjs';

test('monitor alerts after three failures, throttles unchanged state and reports recovery once', () => {
  let state = advance({}, false, 1000);
  assert.equal(state.alert, null);
  state = advance(state, false, 2000);
  state = advance(state, false, 3000);
  assert.equal(state.alert, 'failed');
  state = advance(state, false, 4000);
  assert.equal(state.alert, null);
  state = advance(state, false, 904000);
  assert.equal(state.alert, 'failed');
  state = advance(state, true, 905000);
  assert.equal(state.alert, 'recovered');
  assert.equal(advance(state, true, 906000).alert, null);
  assert.equal(advance(state, true, 906000).pending, 'recovered', 'undelivered recovery must survive the next healthy cycle');
});
