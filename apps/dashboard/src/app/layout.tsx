import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Beema Platform - Dashboard',
  description: 'AI-powered insurance platform with metadata-driven architecture',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
