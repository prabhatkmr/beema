import type { Metadata } from "next";
import { Inter } from "next/font/google";
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
          <div className="container mx-auto px-4 py-4">
            <h1 className="text-xl font-bold text-[#1E40AF]">
              Beema Studio
            </h1>
          </div>
        </nav>
        {children}
      </body>
    </html>
  );
}
