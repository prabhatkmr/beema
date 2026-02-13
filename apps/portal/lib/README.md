# Locale Formatting Utilities

This directory contains utilities for locale-aware formatting of currency, dates, numbers, and other data types.

## Features

### Currency Formatting
Automatically formats currency based on the user's locale:
- **en-GB** 🇬🇧: £15,000.00 (GBP)
- **en-US** 🇺🇸: $15,000.00 (USD)
- **es-ES** 🇪🇸: 15.000,00 € (EUR)

### Number Formatting
Uses locale-specific decimal separators and grouping:
- **en-GB** 🇬🇧: 1,234,567.89
- **es-ES** 🇪🇸: 1.234.567,89

### Date Formatting
Shows dates in locale-appropriate formats:
- **en-GB** 🇬🇧: 13 Feb 2026
- **en-US** 🇺🇸: Feb 13, 2026
- **es-ES** 🇪🇸: 13 feb 2026

### Percentage Formatting
Formats percentages with locale-specific decimal separators:
- **en-GB** 🇬🇧: 15.37%
- **es-ES** 🇪🇸: 15,37 %

## Usage

### Client Components

```typescript
'use client';

import { useFormatters } from '@/lib/format-utils';

export default function MyComponent() {
  const {
    formatCurrency,
    formatNumber,
    formatDate,
    formatPercent,
    formatDateTime,
    formatRelativeTime,
    getCurrencySymbol,
  } = useFormatters();

  return (
    <div>
      <p>Premium: {formatCurrency(15000)}</p>
      <p>Policies: {formatNumber(12847)}</p>
      <p>Date: {formatDate(new Date())}</p>
      <p>Growth: {formatPercent(0.1537)}</p>
      <p>Updated: {formatRelativeTime(-3, 'day')}</p>
    </div>
  );
}
```

### Server Components

```typescript
import { serverFormatters } from '@/lib/format-utils';

export default function ServerComponent({ locale }: { locale: Locale }) {
  const amount = serverFormatters.formatCurrency(15000, locale);
  const date = serverFormatters.formatDate(new Date(), locale);

  return (
    <div>
      <p>{amount}</p>
      <p>{date}</p>
    </div>
  );
}
```

## Configuration

Locale-specific settings are defined in [locale-config.ts](locale-config.ts):

```typescript
export const localeConfig = {
  'en-GB': {
    currency: 'GBP',
    timeZone: 'Europe/London',
    dateFormat: { day: '2-digit', month: 'short', year: 'numeric' }
  },
  // ...
};
```

## Adding New Locales

To add support for a new locale:

1. Add the locale to `i18n.ts`:
   ```typescript
   export const locales = ['en-GB', 'en-US', 'es-ES', 'fr-FR'] as const;
   ```

2. Add configuration in `locale-config.ts`:
   ```typescript
   'fr-FR': {
     currency: 'EUR',
     currencyDisplay: 'symbol',
     dateFormat: { day: '2-digit', month: 'short', year: 'numeric' },
     timeZone: 'Europe/Paris'
   }
   ```

3. Add locale info with flag in `LanguageSwitcher.tsx`:
   ```typescript
   'fr-FR': { flag: '🇫🇷', name: 'Français (France)' }
   ```

4. Create translation file at `locales/fr-FR.json`

## API Reference

### `useFormatters()` Hook

Returns an object with the following methods:

- **`formatCurrency(amount: number)`** - Format amount as currency
- **`formatNumber(value: number, options?: Intl.NumberFormatOptions)`** - Format number
- **`formatDate(date: Date | string | number, options?: Intl.DateTimeFormatOptions)`** - Format date
- **`formatDateTime(date: Date | string | number)`** - Format date and time
- **`formatRelativeTime(value: number, unit: Intl.RelativeTimeFormatUnit)`** - Format relative time (e.g., "3 days ago")
- **`formatPercent(value: number, decimals?: number)`** - Format percentage
- **`getCurrencySymbol()`** - Get current locale's currency symbol
- **`getCurrency()`** - Get current locale's currency code
- **`getTimeZone()`** - Get current locale's time zone

### Server Formatters

For server-side rendering:

- **`serverFormatters.formatCurrency(amount: number, locale: Locale)`**
- **`serverFormatters.formatDate(date: Date | string | number, locale: Locale)`**

## Examples

See [portal/dashboard/page.tsx](../app/[locale]/portal/dashboard/page.tsx) for a complete example of using these formatters in a component.
