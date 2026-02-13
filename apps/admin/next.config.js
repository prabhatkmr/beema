/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  transpilePackages: ['@beema/ui'],
  output: 'standalone',
};

module.exports = nextConfig;
