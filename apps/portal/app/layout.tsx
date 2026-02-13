import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Beema Portal",
  description: "Insurance portal with dynamic form rendering",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <nav className="border-b bg-background">
          <div className="container mx-auto px-4">
            <div className="flex items-center gap-6 py-4">
              <h1 className="text-xl font-bold text-primary">Beema Portal</h1>
            </div>
          </div>
        </nav>
        <main className="container mx-auto px-4 py-6">{children}</main>
      </body>
    </html>
  );
}
