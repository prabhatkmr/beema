import type { Locale } from '../i18n';

// Locale-specific configurations
export const localeConfig: Record<Locale, {
  currency: string;
  currencyDisplay: 'symbol' | 'code' | 'name';
  dateFormat: Intl.DateTimeFormatOptions;
  timeZone: string;
}> = {
  'en-GB': {
    currency: 'GBP',
    currencyDisplay: 'symbol',
    dateFormat: {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    },
    timeZone: 'Europe/London'
  },
  'en-US': {
    currency: 'USD',
    currencyDisplay: 'symbol',
    dateFormat: {
      month: 'short',
      day: '2-digit',
      year: 'numeric'
    },
    timeZone: 'America/New_York'
  },
  'es': {
    currency: 'EUR',
    currencyDisplay: 'symbol',
    dateFormat: {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    },
    timeZone: 'Europe/Madrid'
  }
};
