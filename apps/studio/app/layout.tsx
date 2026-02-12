import type { Metadata } from "next";
import { Inter } from "next/font/google";
import Link from "next/link";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Beema Studio",
  description: "Visual layout builder for insurance forms",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <nav className="border-b">
          <div className="container mx-auto px-4">
            <div className="flex items-center gap-6 py-4">
              <h1 className="text-xl font-bold text-[#1E40AF]">
                Beema Studio
              </h1>
              <div className="flex gap-6">
                <Link href="/" className="hover:text-blue-600">
                  Home
                </Link>
                <Link href="/canvas" className="hover:text-blue-600">
                  Canvas
                </Link>
                <Link href="/blueprints" className="hover:text-blue-600">
                  Blueprints
                </Link>
                <Link href="/webhooks" className="hover:text-blue-600">
                  Webhooks
                </Link>
              </div>
            </div>
          </div>
        </nav>
        {children}
      </body>
    </html>
  );
}
