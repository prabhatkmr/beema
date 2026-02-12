import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  transpilePackages: ['@beema/ui'],
  reactStrictMode: true,
  output: 'standalone', // Enable standalone output for Docker

  // Set absolute workspace root for Docker builds only
  // In Docker, workspace root is always /app
  ...(process.env.DOCKER_BUILD === 'true' && {
    turbopack: {
      root: '/app',
    },
  }),
};

export default nextConfig;
