"use client";

import React from "react";
import { ChevronLeft, Plus, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface AppShellProps {
  title: string;
  actionLabel: string;
  onAction: () => void;
  searchPlaceholder?: string;
  onBack?: () => void;
  onSearchChange?: (value: string) => void;
  children: React.ReactNode;
  className?: string;
}

export function AppShell({
  title,
  actionLabel,
  onAction,
  searchPlaceholder = "Search...",
  onBack,
  onSearchChange,
  children,
  className,
}: AppShellProps) {
  return (
    <div className={cn("flex h-full flex-col", className)}>
      {/* Sticky Header */}
      <header className="sticky top-0 z-10 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="flex items-center gap-4 px-4 py-3">
          {/* Left: Back button + Title */}
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="icon"
              onClick={onBack}
              aria-label="Go back"
              className="shrink-0"
            >
              <ChevronLeft className="h-5 w-5" />
            </Button>
            <h1 className="text-xl font-bold tracking-tight">{title}</h1>
          </div>

          {/* Middle: Search input */}
          <div className="relative flex-1 max-w-md mx-auto">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={searchPlaceholder}
              onChange={(e) => onSearchChange?.(e.target.value)}
              className="rounded-full bg-muted/50 pl-9 border-0 focus-visible:ring-1"
            />
          </div>

          {/* Right: Action button */}
          <Button
            onClick={onAction}
            className="shrink-0 rounded-full gap-1.5"
          >
            <Plus className="h-4 w-4" />
            {actionLabel}
          </Button>
        </div>
      </header>

      {/* Body: Content area */}
      <main className="flex-1 overflow-auto">{children}</main>
    </div>
  );
}
