"use client";

import { useState, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import {
  ScrollText,
  FileText,
  RefreshCw,
  ShieldCheck,
  Briefcase,
  Building2,
  Landmark,
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
import { PolicyDetail } from "@/components/policy/PolicyDetail";
import { LayoutRenderer } from "@/components/dynamic/LayoutRenderer";
import { ScrollArea } from "@/components/ui/scroll-area";
import type { Layout } from "@/types/layout";
import { cn } from "@/lib/utils";
import { saveQuoteInProgress, removeQuoteInProgress, type QuoteInProgress } from "@/components/QuotesInProgress";

interface Policy {
  id: string;
  title: string;
  subtitle: string;
  status: string;
  statusColor: "default" | "secondary" | "destructive" | "outline";
  metadata: string;
  icon: LucideIcon;
}

const mockPolicies: Policy[] = [
  {
    id: "POL-001",
    title: "Renewal - John Doe",
    subtitle: "Commercial Property",
    status: "Active",
    statusColor: "default",
    metadata: "Expires 15 Mar 2026",
    icon: RefreshCw,
  },
  {
    id: "POL-002",
    title: "Quote - Jane Smith",
    subtitle: "Professional Indemnity",
    status: "Quote",
    statusColor: "secondary",
    metadata: "Created 10 Feb 2026",
    icon: FileText,
  },
  {
    id: "POL-003",
    title: "Bound - Acme Corp",
    subtitle: "Directors & Officers",
    status: "Bound",
    statusColor: "default",
    metadata: "Effective 01 Jan 2026",
    icon: ShieldCheck,
  },
  {
    id: "POL-004",
    title: "Renewal - Tech Solutions Ltd",
    subtitle: "Cyber Liability",
    status: "Pending",
    statusColor: "outline",
    metadata: "Due 28 Feb 2026",
    icon: RefreshCw,
  },
  {
    id: "POL-005",
    title: "Quote - Global Logistics",
    subtitle: "Marine Cargo",
    status: "Quote",
    statusColor: "secondary",
    metadata: "Created 05 Feb 2026",
    icon: FileText,
  },
  {
    id: "POL-006",
    title: "Endorsement - Sarah Williams",
    subtitle: "Home & Contents",
    status: "In Review",
    statusColor: "outline",
    metadata: "Submitted 12 Feb 2026",
    icon: ScrollText,
  },
  {
    id: "POL-007",
    title: "New Business - Metro Builders",
    subtitle: "Construction All Risk",
    status: "Active",
    statusColor: "default",
    metadata: "Bound 20 Jan 2026",
    icon: Building2,
  },
  {
    id: "POL-008",
    title: "Renewal - First Capital Bank",
    subtitle: "Financial Institutions",
    status: "Lapsed",
    statusColor: "destructive",
    metadata: "Expired 31 Dec 2025",
    icon: Landmark,
  },
  {
    id: "POL-009",
    title: "Quote - Henderson & Partners",
    subtitle: "Employers Liability",
    status: "Quote",
    statusColor: "secondary",
    metadata: "Created 08 Feb 2026",
    icon: Briefcase,
  },
  {
    id: "POL-010",
    title: "MTA - Riverside Hotels",
    subtitle: "Commercial Combined",
    status: "Pending",
    statusColor: "outline",
    metadata: "Requested 11 Feb 2026",
    icon: ScrollText,
  },
];

const newQuoteLayout: Layout = {
  regions: [
    {
      id: "quote-info",
      label: "Quote Information",
      columns: 2,
      fields: [
        { id: "product", label: "Product", type: "SELECT", required: true, options: ["Commercial Property", "Professional Indemnity", "Cyber Liability", "Directors & Officers", "Marine Cargo", "Employers Liability"], placeholder: "Select product..." },
        { id: "lineOfBusiness", label: "Line of Business", type: "SELECT", required: true, options: ["Retail", "Commercial", "London Market"], placeholder: "Select line..." },
        { id: "effectiveDate", label: "Effective Date", type: "TEXT", required: true, placeholder: "DD/MM/YYYY" },
        { id: "expiryDate", label: "Expiry Date", type: "TEXT", required: true, placeholder: "DD/MM/YYYY" },
      ],
    },
    {
      id: "insured-details",
      label: "Insured Details",
      columns: 2,
      fields: [
        { id: "insuredName", label: "Insured Name", type: "TEXT", required: true, placeholder: "Enter insured name" },
        { id: "insuredEmail", label: "Email", type: "TEXT", required: false, placeholder: "email@example.com" },
        { id: "insuredAddress", label: "Address", type: "TEXT", required: false, placeholder: "Enter address" },
        { id: "territory", label: "Territory", type: "SELECT", required: true, options: ["United Kingdom", "United States", "European Union", "Asia Pacific"], placeholder: "Select territory..." },
      ],
    },
    {
      id: "coverage",
      label: "Coverage",
      columns: 2,
      fields: [
        { id: "sumInsured", label: "Sum Insured", type: "CURRENCY", required: true, placeholder: "0.00" },
        { id: "premium", label: "Estimated Premium", type: "CURRENCY", required: false, placeholder: "0.00" },
        { id: "deductible", label: "Deductible", type: "CURRENCY", required: false, placeholder: "0.00" },
        { id: "retroactiveDate", label: "Retroactive Cover", type: "TOGGLE", required: false, defaultValue: false },
      ],
    },
  ],
};

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

export default function PolicyCenterPage() {
  const router = useRouter();
  const [selectedPolicy, setSelectedPolicy] = useState<Policy | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<string[]>([]);
  const [lineOfBusinessFilter, setLineOfBusinessFilter] = useState<string[]>([]);
  const [sortBy, setSortBy] = useState<string>("newest");
  const [isCreatingQuote, setIsCreatingQuote] = useState(false);
  const [quoteCount, setQuoteCount] = useState<1 | 2>(1);
  const [currentQuoteId, setCurrentQuoteId] = useState<string | null>(null);
  const [quote1FormData, setQuote1FormData] = useState<Record<string, any>>({});
  const [quote2FormData, setQuote2FormData] = useState<Record<string, any>>({});
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

  // Check for resumed quote on mount
  useEffect(() => {
    const resumeQuoteData = sessionStorage.getItem('resumeQuote');
    if (resumeQuoteData) {
      try {
        const quote: QuoteInProgress = JSON.parse(resumeQuoteData);
        setCurrentQuoteId(quote.id);
        setQuote1FormData(quote.data);
        setIsCreatingQuote(true);
        sessionStorage.removeItem('resumeQuote');
      } catch (error) {
        console.error('Failed to resume quote:', error);
      }
    }
  }, []);

  // Auto-save quote as user works on it
  useEffect(() => {
    if (isCreatingQuote && currentQuoteId && Object.keys(quote1FormData).length > 0) {
      const saveTimer = setTimeout(() => {
        const title = quote1FormData.insuredName
          ? `Quote for ${quote1FormData.insuredName}`
          : quote1FormData.product
          ? `${quote1FormData.product} Quote`
          : 'Untitled Quote';

        saveQuoteInProgress({
          id: currentQuoteId,
          title,
          product: quote1FormData.product,
          insuredName: quote1FormData.insuredName,
          data: quote1FormData,
        });
      }, 1000); // Debounce saves by 1 second

      return () => clearTimeout(saveTimer);
    }
  }, [isCreatingQuote, currentQuoteId, quote1FormData]);

  // Get unique values for filters
  const uniqueStatuses = Array.from(new Set(mockPolicies.map(p => p.status)));
  const uniqueLinesOfBusiness = Array.from(new Set(mockPolicies.map(p => p.subtitle)));

  // Apply filters and sorting
  let filteredPolicies = mockPolicies.filter((p) => {
    const matchesSearch =
      p.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.subtitle.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.id.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesStatus = statusFilter.length === 0 || statusFilter.includes(p.status);
    const matchesLineOfBusiness = lineOfBusinessFilter.length === 0 || lineOfBusinessFilter.includes(p.subtitle);

    return matchesSearch && matchesStatus && matchesLineOfBusiness;
  });

  // Sort policies
  filteredPolicies = [...filteredPolicies].sort((a, b) => {
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

  const handleNewQuote = () => {
    setSelectedPolicy(null);
    setIsCreatingQuote(true);
    setQuoteCount(1);
    setCurrentQuoteId(`quote-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`);
    setQuote1FormData({});
    setQuote2FormData({});
  };

  const handleCancelQuote = () => {
    // Optionally keep the quote in progress
    setIsCreatingQuote(false);
    setCurrentQuoteId(null);
    setQuote1FormData({});
    setQuote2FormData({});
  };

  const handleSubmitQuote = () => {
    if (quoteCount === 1) {
      console.log("Submitting quote 1:", quote1FormData);
    } else {
      console.log("Submitting quote 1:", quote1FormData);
      console.log("Submitting quote 2:", quote2FormData);
    }

    // Remove from quotes in progress after successful submission
    if (currentQuoteId) {
      removeQuoteInProgress(currentQuoteId);
    }

    // TODO: Implement actual quote submission
    setIsCreatingQuote(false);
    setCurrentQuoteId(null);
    setQuote1FormData({});
    setQuote2FormData({});
  };

  const handleQuote1FormChange = (fieldId: string, value: any) => {
    setQuote1FormData((prev) => ({ ...prev, [fieldId]: value }));
  };

  const handleQuote2FormChange = (fieldId: string, value: any) => {
    setQuote2FormData((prev) => ({ ...prev, [fieldId]: value }));
  };

  return (
    <AppShell
      title="Dashboard"
      searchPlaceholder="Search policies..."
      onBack={() => router.push('/portal/dashboard')}
      onSearchChange={setSearchQuery}
      actionSlot={
        <Button
          onClick={handleNewQuote}
          className="shrink-0 rounded-full gap-1.5"
        >
          <Plus className="h-4 w-4" />
          + New Quote
        </Button>
      }
    >
      <div className="flex flex-col h-full">
        {/* Filter Bar */}
        <div className="border-b bg-muted/30 px-4 py-3">
          <div className="flex items-center gap-3 flex-wrap">
            <span className="text-sm font-medium text-muted-foreground">Filters:</span>

            <MultiSelectDropdown
              label="Statuses"
              options={uniqueStatuses}
              selected={statusFilter}
              onChange={setStatusFilter}
              ariaLabel="Filter by status"
            />

            <MultiSelectDropdown
              label="Lines of Business"
              options={uniqueLinesOfBusiness}
              selected={lineOfBusinessFilter}
              onChange={setLineOfBusinessFilter}
              ariaLabel="Filter by line of business"
            />

            {(statusFilter.length > 0 || lineOfBusinessFilter.length > 0) && (
              <button
                onClick={() => {
                  setStatusFilter([]);
                  setLineOfBusinessFilter([]);
                }}
                className="text-sm text-blue-600 hover:text-blue-700 underline"
              >
                Clear all filters
              </button>
            )}

            <div className="ml-auto flex items-center gap-2">
              <span className="text-sm font-medium text-muted-foreground">Sort by:</span>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="px-3 py-1.5 text-sm border rounded-md bg-background focus:ring-2 focus:ring-blue-500 focus:outline-none"
                aria-label="Sort policies"
              >
                <option value="newest">Newest First</option>
                <option value="oldest">Oldest First</option>
                <option value="name-asc">Name (A-Z)</option>
                <option value="name-desc">Name (Z-A)</option>
                <option value="status">Status</option>
              </select>
            </div>

            <span className="text-sm text-muted-foreground">
              {filteredPolicies.length} {filteredPolicies.length === 1 ? 'policy' : 'policies'}
            </span>
          </div>
        </div>

        {/* Content Grid */}
        <div className={cn(
          "flex-1 overflow-hidden",
          isCreatingQuote ? "flex" : "grid grid-cols-12"
        )}>
          {/* Left Sidebar - Policy List (hidden when creating quotes) */}
          {!isCreatingQuote && !isSidebarCollapsed && (
            <aside className="col-span-4 overflow-y-auto border-r relative">
              {filteredPolicies.length === 0 ? (
                <div className="flex items-center justify-center py-12 text-sm text-muted-foreground">
                  No policies found
                </div>
              ) : (
                filteredPolicies.map((policy) => (
                  <FeedCard
                    key={policy.id}
                    icon={policy.icon}
                    title={policy.title}
                    subtitle={policy.subtitle}
                    metadata={`${policy.id} · ${policy.metadata}`}
                    status={policy.status}
                    statusColor={policy.statusColor}
                    active={selectedPolicy?.id === policy.id}
                    onClick={() => {
                      setSelectedPolicy(policy);
                    }}
                  />
                ))
              )}
              <button
                onClick={() => setIsSidebarCollapsed(true)}
                className="absolute top-1/2 -translate-y-1/2 -right-3 px-1.5 py-5 bg-blue-50 hover:bg-blue-100 border-2 border-blue-200 border-l-0 rounded-r-lg transition-all shadow-lg hover:shadow-xl z-20"
                aria-label="Collapse sidebar"
                title="Collapse sidebar"
              >
                <ChevronLeft className="h-5 w-5 text-blue-600" />
              </button>
            </aside>
          )}

        {/* Right Detail Panel / Full Width Quote Panel */}
        <section className={cn(
          "overflow-hidden flex flex-col",
          isCreatingQuote ? "flex-1" : isSidebarCollapsed ? "col-span-12" : "col-span-8"
        )}>
          {!isCreatingQuote && isSidebarCollapsed && (
            <button
              onClick={() => setIsSidebarCollapsed(false)}
              className="fixed top-1/2 -translate-y-1/2 left-0 px-1.5 py-5 bg-blue-50 hover:bg-blue-100 border-2 border-blue-200 border-l-0 rounded-r-lg transition-all shadow-lg hover:shadow-xl z-20"
              aria-label="Expand sidebar"
              title="Expand sidebar"
            >
              <ChevronRight className="h-5 w-5 text-blue-600" />
            </button>
          )}
          {isCreatingQuote ? (
            <>
              <div className="border-b px-6 py-4 flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-bold">New Quote{quoteCount === 2 ? ' Comparison' : ''}</h2>
                  <p className="text-sm text-muted-foreground">
                    {quoteCount === 2 ? 'Create and compare two quote scenarios' : 'Create a new policy quote'}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-1 border rounded-md p-1">
                    <Button
                      variant={quoteCount === 1 ? "default" : "ghost"}
                      size="sm"
                      onClick={() => setQuoteCount(1)}
                      className="h-7 px-3 text-xs"
                    >
                      Single Quote
                    </Button>
                    <Button
                      variant={quoteCount === 2 ? "default" : "ghost"}
                      size="sm"
                      onClick={() => setQuoteCount(2)}
                      className="h-7 px-3 text-xs"
                    >
                      Compare (2)
                    </Button>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleCancelQuote}
                  >
                    <X className="h-4 w-4 mr-1" />
                    Cancel
                  </Button>
                </div>
              </div>
              <div className="flex-1 overflow-hidden">
                <div className={cn(
                  "h-full",
                  quoteCount === 2 ? "grid grid-cols-2" : ""
                )}>
                  {/* Quote 1 */}
                  <div className="flex flex-col h-full border-r">
                    {quoteCount === 2 && (
                      <div className="border-b px-4 py-2 bg-muted/30">
                        <h3 className="text-sm font-semibold">Quote Option A</h3>
                      </div>
                    )}
                    <ScrollArea className="flex-1 px-6">
                      <div className="py-4">
                        <LayoutRenderer
                          layout={newQuoteLayout}
                          data={quote1FormData}
                          onChange={handleQuote1FormChange}
                        />
                      </div>
                    </ScrollArea>
                  </div>

                  {/* Quote 2 (only shown when comparing) */}
                  {quoteCount === 2 && (
                    <div className="flex flex-col h-full">
                      <div className="border-b px-4 py-2 bg-muted/30">
                        <h3 className="text-sm font-semibold">Quote Option B</h3>
                      </div>
                      <ScrollArea className="flex-1 px-6">
                        <div className="py-4">
                          <LayoutRenderer
                            layout={newQuoteLayout}
                            data={quote2FormData}
                            onChange={handleQuote2FormChange}
                          />
                        </div>
                      </ScrollArea>
                    </div>
                  )}
                </div>
              </div>
              <div className="border-t px-6 py-4">
                <Button onClick={handleSubmitQuote} className="w-full">
                  Submit {quoteCount === 2 ? 'Both Quotes' : 'Quote'}
                </Button>
              </div>
            </>
          ) : selectedPolicy ? (
            <PolicyDetail policy={selectedPolicy} />
          ) : (
            <div className="flex h-full items-center justify-center bg-muted/30">
              <div className="text-center">
                <ScrollText className="mx-auto h-12 w-12 text-muted-foreground/50" />
                <p className="mt-3 text-sm font-medium text-muted-foreground">
                  Select an item to view details
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
          Back to Dashboard
        </Button>
      </div>
    </AppShell>
  );
}
