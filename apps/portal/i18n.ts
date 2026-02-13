import { getRequestConfig } from 'next-intl/server';
import { notFound } from 'next/navigation';

// Can be imported from a shared config
export const locales = ['en-GB', 'en-US', 'es-ES'] as const;
export type Locale = (typeof locales)[number];

export default getRequestConfig(async ({ requestLocale }) => {
  // requestLocale is the locale from the request (URL, cookie, header, etc.)
  let locale = await requestLocale;

  // Validate that the incoming `locale` parameter is valid
  if (!locale || !locales.includes(locale as Locale)) {
    locale = 'en-GB';
  }

  return {
    locale,
    messages: (await import(`./locales/${locale}.json`)).default,
    // Configure locale-specific formatting
    timeZone: locale === 'en-US' ? 'America/New_York' :
              locale === 'es-ES' ? 'Europe/Madrid' : 'Europe/London',
    now: new Date()
  };
});
