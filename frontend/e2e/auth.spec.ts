import { expect, test } from '@playwright/test';
import { logInThroughForm, STUDENT } from './helpers';

test.describe('sign up and sign in', () => {
  test('a new student signs up and is asked to verify the email', async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel('Name').fill(STUDENT.name);
    await page.getByLabel('Email').fill(STUDENT.email);
    await page.getByLabel('Password', { exact: true }).fill(STUDENT.password);
    await page.getByLabel('Date of birth').fill('2000-01-01');
    await page.getByRole('checkbox', { name: /I accept the Terms/ }).check();
    await page.getByRole('button', { name: 'Create account' }).click();

    await expect(page).toHaveURL(/\/verify-email$/);
  });

  test('a registered student logs in and reaches the dashboard', async ({ page }) => {
    await logInThroughForm(page);

    await expect(page).toHaveURL(/\/dashboard$/);
  });

  test('a visitor who opens a protected page is sent to login and returns after signing in', async ({
    page,
  }) => {
    await page.goto('/lessons/new');
    await expect(page).toHaveURL(/\/login\?returnUrl=%2Flessons%2Fnew$/);

    await page.getByLabel('Email').fill(STUDENT.email);
    await page.getByLabel('Password', { exact: true }).fill(STUDENT.password);
    await page.getByRole('button', { name: 'Log in' }).click();

    await expect(page).toHaveURL(/\/lessons\/new$/);
  });
});
