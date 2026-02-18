"use client";

import React, { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import {
  Loader2,
  RefreshCw,
  Copy,
  Check,
  ClipboardList,
  ChevronRight,
  X,
  Activity,
  Clock,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Signal,
  Play,
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
import type {
  Submission,
  SubmissionStatus,
  WorkflowStatus,
  WorkflowEvent,
} from "@/types/submission";

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
    label: "Bound",
    className: "bg-green-100 text-green-700 border-green-200",
  },
  ISSUED: {
    label: "Issued",
    className: "bg-emerald-100 text-emerald-700 border-emerald-200",
  },
  DECLINED: {
    label: "Declined",
    className: "bg-red-100 text-red-700 border-red-200",
  },
};

const WORKFLOW_STATUS_BADGE: Record<string, { label: string; className: string }> = {
  RUNNING: { label: "Running", className: "bg-blue-100 text-blue-700 border-blue-200" },
  COMPLETED: { label: "Completed", className: "bg-green-100 text-green-700 border-green-200" },
  FAILED: { label: "Failed", className: "bg-red-100 text-red-700 border-red-200" },
  TIMED_OUT: { label: "Timed Out", className: "bg-orange-100 text-orange-700 border-orange-200" },
  CANCELED: { label: "Canceled", className: "bg-gray-100 text-gray-700 border-gray-200" },
};

function getEventIcon(eventType: string) {
  if (eventType.includes("STARTED") || eventType.includes("SCHEDULED"))
    return <Play className="h-3.5 w-3.5 text-blue-500" aria-hidden="true" />;
  if (eventType.includes("COMPLETED"))
    return <CheckCircle2 className="h-3.5 w-3.5 text-green-500" aria-hidden="true" />;
  if (eventType.includes("FAILED"))
    return <XCircle className="h-3.5 w-3.5 text-red-500" aria-hidden="true" />;
  if (eventType.includes("SIGNALED"))
    return <Signal className="h-3.5 w-3.5 text-purple-500" aria-hidden="true" />;
  if (eventType.includes("TIMED_OUT"))
    return <AlertCircle className="h-3.5 w-3.5 text-orange-500" aria-hidden="true" />;
  return <Activity className="h-3.5 w-3.5 text-gray-400" aria-hidden="true" />;
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <button
      onClick={(e) => {
        e.stopPropagation();
        handleCopy();
      }}
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

function WorkflowDetailPanel({
  submissionId,
  onClose,
}: {
  submissionId: string;
  onClose: () => void;
}) {
  const [workflow, setWorkflow] = useState<WorkflowStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchWorkflow() {
      setLoading(true);
      setError(null);
      try {
        const response = await fetch(
          `/api/kernel/submissions/${submissionId}/workflow`,
          { headers: { "X-Tenant-ID": "default-tenant" } }
        );
        if (response.status === 404) {
          setError("No workflow found for this submission");
          return;
        }
        if (!response.ok) {
          throw new Error(`Failed to fetch workflow (${response.status})`);
        }
        const data = await response.json();
        setWorkflow(data);
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Failed to load workflow"
        );
      } finally {
        setLoading(false);
      }
    }
    fetchWorkflow();
  }, [submissionId]);

  const formatTime = (iso: string) => {
    try {
      return new Date(iso).toLocaleString("en-US", {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      });
    } catch {
      return iso;
    }
  };

  return (
    <div className="border-t bg-muted/20">
      <div className="px-6 py-4">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Activity className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
            <h3 className="text-sm font-semibold">Workflow Timeline</h3>
          </div>
          <Button
            variant="ghost"
            size="icon"
            className="h-6 w-6"
            onClick={onClose}
            aria-label="Close workflow details"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        {loading ? (
          <div className="flex items-center gap-2 py-4">
            <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
            <span className="text-sm text-muted-foreground">
              Loading workflow...
            </span>
          </div>
        ) : error ? (
          <p className="text-sm text-muted-foreground py-2">{error}</p>
        ) : workflow ? (
          <div className="space-y-4">
            {/* Workflow Summary */}
            <div className="flex flex-wrap items-center gap-3 text-sm">
              <Badge
                className={
                  (WORKFLOW_STATUS_BADGE[workflow.status] ?? WORKFLOW_STATUS_BADGE.RUNNING)
                    .className
                }
                variant="outline"
              >
                {(WORKFLOW_STATUS_BADGE[workflow.status] ?? { label: workflow.status }).label}
              </Badge>
              <span className="text-muted-foreground flex items-center gap-1">
                <Clock className="h-3 w-3" aria-hidden="true" />
                Started: {formatTime(workflow.startTime)}
              </span>
              {workflow.closeTime && (
                <span className="text-muted-foreground flex items-center gap-1">
                  <CheckCircle2 className="h-3 w-3" aria-hidden="true" />
                  Ended: {formatTime(workflow.closeTime)}
                </span>
              )}
              <span className="text-muted-foreground text-xs">
                Queue: {workflow.taskQueue}
              </span>
            </div>

            {/* Event Timeline */}
            {workflow.events.length > 0 && (
              <div className="relative ml-2">
                <div className="absolute left-[7px] top-2 bottom-2 w-px bg-border" />
                <div className="space-y-3">
                  {workflow.events.map((event) => (
                    <div
                      key={event.eventId}
                      className="flex items-start gap-3 relative"
                    >
                      <div className="relative z-10 mt-0.5 rounded-full bg-background p-0.5">
                        {getEventIcon(event.eventType)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-medium">
                            {event.detail}
                          </span>
                          <span className="text-xs text-muted-foreground">
                            {formatTime(event.timestamp)}
                          </span>
                        </div>
                        <span className="text-xs text-muted-foreground/70">
                          {event.eventType.replace(/_/g, " ").toLowerCase()}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Run ID */}
            <div className="text-xs text-muted-foreground pt-1 border-t">
              Run ID: <code className="font-mono">{workflow.runId}</code>
            </div>
          </div>
        ) : null}
      </div>
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
  const [expandedId, setExpandedId] = useState<string | null>(null);

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

  const toggleExpand = (submissionId: string) => {
    setExpandedId((prev) => (prev === submissionId ? null : submissionId));
  };

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
                <span className="text-muted-foreground">
                  Issued:{" "}
                  <span className="font-medium text-emerald-700">
                    {submissions.filter((s) => s.status === "ISSUED").length}
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
                  <TableHead className="w-8 pl-4" />
                  <TableHead>Submission ID</TableHead>
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
                  const isExpanded = expandedId === submission.submissionId;

                  return (
                    <React.Fragment key={submission.submissionId}>
                      <TableRow
                        className="cursor-pointer hover:bg-muted/50"
                        onClick={() => toggleExpand(submission.submissionId)}
                      >
                        <TableCell className="pl-4 w-8">
                          <ChevronRight
                            className={`h-4 w-4 text-muted-foreground transition-transform ${
                              isExpanded ? "rotate-90" : ""
                            }`}
                            aria-hidden="true"
                          />
                        </TableCell>
                        <TableCell>
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
                              onClick={(e) => {
                                e.stopPropagation();
                                handleBind(submission.submissionId);
                              }}
                              disabled={isBinding}
                            >
                              {isBinding && (
                                <Loader2 className="mr-1 h-3 w-3 animate-spin" />
                              )}
                              {isBinding ? "Accepting..." : "Accept Quote"}
                            </Button>
                          )}
                          {submission.status === "BOUND" && (
                            <Badge
                              className="bg-green-100 text-green-700 border-green-200"
                              variant="outline"
                            >
                              Bound
                            </Badge>
                          )}
                          {submission.status === "ISSUED" && (
                            <Badge
                              className="bg-emerald-100 text-emerald-700 border-emerald-200"
                              variant="outline"
                            >
                              Issued
                            </Badge>
                          )}
                        </TableCell>
                      </TableRow>
                      {isExpanded && (
                        <TableRow>
                          <TableCell colSpan={7} className="p-0">
                            <WorkflowDetailPanel
                              submissionId={submission.submissionId}
                              onClose={() => setExpandedId(null)}
                            />
                          </TableCell>
                        </TableRow>
                      )}
                    </React.Fragment>
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
