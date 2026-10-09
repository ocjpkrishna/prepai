import { expect, test } from '@playwright/test';
import { LESSON_TITLE, PROBLEM, signInAsStudent } from './helpers';

test.describe('ask a problem', () => {
  test.beforeEach(async ({ page }) => {
    await signInAsStudent(page);
    await page.goto('/lessons/new');
  });

  test('a student asks a problem and the lesson is generated', async ({ page }) => {
    await page.getByLabel('Your question or problem').fill(PROBLEM);

    const generated = page.waitForResponse((response) =>
      response.url().endsWith('/api/v1/lessons/generate'),
    );
    await page.getByRole('button', { name: 'Explain this' }).click();

    expect((await generated).status()).toBe(201);
  });

  test('an empty question is not sent', async ({ page }) => {
    let sent = false;
    page.on('request', (request) => {
      if (request.url().endsWith('/api/v1/lessons/generate')) {
        sent = true;
      }
    });

    await page.getByRole('button', { name: 'Explain this' }).click();

    await expect(page.getByText('Type the question you want explained')).toBeVisible();
    expect(sent).toBe(false);
  });

  // The lesson player route (/lessons/:id) is built in row 17. Until then generate navigates to a
  // path that falls through to the not-found page, so the whiteboard cannot be reached.
  test.fixme('the lesson plays on the whiteboard after it is generated', async ({ page }) => {
    await page.getByLabel('Your question or problem').fill(PROBLEM);
    await page.getByRole('button', { name: 'Explain this' }).click();

    await expect(page.getByRole('heading', { name: LESSON_TITLE })).toBeVisible();
    await expect(page.locator('canvas').first()).toBeVisible();
  });
});
