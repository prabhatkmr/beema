"use client";

import { useState, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from 'next-intl';
import {
  CreditCard,
  Receipt,
  Clock,
  AlertTriangle,
  CheckCircle2,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  X,
  Plus,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { FeedCard } from "@/components/layout/FeedCard";
import { Button } from "@/components/ui/button";
import { BillingDetail } from "@/components/billing/BillingDetail";
import { cn } from "@/lib/utils";
import { useFormatters } from "@/lib/format-utils";

interface Invoice {
  id: string;
  title: string;
  subtitle: string;
  status: string;
  statusColor: "default" | "secondary" | "destructive" | "outline";
  metadata: string;
  icon: LucideIcon;
  dueDate: string;
  amount: number;
}

const mockInvoices: Invoice[] = [
  {
    id: "INV-2024-01",
    title: "Renewal Premium - Acme Corp",
    subtitle: "Commercial Property",
    status: "Paid",
    statusColor: "default",
    metadata: "Due: 15 Mar 2026",
    icon: CheckCircle2,
    dueDate: "Mar 15, 2026",
    amount: 12500,
  },
  {
    id: "INV-2024-02",
    title: "Installment 2 - Jane Smith",
    subtitle: "Professional Indemnity",
    status: "Pending",
    statusColor: "secondary",
    metadata: "Due: 01 Apr 2026",
    icon: Clock,
    dueDate: "Apr 01, 2026",
    amount: 3125,
  },
  {
    id: "INV-2024-03",
    title: "Endorsement Fee - Tech Solutions",
    subtitle: "Cyber Liability",
    status: "Overdue",
    statusColor: "destructive",
    metadata: "Due: 20 Feb 2026",
    icon: AlertTriangle,
    dueDate: "Feb 20, 2026",
    amount: 150,
  },
  {
    id: "INV-2024-04",
    title: "Installment 1 - Global Logistics",
    subtitle: "Marine Cargo",
    status: "Paid",
    statusColor: "default",
    metadata: "Due: 01 Mar 2026",
    icon: CheckCircle2,
    dueDate: "Mar 01, 2026",
    amount: 3125,
  },
  {
    id: "INV-2024-05",
    title: "Annual Premium - Metro Builders",
    subtitle: "Construction All Risk",
    status: "Pending",
    statusColor: "secondary",
    metadata: "Due: 15 Apr 2026",
    icon: Clock,
    dueDate: "Apr 15, 2026",
    amount: 8750,
  },
];

// Multi-select dropdown component
function MultiSelectDropdown({
  label,
  options,
  selected,
  onChange,
  ariaLabel,
}: {
  label: string;
  options: string[];
  selected: string[];
  onChange: (selected: string[]) => void;
  ariaLabel: string;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen]);

  const toggleOption = (option: string) => {
    if (selected.includes(option)) {
      onChange(selected.filter((item) => item !== option));
    } else {
      onChange([...selected, option]);
    }
  };

  const clearAll = () => {
    onChange([]);
  };

  const displayText = selected.length === 0
    ? `All ${label}`
    : selected.length === 1
    ? selected[0]
    : `${selected.length} selected`;

  return (
    <div ref={dropdownRef} className="relative">
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-3 py-1.5 text-sm border rounded-md bg-background hover:bg-muted/50 focus:ring-2 focus:ring-blue-500 focus:outline-none transition-colors"
        aria-label={ariaLabel}
        aria-expanded={isOpen}
      >
        <span className="truncate max-w-[150px]">{displayText}</span>
        {selected.length > 0 && (
          <X
            className="h-3 w-3 text-muted-foreground hover:text-foreground"
            onClick={(e) => {
              e.stopPropagation();
              clearAll();
            }}
          />
        )}
        <ChevronDown className={cn("h-4 w-4 transition-transform", isOpen && "rotate-180")} />
      </button>

      {isOpen && (
        <div className="absolute z-50 mt-1 w-56 bg-background border rounded-md shadow-lg">
          <div className="max-h-64 overflow-y-auto p-2">
            {options.map((option) => (
              <label
                key={option}
                className="flex items-center gap-2 px-2 py-1.5 hover:bg-muted rounded cursor-pointer"
              >
                <input
                  type="checkbox"
                  checked={selected.includes(option)}
                  onChange={() => toggleOption(option)}
                  className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <span className="text-sm">{option}</span>
              </label>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default function BillingCenterPage() {
  const router = useRouter();
  const t = useTranslations('billing');
  const tc = useTranslations('common');
  const { formatCurrency } = useFormatters();
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<string[]>([]);
  const [productFilter, setProductFilter] = useState<string[]>([]);
  const [sortBy, setSortBy] = useState<string>("newest");
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

  // Get unique values for filters
  const uniqueStatuses = Array.from(new Set(mockInvoices.map(i => i.status)));
  const uniqueProducts = Array.from(new Set(mockInvoices.map(i => i.subtitle)));

  // Apply filters and sorting
  let filteredInvoices = mockInvoices.filter((i) => {
    const matchesSearch =
      i.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      i.subtitle.toLowerCase().includes(searchQuery.toLowerCase()) ||
      i.id.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesStatus = statusFilter.length === 0 || statusFilter.includes(i.status);
    const matchesProduct = productFilter.length === 0 || productFilter.includes(i.subtitle);

    return matchesSearch && matchesStatus && matchesProduct;
  });

  // Sort invoices
  filteredInvoices = [...filteredInvoices].sort((a, b) => {
    switch (sortBy) {
      case "newest":
        return b.id.localeCompare(a.id);
      case "oldest":
        return a.id.localeCompare(b.id);
      case "name-asc":
        return a.title.localeCompare(b.title);
      case "name-desc":
        return b.title.localeCompare(a.title);
      case "status":
        return a.status.localeCompare(b.status);
      default:
        return 0;
    }
  });

  return (
    <AppShell
      title={tc('backToDashboard')}
      searchPlaceholder={t('searchPlaceholder')}
      onBack={() => router.push('/portal/dashboard')}
      onSearchChange={setSearchQuery}
      actionSlot={
        <Button
          onClick={() => {/* TODO: Open new invoice form */}}
          className="shrink-0 rounded-full gap-1.5"
        >
          <Plus className="h-4 w-4" />
          {t('newInvoice')}
        </Button>
      }
    >
      <div className="flex flex-col h-full">
        {/* Filter Bar */}
        <div className="border-b bg-muted/30 px-4 py-3">
          <div className="flex items-center gap-3 flex-wrap">
            <span className="text-sm font-medium text-muted-foreground">{tc('filters')}</span>

            <MultiSelectDropdown
              label={t('allStatuses')}
              options={uniqueStatuses}
              selected={statusFilter}
              onChange={setStatusFilter}
              ariaLabel={t('filterByStatus')}
            />

            <MultiSelectDropdown
              label={t('allProducts')}
              options={uniqueProducts}
              selected={productFilter}
              onChange={setProductFilter}
              ariaLabel={t('filterByProduct')}
            />

            {(statusFilter.length > 0 || productFilter.length > 0) && (
              <button
                onClick={() => {
                  setStatusFilter([]);
                  setProductFilter([]);
                }}
                className="text-sm text-blue-600 hover:text-blue-700 underline"
              >
                {tc('clearAllFilters')}
              </button>
            )}

            <div className="ml-auto flex items-center gap-2">
              <span className="text-sm font-medium text-muted-foreground">{tc('sortBy')}</span>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="px-3 py-1.5 text-sm border rounded-md bg-background focus:ring-2 focus:ring-blue-500 focus:outline-none"
                aria-label={t('sortInvoices')}
              >
                <option value="newest">{tc('sort.newest')}</option>
                <option value="oldest">{tc('sort.oldest')}</option>
                <option value="name-asc">{tc('sort.nameAsc')}</option>
                <option value="name-desc">{tc('sort.nameDesc')}</option>
                <option value="status">{tc('sort.status')}</option>
              </select>
            </div>

            <span className="text-sm text-muted-foreground" aria-live="polite" aria-atomic="true">
              {t('count', { count: filteredInvoices.length })}
            </span>
          </div>
        </div>

        {/* Content Grid */}
        <div className="flex-1 overflow-hidden grid grid-cols-12">
          {/* Left Sidebar - Invoice List */}
          {!isSidebarCollapsed && (
            <aside className="col-span-4 overflow-y-auto border-r relative">
            {filteredInvoices.length === 0 ? (
              <div className="flex items-center justify-center py-12 text-sm text-muted-foreground">
                {t('noResults')}
              </div>
            ) : (
              filteredInvoices.map((invoice) => (
                <FeedCard
                  key={invoice.id}
                  icon={invoice.icon}
                  title={invoice.title}
                  subtitle={invoice.subtitle}
                  metadata={`${invoice.id} · ${invoice.metadata}`}
                  status={invoice.status}
                  statusColor={invoice.statusColor}
                  active={selectedInvoice?.id === invoice.id}
                  onClick={() => setSelectedInvoice(invoice)}
                />
              ))
            )}
            <button
              onClick={() => setIsSidebarCollapsed(true)}
              className="absolute top-1/2 -translate-y-1/2 -right-3 px-1.5 py-5 bg-blue-50 hover:bg-blue-100 border-2 border-blue-200 border-l-0 rounded-r-lg transition-all shadow-lg hover:shadow-xl z-20"
              aria-label={tc('collapseSidebar')}
              title={tc('collapseSidebar')}
            >
              <ChevronLeft className="h-5 w-5 text-blue-600" />
            </button>
          </aside>
          )}

          {/* Right Detail Panel */}
          <section className={cn(
            "overflow-hidden flex flex-col",
            isSidebarCollapsed ? "col-span-12" : "col-span-8"
          )}>
            {isSidebarCollapsed && (
              <button
                onClick={() => setIsSidebarCollapsed(false)}
                className="fixed top-1/2 -translate-y-1/2 left-0 px-1.5 py-5 bg-blue-50 hover:bg-blue-100 border-2 border-blue-200 border-l-0 rounded-r-lg transition-all shadow-lg hover:shadow-xl z-20"
                aria-label={tc('expandSidebar')}
                title={tc('expandSidebar')}
              >
                <ChevronRight className="h-5 w-5 text-blue-600" />
              </button>
            )}
            {selectedInvoice ? (
              <BillingDetail invoice={selectedInvoice} />
            ) : (
              <div className="flex h-full items-center justify-center bg-muted/30">
                <div className="text-center">
                  <CreditCard className="mx-auto h-12 w-12 text-muted-foreground/50" />
                  <p className="mt-3 text-sm font-medium text-muted-foreground">
                    {t('selectInvoice')}
                  </p>
                </div>
              </div>
            )}
          </section>
        </div>
      </div>

      {/* Back to Dashboard Button */}
      <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-30">
        <Button onClick={() => router.push('/portal/dashboard')} variant="outline" className="shadow-lg">
          {tc('backToDashboard')}
        </Button>
      </div>
    </AppShell>
  );
}
