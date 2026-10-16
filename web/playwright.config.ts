import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://127.0.0.1:3198',
    trace: 'retain-on-failure',
    extraHTTPHeaders: { 'x-forwarded-for': '203.0.113.20' },
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: 'node ../backend/tests/e2e-server.cjs',
      url: 'http://127.0.0.1:5105/api/health',
      reuseExistingServer: false,
    },
    {
      command: 'node node_modules/next/dist/bin/next start --hostname 127.0.0.1 --port 3198',
      url: 'http://127.0.0.1:3198/login',
      reuseExistingServer: false,
      env: {
        API_URL: 'http://127.0.0.1:5105/api/v1',
        WEB_PROXY_SECRET: 'e2e-web-proxy-secret-at-least-32-characters',
        WEB_CLIENT_IP_HEADER: 'x-forwarded-for',
        NEXT_PUBLIC_GOOGLE_CLIENT_ID: '',
      },
    },
  ],
});
