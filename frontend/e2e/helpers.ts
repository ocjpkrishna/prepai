import { expect, Page } from '@playwright/test';

export const STUDENT = {
  name: 'Asha',
  email: 'student@example.com',
  password: 'correct-horse-battery',
};
export const LESSON_TITLE = 'Projectile Motion: Angled Launch';
export const PROBLEM =
  'A particle is projected at 60 degrees with speed 20 m/s. Find the time of flight.';

const ACCESS_TOKEN_KEY = 'prepai.accessToken';

/** Starts the page as a logged-in student without going through the login form. */
export async function signInAsStudent(page: Page): Promise<void> {
  await page.addInitScript(
    (key) => localStorage.setItem(key, 'mock-access-token'),
    ACCESS_TOKEN_KEY,
  );
}

export async function logInThroughForm(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Email').fill(STUDENT.email);
  await page.getByLabel('Password', { exact: true }).fill(STUDENT.password);
  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
}
