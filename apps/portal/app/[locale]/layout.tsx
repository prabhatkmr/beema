import type { Metadata } from "next";
import { Inter } from "next/font/google";
import { NextIntlClientProvider } from 'next-intl';
import { getMessages } from 'next-intl/server';
import { notFound } from 'next/navigation';
import { locales } from '../../i18n';
import { LanguageSwitcher } from '../../components/LanguageSwitcher';
import "../globals.css";

const inter = Inter({ subsets: ["latin"] });

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
    <html lang={locale}>
      <body className={inter.className}>
        <NextIntlClientProvider messages={messages}>
          <nav className="border-b bg-background">
            <div className="container mx-auto px-4">
              <div className="flex items-center justify-between py-4">
                <h1 className="text-xl font-bold text-primary">Beema Portal</h1>
                <LanguageSwitcher />
              </div>
            </div>
          </nav>
          <main className="container mx-auto px-4 py-6">{children}</main>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
