import createMiddleware from 'next-intl/middleware';
import { locales } from './i18n';

export default createMiddleware({
  // A list of all locales that are supported
  locales,

  // Used when no locale matches
  defaultLocale: 'en-GB',

  // Always show locale in URL for clarity
  localePrefix: 'always'
});

export const config = {
  // Match all pathnames except for
  // - api routes
  // - _next (Next.js internals)
  // - _vercel (Vercel internals)
  // - files with extensions (e.g. .png, .css)
  matcher: ['/', '/((?!api|_next|_vercel|.*\\..*).*)']
};
