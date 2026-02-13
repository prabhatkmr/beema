'use client';

import { usePathname, useRouter } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { locales } from '../i18n';

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
    router.push(newPathname);
  };

  const localeNames: Record<string, string> = {
    'en-GB': 'English (UK)',
    'en-US': 'English (US)',
    'es': 'Español'
  };

  return (
    <select
      value={locale}
      onChange={(e) => switchLocale(e.target.value)}
      className="px-3 py-1 text-sm border rounded-md bg-background"
    >
      {locales.map((loc) => (
        <option key={loc} value={loc}>
          {localeNames[loc]}
        </option>
      ))}
    </select>
  );
}
