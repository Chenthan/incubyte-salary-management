// A real browser recording. Pauses intentionally leave time to read each demonstrated state.
import { chromium, expect } from '@playwright/test';
import { readFileSync, mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve('..');
const configuration = Object.fromEntries(
  readFileSync(resolve(root, '.env'), 'utf8')
    .split(/\r?\n/)
    .filter((line) => /^[A-Z_]+=/.test(line))
    .map((line) => {
      const at = line.indexOf('=');
      return [line.slice(0, at), line.slice(at + 1)];
    }),
);
const browser = await chromium.launch(
  process.env['PLAYWRIGHT_CHROMIUM_EXECUTABLE']
    ? {
        executablePath: process.env['PLAYWRIGHT_CHROMIUM_EXECUTABLE'],
        args: ['--no-sandbox', '--disable-dev-shm-usage'],
      }
    : {},
);
const context = await browser.newContext({
  viewport: { width: 1280, height: 900 },
  baseURL: process.env['DEMO_URL'] ?? 'http://127.0.0.1:8080',
  recordVideo: { dir: resolve(root, 'artifacts', 'demo'), size: { width: 1280, height: 900 } },
});
const page = await context.newPage();
const video = page.video();
mkdirSync(resolve(root, 'docs', 'screenshots'), { recursive: true });
try {
  await page.goto('/login');
  await page.screenshot({
    path: resolve(root, 'docs', 'screenshots', 'login.png'),
    fullPage: true,
  });
  await page.waitForTimeout(1800);
  await page.getByLabel('Username', { exact: true }).fill(configuration['HR_USERNAME']);
  await page.getByLabel('Password', { exact: true }).fill(configuration['HR_PASSWORD']);
  await page.getByRole('button', { name: 'Sign in to workspace' }).click();
  await expect(page.getByTestId('headcount')).toHaveText('9,500');
  await page.waitForTimeout(2800);
  await page.getByLabel('Country', { exact: true }).selectOption('IN');
  await page.getByLabel('Department', { exact: true }).selectOption('ENGINEERING');
  await page.getByRole('button', { name: 'Apply filters' }).click();
  await expect(page).toHaveURL(/country=IN/);
  await expect(page.getByTestId('headcount')).toBeVisible();
  await page.waitForTimeout(2200);
  await page.getByRole('link', { name: /View all employees/ }).click();
  await expect(page.getByRole('heading', { name: 'Employees', exact: true })).toBeVisible();
  await expect(page.locator('.employee-link').first()).toBeVisible();
  await page.waitForTimeout(2300);
  await page.getByRole('link', { name: 'Add employee' }).click();
  const code = `DEMO-${Date.now()}`;
  await page.getByLabel('Employee ID', { exact: true }).fill(code);
  await page.getByLabel('First name', { exact: true }).fill('Jordan');
  await page.getByLabel('Last name', { exact: true }).fill('Demo');
  await page
    .getByLabel('Work email', { exact: true })
    .fill(`${code.toLowerCase()}@example.invalid`);
  await page.getByLabel('Country', { exact: true }).selectOption('IN');
  await page.getByLabel('Salary currency', { exact: true }).selectOption('INR');
  await page.getByLabel('Annual base salary', { exact: true }).fill('1000000.00');
  await page.waitForTimeout(1700);
  await page.getByRole('button', { name: 'Create employee', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Employee details', exact: true })).toBeVisible();
  await expect(page.getByLabel('Annual base salary', { exact: true })).toHaveValue('1000000.00');
  await page.screenshot({
    path: resolve(root, 'docs', 'screenshots', 'employee-detail.png'),
    fullPage: true,
  });
  await page.waitForTimeout(2000);
  await page.getByLabel('Annual base salary', { exact: true }).fill('1200000.00');
  await page.getByRole('button', { name: 'Save changes', exact: true }).click();
  await expect(
    page.getByText('Changes saved. Salary reports now use this updated record.', { exact: true }),
  ).toBeVisible();
  await page.waitForTimeout(1800);
  await page.goto(`/overview?q=${code}`);
  await expect(page.getByTestId('salary-total')).toHaveText('USD 14,400.00');
  await page.waitForTimeout(2500);
  await page.getByRole('link', { name: new RegExp(code) }).click();
  await page.getByRole('button', { name: 'Deactivate employee', exact: true }).click();
  await expect(
    page.getByText(
      'Employee deactivated. The record is retained and excluded from active reports.',
      { exact: true },
    ),
  ).toBeVisible();
  await page.goto('/overview');
  await expect(page.getByTestId('headcount')).toHaveText('9,500');
  await page.waitForTimeout(2000);
  await page.getByRole('button', { name: 'Sign out', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Welcome back.' })).toBeVisible();
  await page.waitForTimeout(1200);
} finally {
  await context.close();
  if (video) await video.saveAs(resolve(root, 'docs', 'demo.webm'));
  await browser.close();
}
console.log('Recorded browser demo in docs/demo.webm; no credentials or session state exported.');
