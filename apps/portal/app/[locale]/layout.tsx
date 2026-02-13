import type { Metadata } from "next";
import { NextIntlClientProvider } from 'next-intl';
import { getMessages } from 'next-intl/server';
import { notFound } from 'next/navigation';
import { locales } from '../../i18n';
import { LanguageSwitcher } from '../../components/LanguageSwitcher';
import { QuotesInProgressWrapper } from '../../components/QuotesInProgressWrapper';

export const metadata: Metadata = {
  title: "Beema Portal",
  description: "Insurance portal with dynamic form rendering",
};

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

export default async function LocaleLayout({
  children,
  params
}: {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}) {
  // Await params in Next.js 15
  const { locale } = await params;

  // Validate that the incoming `locale` parameter is valid
  if (!locales.includes(locale as any)) {
    notFound();
  }

  // Providing all messages to the client
  // side is the easiest way to get started
  const messages = await getMessages();

  return (
    <NextIntlClientProvider messages={messages}>
      <a href="#main-content" className="skip-link">
        Skip to main content
      </a>
      <nav className="border-b bg-background" role="navigation" aria-label="Main navigation">
        <div className="container mx-auto px-4">
          <div className="flex items-center justify-between py-4">
            <h1 className="text-xl font-bold text-primary">Best Insurance Company</h1>
            <div className="flex items-center gap-3">
              <QuotesInProgressWrapper />
              <LanguageSwitcher />
            </div>
          </div>
        </div>
      </nav>
      <main id="main-content" className="container mx-auto px-4 py-6" role="main">
        {children}
      </main>
    </NextIntlClientProvider>
  );
}
