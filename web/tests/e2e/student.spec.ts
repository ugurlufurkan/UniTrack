import { test, expect } from '@playwright/test';

test('student can register, manage academic data, sign out and sign in again', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  const email = `student-${Date.now()}@example.invalid`;
  await page.goto('/register');
  await page.getByLabel('Ad soyad').fill('Test Öğrenci');
  await page.getByLabel('E-posta adresi').fill(email);
  await page.getByLabel('Şifre', { exact: true }).fill('UniTrackTest123!');
  await page.getByLabel('Şifre tekrar', { exact: true }).fill('UniTrackTest123!');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Hesabımı oluştur' }).click();
  await expect(page).toHaveURL('/');
  await page.getByRole('link', { name: 'Dönemlerim', exact: true }).click();
  await page.getByRole('button', { name: 'Dönem ekle' }).click();
  await page.getByLabel('Akademik yıl başlangıcı').fill('2026');
  await page.getByRole('dialog').getByRole('button', { name: 'Kaydet', exact: true }).click();
  await expect(page.getByText('Güz dönemi', { exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Dersler ve notlar', exact: true }).click();
  await page.getByRole('button', { name: 'Ders ekle', exact: true }).click();
  await page.getByLabel('Ders adı', { exact: true }).fill('Veri Yapıları');
  await page.getByLabel('Kredi', { exact: true }).fill('3');
  await page.getByLabel('Vize notu').fill('80');
  await page.getByLabel('Final notu').fill('90');
  await page.getByRole('dialog').getByRole('button', { name: 'Kaydet', exact: true }).click();
  await expect(page.getByText('Veri Yapıları', { exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByText('Veri Yapıları', { exact: true })).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  const dimensions = await page.evaluate(() => ({
    width: innerWidth,
    document: document.documentElement.scrollWidth,
    outside: [...document.querySelectorAll('body *')]
      .map((el) => ({
        tag: el.tagName,
        class: el.className,
        right: el.getBoundingClientRect().right,
      }))
      .filter((el) => el.right > innerWidth + 1)
      .slice(0, 12),
  }));
  expect(dimensions, JSON.stringify(dimensions)).toMatchObject({ width: 390, document: 390 });
  await page.getByRole('button', { name: 'Menüyü aç', exact: true }).click();
  await page.getByRole('button', { name: 'Çıkış yap', exact: true }).click();
  await expect(page).toHaveURL('/login');
  await page.getByLabel('E-posta adresi').fill(email);
  await page.getByLabel('Şifre', { exact: true }).fill('UniTrackTest123!');
  await page.getByRole('button', { name: 'Giriş yap' }).click();
  await expect(page).toHaveURL('/');
  expect(errors).toEqual([]);
});
