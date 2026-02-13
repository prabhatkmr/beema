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
  X,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { FeedCard } from "@/components/layout/FeedCard";
import { cn } from "@/lib/utils";

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
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<string[]>([]);
  const [lineOfBusinessFilter, setLineOfBusinessFilter] = useState<string[]>([]);
  const [sortBy, setSortBy] = useState<string>("newest");

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

  const selectedPolicy = mockPolicies.find((p) => p.id === selectedId);

  return (
    <AppShell
      title="Policy Center"
      actionLabel="+ New Quote"
      searchPlaceholder="Search policies..."
      onAction={() => {
        /* placeholder for new quote action */
      }}
      onBack={() => router.back()}
      onSearchChange={setSearchQuery}
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
        <div className="grid flex-1 grid-cols-12 overflow-hidden">
          {/* Left Sidebar - Policy List */}
          <aside className="col-span-4 overflow-y-auto border-r">
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
                active={selectedId === policy.id}
                onClick={() => setSelectedId(policy.id)}
              />
            ))
          )}
        </aside>

        {/* Right Detail Panel */}
        <section className="col-span-8 overflow-y-auto">
          {selectedPolicy ? (
            <div className="p-6 space-y-6">
              <div className="flex items-center gap-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-muted">
                  <selectedPolicy.icon className="h-6 w-6 text-muted-foreground" />
                </div>
                <div>
                  <h2 className="text-lg font-bold">{selectedPolicy.title}</h2>
                  <p className="text-sm text-muted-foreground">
                    {selectedPolicy.id} · {selectedPolicy.subtitle}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="rounded-lg border p-4">
                  <p className="text-xs font-medium text-muted-foreground">
                    Status
                  </p>
                  <p className="mt-1 text-sm font-semibold">
                    {selectedPolicy.status}
                  </p>
                </div>
                <div className="rounded-lg border p-4">
                  <p className="text-xs font-medium text-muted-foreground">
                    Line of Business
                  </p>
                  <p className="mt-1 text-sm font-semibold">
                    {selectedPolicy.subtitle}
                  </p>
                </div>
                <div className="rounded-lg border p-4">
                  <p className="text-xs font-medium text-muted-foreground">
                    Reference
                  </p>
                  <p className="mt-1 text-sm font-semibold">
                    {selectedPolicy.id}
                  </p>
                </div>
                <div className="rounded-lg border p-4">
                  <p className="text-xs font-medium text-muted-foreground">
                    Key Date
                  </p>
                  <p className="mt-1 text-sm font-semibold">
                    {selectedPolicy.metadata}
                  </p>
                </div>
              </div>
            </div>
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
    </AppShell>
  );
}
