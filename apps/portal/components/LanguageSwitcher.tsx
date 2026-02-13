'use client';

import { usePathname, useRouter } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { locales } from '../i18n';
import { announceToScreenReader } from '@/lib/accessibility-utils';

type LocaleInfo = {
  flag: string;
  name: string;
};

const localeInfo: Record<string, LocaleInfo> = {
  'en-GB': { flag: '🇬🇧', name: 'English (UK)' },
  'en-US': { flag: '🇺🇸', name: 'English (US)' },
  'es-ES': { flag: '🇪🇸', name: 'Español (España)' }
};

export function LanguageSwitcher() {
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();
  const t = useTranslations('common');

  const switchLocale = (newLocale: string) => {
    // Replace the current locale in the pathname with the new one
    const segments = pathname.split('/');
    segments[1] = newLocale;
    const newPathname = segments.join('/');

    // Announce locale change to screen readers
    announceToScreenReader(`Language changed to ${localeInfo[newLocale].name}`, 'polite');

    router.push(newPathname);
  };

  return (
    <label className="flex items-center gap-2">
      <span className="sr-only">Select language</span>
      <select
        value={locale}
        onChange={(e) => switchLocale(e.target.value)}
        className="px-3 py-1.5 text-sm border rounded-md bg-background focus:ring-2 focus:ring-blue-500 focus:outline-none transition-shadow"
        aria-label="Language selector"
        style={{ fontFamily: 'system-ui, -apple-system, sans-serif' }}
      >
        {locales.map((loc) => (
          <option key={loc} value={loc}>
            {localeInfo[loc].flag} {localeInfo[loc].name}
          </option>
        ))}
      </select>
    </label>
  );
}
