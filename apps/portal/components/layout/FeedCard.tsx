"use client";

import React from "react";
import { ChevronRight, type LucideIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

interface FeedCardProps {
  icon: LucideIcon;
  title: string;
  subtitle: string;
  metadata?: string;
  status?: string;
  statusColor?: "default" | "secondary" | "destructive" | "outline";
  onClick?: () => void;
  active?: boolean;
  className?: string;
}

export function FeedCard({
  icon: Icon,
  title,
  subtitle,
  metadata,
  status,
  statusColor = "secondary",
  onClick,
  active = false,
  className,
}: FeedCardProps) {
  return (
    <div
      role="button"
      tabIndex={0}
      aria-label={`${title}${status ? `, ${status}` : ''}`}
      onClick={onClick}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onClick?.();
        }
      }}
      className={cn(
        "flex items-center gap-3 border-b px-4 py-3 cursor-pointer transition-colors",
        "hover:bg-slate-50 dark:hover:bg-slate-800/50",
        active && "bg-slate-100 dark:bg-slate-800",
        className
      )}
    >
      {/* Left: Icon/Avatar */}
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-muted">
        <Icon className="h-5 w-5 text-muted-foreground" />
      </div>

      {/* Middle: Text content */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold truncate">{title}</p>
        <p className="text-sm text-muted-foreground truncate">{subtitle}</p>
        {metadata && (
          <p className="text-xs text-muted-foreground/70 mt-0.5">{metadata}</p>
        )}
      </div>

      {/* Right: Status badge + Chevron */}
      <div className="flex items-center gap-2 shrink-0">
        {status && (
          <Badge variant={statusColor} className="rounded-full text-xs">
            {status}
          </Badge>
        )}
        <ChevronRight className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
      </div>
    </div>
  );
}
