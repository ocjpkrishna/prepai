import { expect, test } from '@playwright/test';
import { signInAsStudent } from './helpers';

test.describe('pricing', () => {
  test('the plans page lists the free and paid plans with prices', async ({ page }) => {
    await page.goto('/pricing');

    await expect(page.getByRole('heading', { name: 'Free' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Pro', exact: true })).toBeVisible();
    await expect(page.getByText('₹199 / month')).toBeVisible();
    await expect(page.getByText('₹399 / month')).toBeVisible();
  });

  test('a visitor who chooses a paid plan is sent to register', async ({ page }) => {
    await page.goto('/pricing');
    await page.getByRole('button', { name: 'Choose Pro', exact: true }).click();

    await expect(page).toHaveURL(/\/register$/);
  });

  test('a student who chooses the free plan goes to the lesson form', async ({ page }) => {
    await signInAsStudent(page);
    await page.goto('/pricing');
    await page.getByRole('button', { name: 'Start free' }).click();

    await expect(page).toHaveURL(/\/lessons\/new$/);
  });
});
