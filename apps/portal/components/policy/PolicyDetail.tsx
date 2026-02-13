"use client";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { LayoutRenderer } from "@/components/dynamic/LayoutRenderer";
import { FileText, Clock, FolderOpen } from "lucide-react";
import type { Layout } from "@/types/layout";

interface PolicyDetailProps {
  policy: {
    id: string;
    title: string;
    subtitle: string;
    status: string;
    statusColor: "default" | "secondary" | "destructive" | "outline";
    premium?: string;
    client?: string;
    metadata: string;
  };
  layout?: Layout;
  data?: Record<string, any>;
}

const statusVariantMap: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  Active: "default",
  Bound: "default",
  Quote: "secondary",
  Pending: "outline",
  "In Review": "outline",
  Lapsed: "destructive",
};

const mockTimelineEvents = [
  { date: "Jan 1, 2026", event: "Policy Bound", description: "Policy bound and effective" },
  { date: "Feb 5, 2026", event: "Premium Collected", description: "Initial premium payment received" },
  { date: "Mar 12, 2026", event: "Endorsement", description: "Coverage limit adjustment requested" },
  { date: "Mar 15, 2026", event: "Endorsement Approved", description: "Endorsement processed and applied" },
];

const mockLayout: Layout = {
  regions: [
    {
      id: "policy-info",
      label: "Policy Information",
      columns: 2,
      fields: [
        { id: "policyNumber", label: "Policy Number", type: "TEXT", required: true },
        { id: "product", label: "Product", type: "TEXT", required: true },
        { id: "effectiveDate", label: "Effective Date", type: "TEXT", required: true },
        { id: "expiryDate", label: "Expiry Date", type: "TEXT", required: true },
      ],
    },
    {
      id: "insured",
      label: "Insured Details",
      columns: 2,
      fields: [
        { id: "insuredName", label: "Insured Name", type: "TEXT", required: true },
        { id: "insuredAddress", label: "Address", type: "TEXT", required: false },
        { id: "lineOfBusiness", label: "Line of Business", type: "TEXT", required: true },
        { id: "territory", label: "Territory", type: "TEXT", required: false },
      ],
    },
  ],
};

function getMockData(policy: PolicyDetailProps["policy"]): Record<string, any> {
  return {
    policyNumber: policy.id,
    product: policy.subtitle,
    effectiveDate: "01/01/2026",
    expiryDate: "01/01/2027",
    insuredName: policy.title.replace(/^(Renewal|Quote|Bound|Endorsement|New Business|MTA)\s*-\s*/, ""),
    insuredAddress: "123 Business Park, London, UK",
    lineOfBusiness: policy.subtitle,
    territory: "United Kingdom",
  };
}

export function PolicyDetail({ policy, layout, data }: PolicyDetailProps) {
  const resolvedLayout = layout ?? mockLayout;
  const resolvedData = data ?? getMockData(policy);
  const badgeVariant = statusVariantMap[policy.status] ?? policy.statusColor;

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="flex items-center justify-between border-b px-6 py-4">
        <div>
          <h2 className="text-xl font-bold">{policy.id}</h2>
          <p className="text-sm text-muted-foreground">
            {policy.title.replace(/^(Renewal|Quote|Bound|Endorsement|New Business|MTA)\s*-\s*/, "")} &middot; {policy.subtitle}
          </p>
        </div>
        <div className="flex items-center gap-4">
          <Badge variant={badgeVariant}>{policy.status}</Badge>
          {policy.premium && (
            <span className="text-xl font-semibold">{policy.premium}</span>
          )}
        </div>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="overview" className="flex flex-1 flex-col overflow-hidden">
        <div className="border-b px-6">
          <TabsList className="h-10">
            <TabsTrigger value="overview" className="gap-1.5">
              <FileText className="h-4 w-4" />
              Overview
            </TabsTrigger>
            <TabsTrigger value="timeline" className="gap-1.5">
              <Clock className="h-4 w-4" />
              Timeline
            </TabsTrigger>
            <TabsTrigger value="documents" className="gap-1.5">
              <FolderOpen className="h-4 w-4" />
              Documents
            </TabsTrigger>
          </TabsList>
        </div>

        <ScrollArea className="flex-1">
          <TabsContent value="overview" className="m-0 p-6">
            <LayoutRenderer
              layout={resolvedLayout}
              data={resolvedData}
              onChange={() => {}}
              readOnly
            />
          </TabsContent>

          <TabsContent value="timeline" className="m-0 p-6">
            <div className="space-y-0">
              {mockTimelineEvents.map((event, index) => (
                <div key={index} className="relative flex gap-4 pb-8 last:pb-0">
                  {/* Vertical line */}
                  {index < mockTimelineEvents.length - 1 && (
                    <div className="absolute left-[7px] top-4 h-full w-px bg-border" />
                  )}
                  {/* Dot */}
                  <div className="relative z-10 mt-1.5 h-3.5 w-3.5 shrink-0 rounded-full border-2 border-primary bg-background" />
                  {/* Content */}
                  <div className="flex-1">
                    <p className="text-sm font-medium">{event.event}</p>
                    <p className="text-xs text-muted-foreground">{event.date}</p>
                    <p className="mt-1 text-sm text-muted-foreground">{event.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </TabsContent>

          <TabsContent value="documents" className="m-0 p-6">
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <FolderOpen className="h-12 w-12 text-muted-foreground/50" />
              <p className="mt-3 text-sm font-medium text-muted-foreground">
                No documents
              </p>
              <p className="text-xs text-muted-foreground">
                Documents will appear here once uploaded.
              </p>
            </div>
          </TabsContent>
        </ScrollArea>
      </Tabs>

      {/* Footer - sticky action bar */}
      <div className="flex items-center justify-end gap-3 border-t bg-background px-6 py-3">
        <Button variant="outline">Endorse</Button>
        <Button variant="destructive">Cancel Policy</Button>
      </div>
    </div>
  );
}
