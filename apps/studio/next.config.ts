import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  transpilePackages: ['@beema/ui'],
  reactStrictMode: true,
};

export default nextConfig;
