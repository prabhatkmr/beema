"use client";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { FileText, DollarSign, MessageSquare } from "lucide-react";
import { useTranslations } from 'next-intl';

interface ClaimDetailProps {
  claim: {
    id: string;
    title: string;
    subtitle: string;
    status: string;
    statusColor: "default" | "secondary" | "destructive" | "outline";
    lossDate?: string;
    reserve?: string;
    paid?: string;
    metadata: string;
  };
}

const statusVariantMap: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  Open: "default",
  "Under Review": "outline",
  Approved: "default",
  Pending: "secondary",
  Denied: "destructive",
  Closed: "secondary",
};

const mockPayments = [
  { date: "Feb 10, 2026", description: "Initial reserve established", amount: "$50,000.00", type: "Reserve" },
  { date: "Feb 20, 2026", description: "Emergency repair payment", amount: "$5,200.00", type: "Payment" },
  { date: "Mar 01, 2026", description: "Contractor invoice - structural", amount: "$4,800.00", type: "Payment" },
  { date: "Mar 15, 2026", description: "Adjuster fee", amount: "$2,000.00", type: "Expense" },
];

export function ClaimDetail({ claim }: ClaimDetailProps) {
  const t = useTranslations('claims.detail');
  const tf = useTranslations('claims.financials');
  const badgeVariant = statusVariantMap[claim.status] ?? claim.statusColor;
  const lossDate = claim.lossDate ?? "Jan 15, 2026";
  const reserve = claim.reserve ?? "$50,000";
  const paid = claim.paid ?? "$12,000";

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="flex items-center justify-between border-b px-6 py-4">
        <div>
          <h2 className="text-xl font-bold">{claim.id}</h2>
          <p className="text-sm text-muted-foreground">
            {claim.title} &middot; Loss Date: {lossDate}
          </p>
        </div>
        <Badge variant={badgeVariant}>{claim.status}</Badge>
      </div>

      {/* Key Metrics */}
      <div className="flex items-center gap-8 border-b px-6 py-4">
        <div>
          <p className="text-xs font-medium uppercase text-muted-foreground">{t('reserve')}</p>
          <p className="text-2xl font-bold">{reserve}</p>
        </div>
        <div>
          <p className="text-xs font-medium uppercase text-muted-foreground">{t('paid')}</p>
          <p className="text-2xl font-bold">{paid}</p>
        </div>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="overview" className="flex flex-1 flex-col overflow-hidden">
        <div className="border-b px-6">
          <TabsList className="h-10">
            <TabsTrigger value="overview" className="gap-1.5">
              <FileText className="h-4 w-4" aria-hidden="true" />
              {t('overview')}
            </TabsTrigger>
            <TabsTrigger value="financials" className="gap-1.5">
              <DollarSign className="h-4 w-4" aria-hidden="true" />
              {t('financials')}
            </TabsTrigger>
            <TabsTrigger value="notes" className="gap-1.5">
              <MessageSquare className="h-4 w-4" aria-hidden="true" />
              {t('notes')}
            </TabsTrigger>
          </TabsList>
        </div>

        <ScrollArea className="flex-1">
          <TabsContent value="overview" className="m-0 p-6">
            <div className="space-y-4">
              <div className="rounded-lg border p-4">
                <h3 className="mb-2 text-sm font-semibold">{t('lossDescription')}</h3>
                <p className="text-sm text-muted-foreground leading-relaxed">
                  Water damage to commercial property caused by burst pipe on the second floor.
                  Significant damage to office equipment, flooring, and ceiling tiles in the
                  affected area. Emergency mitigation services were dispatched within 4 hours
                  of the reported loss. Structural assessment completed by certified engineer
                  on Feb 5, 2026. Restoration work is currently in progress with an estimated
                  completion date of Apr 30, 2026.
                </p>
              </div>
              <div className="rounded-lg border p-4">
                <h3 className="mb-2 text-sm font-semibold">{t('claimDetails')}</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-muted-foreground">{t('claimType')}</span>
                    <span className="ml-2 font-medium">{claim.subtitle}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">{t('policy')}</span>
                    <span className="ml-2 font-medium">POL-001</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">{t('adjuster')}</span>
                    <span className="ml-2 font-medium">James Patterson</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">{t('dateReported')}</span>
                    <span className="ml-2 font-medium">Jan 16, 2026</span>
                  </div>
                </div>
              </div>
            </div>
          </TabsContent>

          <TabsContent value="financials" className="m-0 p-6">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{tf('date')}</TableHead>
                  <TableHead>{tf('description')}</TableHead>
                  <TableHead>{tf('type')}</TableHead>
                  <TableHead className="text-right">{tf('amount')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {mockPayments.map((payment, index) => (
                  <TableRow key={index}>
                    <TableCell className="text-muted-foreground">{payment.date}</TableCell>
                    <TableCell>{payment.description}</TableCell>
                    <TableCell>
                      <Badge variant="outline" className="text-xs">
                        {payment.type}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right font-medium">{payment.amount}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TabsContent>

          <TabsContent value="notes" className="m-0 p-6">
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <MessageSquare className="h-12 w-12 text-muted-foreground/50" />
              <p className="mt-3 text-sm font-medium text-muted-foreground">
                {t('noNotes')}
              </p>
              <p className="text-xs text-muted-foreground">
                {t('notesHint')}
              </p>
            </div>
          </TabsContent>
        </ScrollArea>
      </Tabs>

      {/* Footer - sticky action bar */}
      <div className="flex items-center justify-end gap-3 border-t bg-background px-6 py-3">
        <Button variant="outline">{t('addPayment')}</Button>
        <Button variant="destructive">{t('closeClaim')}</Button>
      </div>
    </div>
  );
}
