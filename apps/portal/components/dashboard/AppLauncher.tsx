import Link from "next/link";
import type { LucideIcon } from "lucide-react";

interface AppLauncherProps {
  title: string;
  icon: LucideIcon;
  href: string;
  color: string;
  description?: string;
  badge?: string | number;
  badgeVariant?: "default" | "warning" | "error" | "success";
}

export function AppLauncher({
  title,
  icon: Icon,
  href,
  color,
  description,
  badge,
  badgeVariant = "default",
}: AppLauncherProps) {
  const badgeColors = {
    default: "bg-blue-500",
    warning: "bg-yellow-500",
    error: "bg-red-500",
    success: "bg-green-500",
  };

  const badgeLabel = badge !== undefined && badge !== 0 ? ` - ${badge} pending items` : '';
  const ariaLabel = `${title}${description ? `: ${description}` : ''}${badgeLabel}`;

  return (
    <Link
      href={href}
      className="flex flex-col items-center gap-3 group relative focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 rounded-lg p-2 -m-2 transition-shadow"
      aria-label={ariaLabel}
    >
      <div
        className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${color} flex items-center justify-center shadow-lg transition-transform group-hover:scale-105 group-focus:scale-105 relative`}
        aria-hidden="true"
      >
        <Icon className="w-8 h-8 text-white" />
        {badge !== undefined && badge !== 0 && (
          <div
            className={`absolute -top-1 -right-1 ${badgeColors[badgeVariant]} text-white text-xs font-bold rounded-full w-6 h-6 flex items-center justify-center shadow-md`}
            aria-label={`${badge} pending`}
          >
            {badge}
          </div>
        )}
      </div>
      <span className="text-sm font-semibold text-gray-700 text-center">
        {title}
      </span>
      {badge !== undefined && badge !== 0 && (
        <span className="text-xs text-gray-500" aria-hidden="true">
          {badge} pending
        </span>
      )}
    </Link>
  );
}
