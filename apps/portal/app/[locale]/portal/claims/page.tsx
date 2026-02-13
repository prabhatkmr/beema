"use client";

import { useState, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import {
  AlertCircle,
  ShieldAlert,
  Car,
  Footprints,
  Wind,
  PackageX,
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
import { ClaimDetail } from "@/components/claims/ClaimDetail";
import { LayoutRenderer } from "@/components/dynamic/LayoutRenderer";
import { ScrollArea } from "@/components/ui/scroll-area";
import type { Layout } from "@/types/layout";
import { cn } from "@/lib/utils";
import { saveDraftClaim, removeDraftClaim, type DraftClaim } from "@/components/DraftClaims";

interface Claim {
  id: string;
  title: string;
  subtitle: string;
  status: string;
  statusColor: "default" | "secondary" | "destructive" | "outline";
  metadata: string;
  icon: LucideIcon;
  lossDate: string;
  reserve: string;
  paid: string;
}

const mockClaims: Claim[] = [
  {
    id: "CLM-101",
    title: "Water Damage - John Doe",
    subtitle: "Residential Property",
    status: "Open",
    statusColor: "default",
    metadata: "Loss Date: Jan 15, 2026",
    icon: AlertCircle,
    lossDate: "Jan 15, 2026",
    reserve: "$50,000",
    paid: "$12,000",
  },
  {
    id: "CLM-102",
    title: "Fender Bender - Jane Smith",
    subtitle: "Auto Liability",
    status: "Under Review",
    statusColor: "outline",
    metadata: "Loss Date: Feb 02, 2026",
    icon: Car,
    lossDate: "Feb 02, 2026",
    reserve: "$8,500",
    paid: "$0",
  },
  {
    id: "CLM-103",
    title: "Slip & Fall - Metro Mall",
    subtitle: "General Liability",
    status: "Closed",
    statusColor: "secondary",
    metadata: "Loss Date: Dec 20, 2025",
    icon: Footprints,
    lossDate: "Dec 20, 2025",
    reserve: "$25,000",
    paid: "$18,750",
  },
  {
    id: "CLM-104",
    title: "Windstorm - Acme Corp",
    subtitle: "Commercial Property",
    status: "Open",
    statusColor: "default",
    metadata: "Loss Date: Mar 01, 2026",
    icon: Wind,
    lossDate: "Mar 01, 2026",
    reserve: "$120,000",
    paid: "$35,000",
  },
  {
    id: "CLM-105",
    title: "Theft - Henderson Office",
    subtitle: "Personal Property",
    status: "Pending",
    statusColor: "secondary",
    metadata: "Loss Date: Feb 25, 2026",
    icon: PackageX,
    lossDate: "Feb 25, 2026",
    reserve: "$15,000",
    paid: "$0",
  },
];

const newClaimLayout: Layout = {
  regions: [
    {
      id: "claim-info",
      label: "Claim Information",
      columns: 2,
      fields: [
        { id: "claimType", label: "Claim Type", type: "SELECT", required: true, options: ["Residential Property", "Auto Liability", "General Liability", "Commercial Property", "Personal Property"], placeholder: "Select claim type..." },
        { id: "policyNumber", label: "Policy Number", type: "TEXT", required: true, placeholder: "POL-XXXXX" },
        { id: "lossDate", label: "Date of Loss", type: "TEXT", required: true, placeholder: "DD/MM/YYYY" },
        { id: "reportedDate", label: "Date Reported", type: "TEXT", required: true, placeholder: "DD/MM/YYYY" },
      ],
    },
    {
      id: "insured-details",
      label: "Insured Details",
      columns: 2,
      fields: [
        { id: "insuredName", label: "Insured Name", type: "TEXT", required: true, placeholder: "Enter insured name" },
        { id: "insuredPhone", label: "Phone Number", type: "TEXT", required: true, placeholder: "+1 (555) 000-0000" },
        { id: "insuredEmail", label: "Email", type: "TEXT", required: false, placeholder: "email@example.com" },
        { id: "insuredAddress", label: "Address", type: "TEXT", required: false, placeholder: "Enter address" },
      ],
    },
    {
      id: "loss-details",
      label: "Loss Details",
      columns: 1,
      fields: [
        { id: "lossLocation", label: "Location of Loss", type: "TEXT", required: true, placeholder: "Enter location where the loss occurred" },
        { id: "lossDescription", label: "Description of Loss", type: "TEXTAREA", required: true, placeholder: "Provide a detailed description of what happened..." },
        { id: "estimatedLoss", label: "Estimated Loss Amount", type: "CURRENCY", required: false, placeholder: "0.00" },
        { id: "policeReport", label: "Police Report Filed", type: "TOGGLE", required: false, defaultValue: false },
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

export default function ClaimsCenterPage() {
  const router = useRouter();
  const [selectedClaim, setSelectedClaim] = useState<Claim | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<string[]>([]);
  const [claimTypeFilter, setClaimTypeFilter] = useState<string[]>([]);
  const [sortBy, setSortBy] = useState<string>("newest");
  const [isCreatingClaim, setIsCreatingClaim] = useState(false);
  const [currentClaimId, setCurrentClaimId] = useState<string | null>(null);
  const [claimFormData, setClaimFormData] = useState<Record<string, any>>({});
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

  // Check for resumed claim on mount
  useEffect(() => {
    const resumeClaimData = sessionStorage.getItem('resumeClaim');
    if (resumeClaimData) {
      try {
        const claim: DraftClaim = JSON.parse(resumeClaimData);
        setCurrentClaimId(claim.id);
        setClaimFormData(claim.data);
        setIsCreatingClaim(true);
        sessionStorage.removeItem('resumeClaim');
      } catch (error) {
        console.error('Failed to resume claim:', error);
      }
    }
  }, []);

  // Auto-save draft claim as user works on it
  useEffect(() => {
    if (isCreatingClaim && currentClaimId && Object.keys(claimFormData).length > 0) {
      const saveTimer = setTimeout(() => {
        const title = claimFormData.insuredName
          ? `Claim for ${claimFormData.insuredName}`
          : claimFormData.claimType
          ? `${claimFormData.claimType} Claim`
          : 'Untitled Claim';

        saveDraftClaim({
          id: currentClaimId,
          title,
          claimType: claimFormData.claimType,
          insuredName: claimFormData.insuredName,
          data: claimFormData,
        });
      }, 1000); // Debounce saves by 1 second

      return () => clearTimeout(saveTimer);
    }
  }, [isCreatingClaim, currentClaimId, claimFormData]);

  // Get unique values for filters
  const uniqueStatuses = Array.from(new Set(mockClaims.map(c => c.status)));
  const uniqueClaimTypes = Array.from(new Set(mockClaims.map(c => c.subtitle)));

  // Apply filters and sorting
  let filteredClaims = mockClaims.filter((c) => {
    const matchesSearch =
      c.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.subtitle.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.id.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesStatus = statusFilter.length === 0 || statusFilter.includes(c.status);
    const matchesType = claimTypeFilter.length === 0 || claimTypeFilter.includes(c.subtitle);

    return matchesSearch && matchesStatus && matchesType;
  });

  // Sort claims
  filteredClaims = [...filteredClaims].sort((a, b) => {
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

  const handleNewClaim = () => {
    setSelectedClaim(null);
    setIsCreatingClaim(true);
    setCurrentClaimId(`claim-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`);
    setClaimFormData({});
  };

  const handleCancelClaim = () => {
    // Optionally keep the draft claim in progress
    setIsCreatingClaim(false);
    setCurrentClaimId(null);
    setClaimFormData({});
  };

  const handleSubmitClaim = () => {
    console.log("Submitting claim:", claimFormData);

    // Remove from draft claims after successful submission
    if (currentClaimId) {
      removeDraftClaim(currentClaimId);
    }

    // TODO: Implement actual claim submission
    setIsCreatingClaim(false);
    setCurrentClaimId(null);
    setClaimFormData({});
  };

  const handleClaimFormChange = (fieldId: string, value: any) => {
    setClaimFormData((prev) => ({ ...prev, [fieldId]: value }));
  };

  return (
    <AppShell
      title="Dashboard"
      searchPlaceholder="Search claims..."
      onBack={() => router.push('/portal/dashboard')}
      onSearchChange={setSearchQuery}
      actionSlot={
        <Button
          onClick={handleNewClaim}
          className="shrink-0 rounded-full gap-1.5"
        >
          <Plus className="h-4 w-4" />
          + FNOL
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
              label="Claim Types"
              options={uniqueClaimTypes}
              selected={claimTypeFilter}
              onChange={setClaimTypeFilter}
              ariaLabel="Filter by claim type"
            />

            {(statusFilter.length > 0 || claimTypeFilter.length > 0) && (
              <button
                onClick={() => {
                  setStatusFilter([]);
                  setClaimTypeFilter([]);
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
                aria-label="Sort claims"
              >
                <option value="newest">Newest First</option>
                <option value="oldest">Oldest First</option>
                <option value="name-asc">Name (A-Z)</option>
                <option value="name-desc">Name (Z-A)</option>
                <option value="status">Status</option>
              </select>
            </div>

            <span className="text-sm text-muted-foreground">
              {filteredClaims.length} {filteredClaims.length === 1 ? 'claim' : 'claims'}
            </span>
          </div>
        </div>

        {/* Content Grid */}
        <div className={cn(
          "flex-1 overflow-hidden",
          isCreatingClaim ? "flex" : "grid grid-cols-12"
        )}>
          {/* Left Sidebar - Claims List (hidden when creating claim) */}
          {!isCreatingClaim && !isSidebarCollapsed && (
            <aside className="col-span-4 overflow-y-auto border-r relative">
            {filteredClaims.length === 0 ? (
              <div className="flex items-center justify-center py-12 text-sm text-muted-foreground">
                No claims found
              </div>
            ) : (
              filteredClaims.map((claim) => (
                <FeedCard
                  key={claim.id}
                  icon={claim.icon}
                  title={claim.title}
                  subtitle={claim.subtitle}
                  metadata={`${claim.id} · ${claim.metadata}`}
                  status={claim.status}
                  statusColor={claim.statusColor}
                  active={selectedClaim?.id === claim.id}
                  onClick={() => setSelectedClaim(claim)}
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

          {/* Right Detail Panel / Full Width Claim Panel */}
          <section className={cn(
            "overflow-hidden flex flex-col",
            isCreatingClaim ? "flex-1" : isSidebarCollapsed ? "col-span-12" : "col-span-8"
          )}>
            {!isCreatingClaim && isSidebarCollapsed && (
              <button
                onClick={() => setIsSidebarCollapsed(false)}
                className="fixed top-1/2 -translate-y-1/2 left-0 px-1.5 py-5 bg-blue-50 hover:bg-blue-100 border-2 border-blue-200 border-l-0 rounded-r-lg transition-all shadow-lg hover:shadow-xl z-20"
                aria-label="Expand sidebar"
                title="Expand sidebar"
              >
                <ChevronRight className="h-5 w-5 text-blue-600" />
              </button>
            )}
            {isCreatingClaim ? (
              <>
                <div className="border-b px-6 py-4 flex items-center justify-between">
                  <div>
                    <h2 className="text-lg font-bold">New Claim (FNOL)</h2>
                    <p className="text-sm text-muted-foreground">
                      First Notice of Loss - Create a new claim
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={handleCancelClaim}
                    >
                      <X className="h-4 w-4 mr-1" />
                      Cancel
                    </Button>
                  </div>
                </div>
                <div className="flex-1 overflow-hidden">
                  <ScrollArea className="h-full px-6">
                    <div className="py-4">
                      <LayoutRenderer
                        layout={newClaimLayout}
                        data={claimFormData}
                        onChange={handleClaimFormChange}
                      />
                    </div>
                  </ScrollArea>
                </div>
                <div className="border-t px-6 py-4">
                  <Button onClick={handleSubmitClaim} className="w-full">
                    Submit Claim
                  </Button>
                </div>
              </>
            ) : selectedClaim ? (
              <ClaimDetail claim={selectedClaim} />
            ) : (
              <div className="flex h-full items-center justify-center bg-muted/30">
                <div className="text-center">
                  <ShieldAlert className="mx-auto h-12 w-12 text-muted-foreground/50" />
                  <p className="mt-3 text-sm font-medium text-muted-foreground">
                    Select a claim to view details
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
