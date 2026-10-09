import { expect, test } from '@playwright/test';
import { LESSON_TITLE, signInAsStudent } from './helpers';

test('a student sees past lessons on the dashboard', async ({ page }) => {
  await signInAsStudent(page);
  await page.goto('/dashboard');

  await expect(page.getByText(LESSON_TITLE)).toBeVisible();
  await expect(page.getByText('Balancing Redox Equations')).toBeVisible();
});
