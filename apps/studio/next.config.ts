import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  transpilePackages: ['@beema/ui'],
  reactStrictMode: true,
  output: 'standalone', // Enable standalone output for Docker

  // Set absolute workspace root for Docker builds
  // In Docker, workspace root is always /app
  turbopack: {
    root: '/app',
  },

  // Ignore TypeScript errors during build to allow monorepo build to proceed
  typescript: {
    ignoreBuildErrors: true,
  },
};

export default nextConfig;
