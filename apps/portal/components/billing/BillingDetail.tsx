"use client";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { FileText, Clock, Download, Send } from "lucide-react";
import { useTranslations } from 'next-intl';
import { useFormatters } from "@/lib/format-utils";

interface BillingDetailProps {
  invoice: {
    id: string;
    title: string;
    subtitle: string;
    dueDate?: string;
    amount?: number;
    status: string;
    statusColor: "default" | "secondary" | "destructive" | "outline";
    metadata: string;
  };
}

const statusVariantMap: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  Paid: "default",
  Pending: "secondary",
  Overdue: "destructive",
  "Partially Paid": "outline",
  Draft: "secondary",
};

const mockPaymentHistory = [
  { date: "Mar 01, 2026", event: "Invoice Created", description: "Invoice generated for policy renewal premium" },
  { date: "Mar 05, 2026", event: "Reminder Sent", description: "Payment reminder email sent to insured" },
  { date: "Mar 10, 2026", event: "Partial Payment", partialAmount: 2500, description: "Received partial payment" },
  { date: "Mar 15, 2026", event: "Follow-up", description: "Second payment reminder sent for remaining balance" },
];

export function BillingDetail({ invoice }: BillingDetailProps) {
  const t = useTranslations('billing.detail');
  const { formatCurrency } = useFormatters();
  const badgeVariant = statusVariantMap[invoice.status] ?? invoice.statusColor;
  const dueDate = invoice.dueDate ?? "Mar 31, 2026";
  const amount = formatCurrency(invoice.amount ?? 5250);
  const isPaid = invoice.status === "Paid";

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="flex items-center justify-between border-b px-6 py-4">
        <div>
          <h2 className="text-xl font-bold">{invoice.id}</h2>
          <p className="text-sm text-muted-foreground">
            {invoice.title} &middot; Due: {dueDate}
          </p>
        </div>
        <Badge variant={badgeVariant}>{invoice.status}</Badge>
      </div>

      {/* Amount Section */}
      <div className="flex items-center justify-between border-b px-6 py-5">
        <div>
          <p className="text-xs font-medium uppercase text-muted-foreground">{t('totalAmount')}</p>
          <p className="text-3xl font-bold">{amount}</p>
        </div>
        {isPaid ? (
          <Badge variant="default" className="text-sm px-4 py-1.5">{t('paymentReceived')}</Badge>
        ) : (
          <Button size="lg">{t('processPayment')}</Button>
        )}
      </div>

      {/* Tabs */}
      <Tabs defaultValue="invoice" className="flex flex-1 flex-col overflow-hidden">
        <div className="border-b px-6">
          <TabsList className="h-10">
            <TabsTrigger value="invoice" className="gap-1.5">
              <FileText className="h-4 w-4" aria-hidden="true" />
              {t('invoice')}
            </TabsTrigger>
            <TabsTrigger value="history" className="gap-1.5">
              <Clock className="h-4 w-4" aria-hidden="true" />
              {t('history')}
            </TabsTrigger>
          </TabsList>
        </div>

        <ScrollArea className="flex-1">
          <TabsContent value="invoice" className="m-0 p-6">
            <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
              <FileText className="h-16 w-16 text-muted-foreground/40" />
              <p className="mt-4 text-sm font-medium text-muted-foreground">
                {t('invoicePreview')}
              </p>
              <p className="text-xs text-muted-foreground">
                {t('pdfHint')}
              </p>
            </div>
          </TabsContent>

          <TabsContent value="history" className="m-0 p-6">
            <div className="space-y-0">
              {mockPaymentHistory.map((event, index) => (
                <div key={index} className="relative flex gap-4 pb-8 last:pb-0">
                  {/* Vertical line */}
                  {index < mockPaymentHistory.length - 1 && (
                    <div className="absolute left-1.75 top-4 h-full w-px bg-border" />
                  )}
                  {/* Dot */}
                  <div className="relative z-10 mt-1.5 h-3.5 w-3.5 shrink-0 rounded-full border-2 border-primary bg-background" />
                  {/* Content */}
                  <div className="flex-1">
                    <p className="text-sm font-medium">{event.event}</p>
                    <p className="text-xs text-muted-foreground">{event.date}</p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {event.description}
                      {event.partialAmount != null && ` of ${formatCurrency(event.partialAmount)}`}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </TabsContent>
        </ScrollArea>
      </Tabs>

      {/* Footer - sticky action bar */}
      <div className="flex items-center justify-end gap-3 border-t bg-background px-6 py-3">
        <Button variant="outline" className="gap-1.5">
          <Download className="h-4 w-4" />
          {t('downloadInvoice')}
        </Button>
        <Button variant="outline" className="gap-1.5">
          <Send className="h-4 w-4" />
          {t('sendReminder')}
        </Button>
      </div>
    </div>
  );
}
