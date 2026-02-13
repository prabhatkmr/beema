'use client';

import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  FileText,
  AlertCircle,
  Send,
  User,
  BarChart3,
  Settings
} from "lucide-react";

export default function DashboardPage() {
  const t = useTranslations('legacyDashboard');
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{t('title')}</h1>
              <p className="text-sm text-gray-600">{t('subtitle')}</p>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-sm text-gray-700">Jane Underwriter</span>
              <Avatar className="h-9 w-9">
                <AvatarFallback className="bg-blue-600 text-white">JU</AvatarFallback>
              </Avatar>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Welcome Section */}
        <div className="mb-8">
          <h2 className="text-3xl font-bold text-gray-900 mb-2">{t('heading')}</h2>
          <p className="text-gray-600">{t('description')}</p>
        </div>

        {/* Navigation Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
          {/* Policies Card */}
          <DashboardCard
            href="/dashboard/policies"
            icon={<FileText className="w-8 h-8" />}
            title={t('policies')}
            description={t('policiesDesc')}
            color="blue"
            stats="5 Active"
          />

          {/* Claims Card */}
          <DashboardCard
            href="/dashboard/claims"
            icon={<AlertCircle className="w-8 h-8" />}
            title={t('claims')}
            description={t('claimsDesc')}
            color="red"
            stats="2 Pending"
          />

          {/* Submissions Card */}
          <DashboardCard
            href="/dashboard/submissions"
            icon={<Send className="w-8 h-8" />}
            title={t('submissions')}
            description={t('submissionsDesc')}
            color="green"
            stats="3 In Review"
          />

          {/* Profile Card */}
          <DashboardCard
            href="/dashboard/profile"
            icon={<User className="w-8 h-8" />}
            title={t('profile')}
            description={t('profileDesc')}
            color="purple"
          />

          {/* Analytics Card */}
          <DashboardCard
            href="/dashboard/analytics"
            icon={<BarChart3 className="w-8 h-8" />}
            title={t('analytics')}
            description={t('analyticsDesc')}
            color="yellow"
          />

          {/* Settings Card */}
          <DashboardCard
            href="/dashboard/settings"
            icon={<Settings className="w-8 h-8" />}
            title={t('settings')}
            description={t('settingsDesc')}
            color="gray"
          />
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <StatCard title={t('totalPolicies')} value="23" trend="+2 this month" />
          <StatCard title={t('activeClaims')} value="7" trend="3 resolved" />
          <StatCard title={t('premiumVolume')} value="$248K" trend="+12% YoY" />
          <StatCard title={t('pendingReviews')} value="5" trend="2 urgent" />
        </div>
      </main>
    </div>
  );
}

// Dashboard navigation card component
function DashboardCard({
  href,
  icon,
  title,
  description,
  color,
  stats
}: {
  href: string;
  icon: React.ReactNode;
  title: string;
  description: string;
  color: 'blue' | 'red' | 'green' | 'purple' | 'yellow' | 'gray';
  stats?: string;
}) {
  const colorClasses = {
    blue: 'from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700',
    red: 'from-red-500 to-red-600 hover:from-red-600 hover:to-red-700',
    green: 'from-green-500 to-green-600 hover:from-green-600 hover:to-green-700',
    purple: 'from-purple-500 to-purple-600 hover:from-purple-600 hover:to-purple-700',
    yellow: 'from-yellow-500 to-yellow-600 hover:from-yellow-600 hover:to-yellow-700',
    gray: 'from-gray-500 to-gray-600 hover:from-gray-600 hover:to-gray-700',
  };

  return (
    <Link
      href={href}
      className={`block p-6 rounded-xl bg-linear-to-br ${colorClasses[color]} text-white shadow-lg hover:shadow-xl transition-all duration-200 transform hover:-translate-y-1`}
    >
      <div className="flex items-start justify-between mb-4">
        <div className="p-3 bg-white/20 rounded-lg backdrop-blur-sm">
          {icon}
        </div>
        <svg className="w-5 h-5 opacity-70" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
        </svg>
      </div>
      <h3 className="text-xl font-bold mb-2">{title}</h3>
      <p className="text-white/90 text-sm mb-3">{description}</p>
      {stats && (
        <p className="text-sm font-semibold bg-white/20 rounded-full px-3 py-1 inline-block">
          {stats}
        </p>
      )}
    </Link>
  );
}

// Stat card component
function StatCard({
  title,
  value,
  trend
}: {
  title: string;
  value: string;
  trend: string;
}) {
  return (
    <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm">
      <h4 className="text-sm font-medium text-gray-600 mb-2">{title}</h4>
      <p className="text-3xl font-bold text-gray-900 mb-1">{value}</p>
      <p className="text-sm text-green-600">{trend}</p>
    </div>
  );
}
