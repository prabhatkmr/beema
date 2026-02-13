'use client';

import { useLocale } from 'next-intl';
import type { Locale } from '../i18n';
import { localeConfig } from './locale-config';

/**
 * Hook to get locale-aware formatting functions
 */
export function useFormatters() {
  const locale = useLocale() as Locale;
  const config = localeConfig[locale];

  return {
    /**
     * Format currency based on locale
     * @example formatCurrency(15000) // "£15,000.00" (en-GB), "$15,000.00" (en-US), "15.000,00 €" (es)
     */
    formatCurrency: (amount: number) => {
      return new Intl.NumberFormat(locale, {
        style: 'currency',
        currency: config.currency,
        currencyDisplay: config.currencyDisplay,
      }).format(amount);
    },

    /**
     * Format number with locale-specific separators
     * @example formatNumber(1234567.89) // "1,234,567.89" (en-GB), "1.234.567,89" (es)
     */
    formatNumber: (value: number, options?: Intl.NumberFormatOptions) => {
      return new Intl.NumberFormat(locale, options).format(value);
    },

    /**
     * Format date based on locale
     * @example formatDate(new Date()) // "13 Feb 2026" (en-GB), "Feb 13, 2026" (en-US), "13 feb 2026" (es)
     */
    formatDate: (date: Date | string | number, options?: Intl.DateTimeFormatOptions) => {
      const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
      return new Intl.DateTimeFormat(locale, options || config.dateFormat).format(dateObj);
    },

    /**
     * Format date and time
     * @example formatDateTime(new Date()) // "13 Feb 2026, 14:30" (en-GB)
     */
    formatDateTime: (date: Date | string | number) => {
      const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
      return new Intl.DateTimeFormat(locale, {
        ...config.dateFormat,
        hour: '2-digit',
        minute: '2-digit',
        timeZone: config.timeZone
      }).format(dateObj);
    },

    /**
     * Format relative time
     * @example formatRelativeTime(-3, 'day') // "3 days ago"
     */
    formatRelativeTime: (value: number, unit: Intl.RelativeTimeFormatUnit) => {
      return new Intl.RelativeTimeFormat(locale, { numeric: 'auto' }).format(value, unit);
    },

    /**
     * Format percentage
     * @example formatPercent(0.1537) // "15.37%" (en-GB), "15,37 %" (es)
     */
    formatPercent: (value: number, decimals: number = 2) => {
      return new Intl.NumberFormat(locale, {
        style: 'percent',
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals,
      }).format(value);
    },

    /**
     * Get locale-specific currency symbol
     */
    getCurrencySymbol: () => {
      const parts = new Intl.NumberFormat(locale, {
        style: 'currency',
        currency: config.currency,
      }).formatToParts(0);

      const symbolPart = parts.find(p => p.type === 'currency');
      return symbolPart?.value || config.currency;
    },

    /**
     * Get currency code
     */
    getCurrency: () => config.currency,

    /**
     * Get time zone
     */
    getTimeZone: () => config.timeZone,
  };
}

/**
 * Server-side formatting utilities
 */
export const serverFormatters = {
  formatCurrency: (amount: number, locale: Locale) => {
    const config = localeConfig[locale];
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: config.currency,
      currencyDisplay: config.currencyDisplay,
    }).format(amount);
  },

  formatDate: (date: Date | string | number, locale: Locale) => {
    const config = localeConfig[locale];
    const dateObj = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
    return new Intl.DateTimeFormat(locale, config.dateFormat).format(dateObj);
  },
};
