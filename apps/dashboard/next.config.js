/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  transpilePackages: ['@beema/ui'],
  output: 'standalone',
  async rewrites() {
    return [
      {
        source: '/studio/:path*',
        destination: 'http://localhost:3010/:path*', // Studio will run on 3010
      },
      {
        source: '/portal/:path*',
        destination: 'http://localhost:3011/:path*', // Portal will run on 3011
      },
    ];
  },
};

module.exports = nextConfig;
