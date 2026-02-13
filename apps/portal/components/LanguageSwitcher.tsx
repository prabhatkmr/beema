'use client';

import { usePathname, useRouter } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { locales } from '../i18n';
import { announceToScreenReader } from '@/lib/accessibility-utils';

export function LanguageSwitcher() {
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();
  const t = useTranslations('common');

  const switchLocale = (newLocale: string) => {
    const localeNames: Record<string, string> = {
      'en-GB': 'English (UK)',
      'en-US': 'English (US)',
      'es': 'Español'
    };

    // Replace the current locale in the pathname with the new one
    const segments = pathname.split('/');
    segments[1] = newLocale;
    const newPathname = segments.join('/');

    // Announce locale change to screen readers
    announceToScreenReader(`Language changed to ${localeNames[newLocale]}`, 'polite');

    router.push(newPathname);
  };

  const localeNames: Record<string, string> = {
    'en-GB': 'English (UK)',
    'en-US': 'English (US)',
    'es': 'Español'
  };

  return (
    <label className="flex items-center gap-2">
      <span className="sr-only">Select language</span>
      <select
        value={locale}
        onChange={(e) => switchLocale(e.target.value)}
        className="px-3 py-1 text-sm border rounded-md bg-background focus:ring-2 focus:ring-blue-500 focus:outline-none transition-shadow"
        aria-label="Language selector"
      >
        {locales.map((loc) => (
          <option key={loc} value={loc}>
            {localeNames[loc]}
          </option>
        ))}
      </select>
    </label>
  );
}
