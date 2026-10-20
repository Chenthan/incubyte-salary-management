import { expect, Page, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { readFileSync, mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const envFile = readFileSync(resolve('..', '.env'), 'utf8');
const local = Object.fromEntries(
  envFile
    .split(/\r?\n/)
    .filter((line) => /^[A-Z_]+=/.test(line))
    .map((line) => {
      const at = line.indexOf('=');
      return [line.slice(0, at), line.slice(at + 1)];
    }),
);
async function login(page: Page): Promise<void> {
  await page.goto('/login');
  await page
    .getByLabel('Username', { exact: true })
    .fill(process.env['HR_USERNAME'] ?? local['HR_USERNAME']);
  await page
    .getByLabel('Password', { exact: true })
    .fill(process.env['HR_PASSWORD'] ?? local['HR_PASSWORD']);
  await page.getByRole('button', { name: 'Sign in to workspace' }).click();
  await expect(page.getByRole('heading', { name: 'Your people, at a glance.' })).toBeVisible();
  await expect(page.getByTestId('headcount')).not.toBeEmpty();
}

test('sign-in, create, salary validation, report refresh, and sign-out', async ({
  page,
  request,
}) => {
  expect((await request.get('/api/employees')).status()).toBe(401);
  await page.goto('/overview');
  await expect(page).toHaveURL(/\/login/);
  await login(page);
  const code = `E2E-${Date.now()}`;
  await page.getByRole('link', { name: 'Add employee' }).click();
  await page.getByLabel('Employee ID', { exact: true }).fill(code);
  await page.getByLabel('First name', { exact: true }).fill('Demo');
  await page.getByLabel('Last name', { exact: true }).fill('Browser');
  await page
    .getByLabel('Work email', { exact: true })
    .fill(`${code.toLowerCase()}@example.invalid`);
  await page.getByLabel('Annual base salary', { exact: true }).fill('1000.00');
  await page.getByRole('button', { name: 'Create employee', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Employee details', exact: true })).toBeVisible();
  await expect(page.getByText('Employee created.', { exact: true })).toBeVisible();
  await page.getByLabel('Annual base salary', { exact: true }).fill('-1');
  await page.getByRole('button', { name: 'Save changes', exact: true }).click();
  await expect(
    page.getByText('Enter 0.01–9,999,999,999.99 with at most two decimal places.', { exact: true }),
  ).toBeVisible();
  await page.getByLabel('Annual base salary', { exact: true }).fill('1250.25');
  await page.getByRole('button', { name: 'Save changes', exact: true }).click();
  await expect(
    page.getByText('Changes saved. Salary reports now use this updated record.', { exact: true }),
  ).toBeVisible();
  const apiPath = '/api' + new URL(page.url()).pathname;
  const current: { version: number } = await (await page.request.get(apiPath)).json();
  const token: { token: string; headerName: string } = await (
    await page.request.get('/api/auth/csrf')
  ).json();
  const externalEdit = await page.request.put(apiPath, {
    headers: { [token.headerName]: token.token },
    data: {
      employeeCode: code,
      firstName: 'Demo',
      lastName: 'Browser',
      email: `${code.toLowerCase()}@example.invalid`,
      countryCode: 'US',
      departmentCode: 'ENGINEERING',
      annualSalary: '1300.00',
      currencyCode: 'USD',
      version: current.version,
    },
  });
  expect(externalEdit.status()).toBe(200);
  await page.getByLabel('Annual base salary', { exact: true }).fill('1400.00');
  await page.getByRole('button', { name: 'Save changes', exact: true }).click();
  await expect(
    page.getByRole('button', { name: 'Reload latest record', exact: true }),
  ).toBeVisible();
  await expect(page.getByLabel('Annual base salary', { exact: true })).toHaveValue('1400.00');
  await page.getByRole('button', { name: 'Reload latest record', exact: true }).click();
  await expect(page.getByLabel('Annual base salary', { exact: true })).toHaveValue('1300.00');
  await page.getByLabel('Annual base salary', { exact: true }).fill('1250.25');
  await page.getByRole('button', { name: 'Save changes', exact: true }).click();
  await expect(
    page.getByText('Changes saved. Salary reports now use this updated record.', { exact: true }),
  ).toBeVisible();
  await page.goto(`/overview?q=${code}`);
  await expect(page.getByTestId('headcount')).toHaveText('1');
  await expect(page.getByTestId('salary-total')).toHaveText('USD 1,250.25');
  await page.getByRole('link', { name: new RegExp(code) }).click();
  await page.getByRole('button', { name: 'Deactivate employee', exact: true }).click();
  await expect(
    page.getByText(
      'Employee deactivated. The record is retained and excluded from active reports.',
      { exact: true },
    ),
  ).toBeVisible();
  await page.goto(`/overview?q=${code}`);
  await expect(page.getByTestId('headcount')).toHaveText('0');
  await page.getByRole('button', { name: 'Sign out', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Welcome back.' })).toBeVisible();
  expect((await page.request.get('/api/employees')).status()).toBe(401);
});

test('filters, pagination, and empty states use the selected population', async ({ page }) => {
  await login(page);
  await page.getByRole('navigation').getByRole('link', { name: 'Employees', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Employees', exact: true })).toBeVisible();
  await page.getByLabel('Country', { exact: true }).selectOption('IN');
  await page.getByLabel('Department', { exact: true }).selectOption('ENGINEERING');
  await page.getByRole('button', { name: 'Apply filters' }).click();
  await expect(page).toHaveURL(/country=IN/);
  await expect(page.getByRole('cell', { name: /^IN\s*India$/ }).first()).toBeVisible();
  const first = await page.locator('.employee-link').first().getAttribute('href');
  await page.getByRole('button', { name: 'Next', exact: true }).click();
  await expect(page).toHaveURL(/page=1/);
  await expect(page.locator('.employee-link').first()).not.toHaveAttribute('href', first ?? '');
  await page.getByLabel('Search people', { exact: true }).fill('NO-MATCH-EMPLOYEE-XYZ');
  await page.getByRole('button', { name: 'Apply filters' }).click();
  await expect(page.getByRole('heading', { name: 'No people in this view' })).toBeVisible();
  await expect(page.getByTestId('headcount')).toHaveText('0');
});

test('overview renders accessibly at desktop and phone sizes', async ({ page }, info) => {
  await login(page);
  await expect(page.getByRole('heading', { name: 'Where salary cost sits' })).toBeVisible();
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1),
  ).toBeTruthy();
  const accessibility = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
    .analyze();
  expect(
    accessibility.violations.map((item) => ({
      id: item.id,
      targets: item.nodes.map((node) => node.target),
    })),
  ).toEqual([]);
  if (process.env['CAPTURE_DEMO'] === '1') {
    const directory = resolve('..', 'docs', 'screenshots');
    mkdirSync(directory, { recursive: true });
    await page.screenshot({
      path: resolve(directory, `overview-${info.project.name}.png`),
      fullPage: true,
    });
  }
});
