'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Users,
  Globe,
  Database,
  Activity,
  Shield,
  ArrowLeft,
} from 'lucide-react';

const navItems = [
  { href: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/tenants', label: 'Tenants', icon: Users },
  { href: '/regions', label: 'Regions', icon: Globe },
  { href: '/datasources', label: 'Datasources', icon: Database },
  { href: '/health', label: 'System Health', icon: Activity },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-64 bg-[#1E293B] text-white flex flex-col">
      {/* Logo */}
      <div className="px-6 py-5 border-b border-slate-700">
        <Link href="/dashboard" className="flex items-center space-x-3">
          <Shield className="w-7 h-7 text-blue-400" />
          <span className="text-lg font-bold">Beema Admin</span>
        </Link>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        {navItems.map((item) => {
          const isActive = pathname === item.href || pathname?.startsWith(item.href + '/');
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-blue-600 text-white'
                  : 'text-slate-300 hover:bg-slate-700 hover:text-white'
              }`}
            >
              <Icon className="w-5 h-5" />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>

      {/* Footer */}
      <div className="px-3 py-4 border-t border-slate-700">
        <a
          href="http://localhost:3000"
          className="flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm text-slate-400 hover:bg-slate-700 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Back to Hub</span>
        </a>
      </div>
    </aside>
  );
}
