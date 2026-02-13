"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import {
  Loader2,
  RefreshCw,
  Copy,
  Check,
  ClipboardList,
} from "lucide-react";
import { toast } from "sonner";
import { AppShell } from "@/components/layout/AppShell";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { Submission, SubmissionStatus } from "@/types/submission";

const STATUS_BADGE: Record<
  SubmissionStatus,
  { label: string; className: string }
> = {
  DRAFT: {
    label: "Draft",
    className: "bg-gray-100 text-gray-700 border-gray-200",
  },
  QUOTED: {
    label: "Quoted",
    className: "bg-blue-100 text-blue-700 border-blue-200",
  },
  BOUND: {
    label: "Accepted",
    className: "bg-green-100 text-green-700 border-green-200",
  },
  DECLINED: {
    label: "Declined",
    className: "bg-red-100 text-red-700 border-red-200",
  },
};

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <button
      onClick={handleCopy}
      className="inline-flex items-center gap-1 text-muted-foreground hover:text-foreground transition-colors"
      aria-label={`Copy ${text}`}
    >
      <code className="text-xs font-mono">
        {text.substring(0, 8)}...
      </code>
      {copied ? (
        <Check className="h-3 w-3 text-green-600" />
      ) : (
        <Copy className="h-3 w-3" />
      )}
    </button>
  );
}

function LoadingSkeleton() {
  return (
    <div className="p-6 space-y-4">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="flex items-center gap-4">
          <div className="h-4 w-24 bg-muted animate-pulse rounded" />
          <div className="h-4 w-20 bg-muted animate-pulse rounded" />
          <div className="h-4 w-16 bg-muted animate-pulse rounded" />
          <div className="h-4 w-20 bg-muted animate-pulse rounded" />
          <div className="h-4 w-32 bg-muted animate-pulse rounded" />
          <div className="h-4 w-24 bg-muted animate-pulse rounded" />
        </div>
      ))}
    </div>
  );
}

export default function SubmissionsPage() {
  const router = useRouter();
  const tc = useTranslations("common");

  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [bindingId, setBindingId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

  const fetchSubmissions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch("/api/kernel/submissions", {
        headers: {
          "X-Tenant-ID": "default-tenant",
        },
      });
      if (!response.ok) {
        throw new Error(`Failed to fetch submissions (${response.status})`);
      }
      const data = await response.json();
      // Handle both array and paginated responses
      const list = Array.isArray(data) ? data : data.content ?? [];
      setSubmissions(list);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "An unexpected error occurred"
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSubmissions();
  }, [fetchSubmissions]);

  const handleBind = async (submissionId: string) => {
    setBindingId(submissionId);
    try {
      const response = await fetch(
        `/api/kernel/submissions/${submissionId}/bind`,
        {
          method: "POST",
          headers: {
            "X-Tenant-ID": "default-tenant",
          },
        }
      );

      if (!response.ok) {
        let message = "Failed to accept quote";
        try {
          const errorBody = await response.json();
          message = errorBody.message || message;
        } catch {
          // response body wasn't JSON
        }
        throw new Error(message);
      }

      toast.success("Quote Accepted!", {
        description: `Submission ${submissionId.substring(0, 8)} has been bound.`,
      });

      // Refresh the list
      await fetchSubmissions();
    } catch (err) {
      toast.error("Failed to accept quote", {
        description:
          err instanceof Error ? err.message : "An unexpected error occurred",
      });
    } finally {
      setBindingId(null);
    }
  };

  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr).toLocaleDateString("en-US", {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateStr;
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(amount);
  };

  // Filter submissions by search query
  const filtered = submissions.filter((s) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      s.submissionId.toLowerCase().includes(q) ||
      s.product.toLowerCase().includes(q) ||
      s.status.toLowerCase().includes(q)
    );
  });

  return (
    <AppShell
      title={tc("backToDashboard")}
      searchPlaceholder="Search submissions..."
      onBack={() => router.push("/portal/dashboard")}
      onSearchChange={setSearchQuery}
      actionSlot={
        <Button
          variant="outline"
          size="sm"
          onClick={fetchSubmissions}
          disabled={loading}
          className="shrink-0 gap-1.5"
        >
          <RefreshCw
            className={`h-4 w-4 ${loading ? "animate-spin" : ""}`}
          />
          Refresh
        </Button>
      }
    >
      <div className="flex flex-col h-full">
        {/* Stats Bar */}
        <div className="border-b bg-muted/30 px-6 py-3">
          <div className="flex items-center gap-6 text-sm">
            <span className="text-muted-foreground">
              {filtered.length}{" "}
              {filtered.length === 1 ? "submission" : "submissions"}
            </span>
            {submissions.length > 0 && (
              <>
                <span className="text-muted-foreground">
                  Quoted:{" "}
                  <span className="font-medium text-blue-700">
                    {submissions.filter((s) => s.status === "QUOTED").length}
                  </span>
                </span>
                <span className="text-muted-foreground">
                  Bound:{" "}
                  <span className="font-medium text-green-700">
                    {submissions.filter((s) => s.status === "BOUND").length}
                  </span>
                </span>
              </>
            )}
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-auto">
          {loading ? (
            <LoadingSkeleton />
          ) : error ? (
            <div className="flex flex-col items-center justify-center py-20 gap-4">
              <p className="text-sm text-destructive">{error}</p>
              <Button variant="outline" size="sm" onClick={fetchSubmissions}>
                {tc("retry")}
              </Button>
            </div>
          ) : filtered.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <ClipboardList className="h-12 w-12 text-muted-foreground/50" />
              <p className="text-sm font-medium text-muted-foreground">
                {submissions.length === 0
                  ? "No submissions yet"
                  : "No matching submissions"}
              </p>
              {submissions.length === 0 && (
                <p className="text-xs text-muted-foreground">
                  Submit a quote from a product page to get started.
                </p>
              )}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-6">Submission ID</TableHead>
                  <TableHead>Product</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Premium</TableHead>
                  <TableHead>Created</TableHead>
                  <TableHead className="pr-6 text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((submission) => {
                  const badge = STATUS_BADGE[submission.status];
                  const isBinding = bindingId === submission.submissionId;

                  return (
                    <TableRow key={submission.submissionId}>
                      <TableCell className="pl-6">
                        <CopyButton text={submission.submissionId} />
                      </TableCell>
                      <TableCell>
                        <span className="font-medium capitalize">
                          {submission.product}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Badge className={badge.className} variant="outline">
                          {badge.label}
                        </Badge>
                        {submission.status === "QUOTED" &&
                          submission.ratingResult && (
                            <span className="ml-2 text-xs text-blue-600 font-medium">
                              {formatCurrency(submission.ratingResult.total)}
                            </span>
                          )}
                      </TableCell>
                      <TableCell>
                        {submission.ratingResult
                          ? formatCurrency(submission.ratingResult.premium)
                          : "-"}
                      </TableCell>
                      <TableCell className="text-muted-foreground text-sm">
                        {formatDate(submission.createdAt)}
                      </TableCell>
                      <TableCell className="pr-6 text-right">
                        {submission.status === "QUOTED" && (
                          <Button
                            size="sm"
                            onClick={() => handleBind(submission.submissionId)}
                            disabled={isBinding}
                          >
                            {isBinding && (
                              <Loader2 className="mr-1 h-3 w-3 animate-spin" />
                            )}
                            {isBinding ? "Accepting..." : "Accept Quote"}
                          </Button>
                        )}
                        {submission.status === "BOUND" && (
                          <Badge className="bg-green-100 text-green-700 border-green-200" variant="outline">
                            Accepted
                          </Badge>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </div>
      </div>

      {/* Back to Dashboard floating button */}
      <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-30">
        <Button
          onClick={() => router.push("/portal/dashboard")}
          variant="outline"
          className="shadow-lg"
        >
          {tc("backToDashboard")}
        </Button>
      </div>
    </AppShell>
  );
}
