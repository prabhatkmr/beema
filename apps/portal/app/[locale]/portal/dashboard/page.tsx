'use client';

import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { AppLauncher } from "@/components/dashboard/AppLauncher"
import { useTranslations } from 'next-intl';
import { useFormatters } from "@/lib/format-utils";
import {
  ScrollText,
  AlertCircle,
  CreditCard,
  PencilRuler,
  Users,
  BarChart3,
  Settings,
  UserCircle,
  TrendingUp,
  Calendar,
  Calculator,
  ClipboardCheck,
  FileStack,
} from "lucide-react"
import type { LucideIcon } from "lucide-react"

export default function DashboardPage() {
  const t = useTranslations('dashboard');
  const tApps = useTranslations('apps');
  const { formatCurrency, formatNumber, formatDate, formatPercent } = useFormatters();

  const apps: {
    title: string
    icon: LucideIcon
    href: string
    color: string
    description: string
    badge?: number
    badgeVariant?: "default" | "warning" | "error" | "success"
  }[] = [
    {
      title: tApps('policy.title'),
      icon: ScrollText,
      href: "/portal/policy",
      color: "from-blue-500 to-blue-600",
      description: tApps('policy.description'),
      badge: 5,
      badgeVariant: "default",
    },
    {
      title: tApps('claim.title'),
      icon: AlertCircle,
      href: "/portal/claims",
      color: "from-red-500 to-red-600",
      description: tApps('claim.description'),
      badge: 3,
      badgeVariant: "error",
    },
    {
      title: tApps('submissions.title'),
      icon: FileStack,
      href: "/portal/submissions",
      color: "from-cyan-500 to-cyan-600",
      description: tApps('submissions.description'),
      badge: 0,
    },
    {
      title: tApps('billing.title'),
      icon: CreditCard,
      href: "/portal/billing",
      color: "from-emerald-500 to-emerald-600",
      description: tApps('billing.description'),
      badge: 2,
      badgeVariant: "warning",
    },
    {
      title: tApps('rating.title'),
      icon: Calculator,
      href: "/portal/rating",
      color: "from-teal-500 to-teal-600",
      description: tApps('rating.description'),
      badge: 0,
    },
    {
      title: tApps('underwriting.title'),
      icon: ClipboardCheck,
      href: "/portal/underwriting",
      color: "from-amber-500 to-amber-600",
      description: tApps('underwriting.description'),
      badge: 4,
      badgeVariant: "warning",
    },
    {
      title: tApps('studio.title'),
      icon: PencilRuler,
      href: "/portal/studio",
      color: "from-purple-500 to-purple-600",
      description: tApps('studio.description'),
      badge: 0,
    },
    {
      title: tApps('agents.title'),
      icon: Users,
      href: "/portal/agents",
      color: "from-orange-500 to-orange-600",
      description: tApps('agents.description'),
      badge: 7,
      badgeVariant: "default",
    },
    {
      title: tApps('analytics.title'),
      icon: BarChart3,
      href: "/portal/analytics",
      color: "from-indigo-500 to-indigo-600",
      description: tApps('analytics.description'),
      badge: 0,
    },
    {
      title: tApps('profile.title'),
      icon: UserCircle,
      href: "/portal/profile",
      color: "from-slate-500 to-slate-600",
      description: tApps('profile.description'),
      badge: 0,
    },
    {
      title: tApps('settings.title'),
      icon: Settings,
      href: "/portal/settings",
      color: "from-gray-600 to-gray-700",
      description: tApps('settings.description'),
      badge: 1,
      badgeVariant: "warning",
    },
  ];

  // Sample data demonstrating locale-aware formatting
  const stats = [
    {
      label: "Total Premium Volume",
      value: formatCurrency(1567893.45),
      icon: TrendingUp,
      change: formatPercent(0.1237)
    },
    {
      label: "Active Policies",
      value: formatNumber(12847),
      icon: ScrollText,
      change: formatNumber(423) + " new"
    },
    {
      label: "Last Updated",
      value: formatDate(new Date()),
      icon: Calendar,
      change: "Today"
    }
  ];

  return (
    <div className="space-y-10">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">
            {t('greeting', { name: 'Jane' })}
          </h2>
          <p className="text-sm text-muted-foreground">
            {t('welcome')}
          </p>
        </div>
        <Avatar className="h-10 w-10">
          <AvatarFallback className="bg-blue-600 text-white text-sm">
            JU
          </AvatarFallback>
        </Avatar>
      </div>

      {/* Locale-aware Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {stats.map((stat, index) => (
          <div
            key={index}
            className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm"
          >
            <div className="flex items-center justify-between mb-2">
              <p className="text-sm font-medium text-gray-600">{stat.label}</p>
              <stat.icon className="w-4 h-4 text-gray-400" />
            </div>
            <p className="text-2xl font-bold text-gray-900 mb-1">{stat.value}</p>
            <p className="text-xs text-green-600 font-medium">↑ {stat.change}</p>
          </div>
        ))}
      </div>

      {/* Apps Grid */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-6">Quick Access</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-8">
          {apps.map((app) => (
            <AppLauncher
              key={app.href}
              title={app.title}
              icon={app.icon}
              href={app.href}
              color={app.color}
              description={app.description}
              badge={app.badge}
              badgeVariant={app.badgeVariant}
            />
          ))}
        </div>
      </div>
    </div>
  )
}
