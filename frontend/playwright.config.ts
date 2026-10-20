import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  timeout: 45_000,
  expect: { timeout: 10_000 },
  forbidOnly: !!process.env['CI'],
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env['DEMO_URL'] ?? 'http://127.0.0.1:8080',
    screenshot: 'only-on-failure',
    trace: 'off',
    launchOptions: process.env['PLAYWRIGHT_CHROMIUM_EXECUTABLE']
      ? {
          executablePath: process.env['PLAYWRIGHT_CHROMIUM_EXECUTABLE'],
          args: ['--no-sandbox', '--disable-dev-shm-usage'],
        }
      : {},
  },
  projects: [
    { name: 'desktop', use: { viewport: { width: 1440, height: 1080 } } },
    {
      name: 'phone',
      use: { viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true },
    },
  ],
});
