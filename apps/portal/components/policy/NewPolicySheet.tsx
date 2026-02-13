"use client";

import { useState, useCallback } from "react";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { format, addYears } from "date-fns";
import {
  Sheet,
  SheetTrigger,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { LayoutRenderer } from "@/components/dynamic/LayoutRenderer";
import type { Layout } from "@/types/layout";

interface NewPolicySheetProps {
  children: React.ReactNode;
  onSuccess?: () => void;
}

const gadgetPolicyLayout: Layout = {
  regions: [
    {
      id: "quote-info",
      label: "Quote Information",
      columns: 2,
      fields: [
        { id: "product", label: "Product", type: "SELECT", required: true, options: ["Commercial Property", "Professional Indemnity", "Cyber Liability", "Directors & Officers", "Marine Cargo", "Employers Liability"], placeholder: "Select product..." },
        { id: "lineOfBusiness", label: "Line of Business", type: "SELECT", required: true, options: ["Retail", "Commercial", "London Market"], placeholder: "Select line..." },
        { id: "effectiveDate", label: "Effective Date", type: "DATE", required: true, placeholder: "Select effective date" },
        { id: "expiryDate", label: "Expiry Date", type: "DATE", required: true, placeholder: "Select expiry date" },
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

function getDefaultDates() {
  const today = format(new Date(), "yyyy-MM-dd");
  const oneYearLater = format(addYears(new Date(), 1), "yyyy-MM-dd");
  return { effectiveDate: today, expiryDate: oneYearLater };
}

export function NewPolicySheet({ children, onSuccess }: NewPolicySheetProps) {
  const [open, setOpen] = useState(false);
  const [formData, setFormData] = useState<Record<string, any>>(getDefaultDates());
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (fieldId: string, value: any) => {
    setFormData((prev) => ({ ...prev, [fieldId]: value }));
  };

  const validateForm = useCallback((): boolean => {
    const requiredFields = gadgetPolicyLayout.regions
      .flatMap((region) => region.fields)
      .filter((field) => field.required);

    const missingFields = requiredFields.filter((field) => {
      const value = formData[field.id];
      return value === undefined || value === null || value === '';
    });

    if (missingFields.length > 0) {
      toast.error("Please fill in all required fields", {
        description: missingFields.map((f) => f.label).join(", "),
      });
      return false;
    }

    return true;
  }, [formData]);

  const handleSubmit = async () => {
    if (!validateForm()) return;

    setIsSubmitting(true);

    try {
      const selectedProduct = formData.product || "general";

      const response = await fetch("/api/kernel/submissions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Tenant-ID": "default-tenant",
        },
        body: JSON.stringify({
          product: selectedProduct,
          data: formData,
        }),
      });

      if (!response.ok) {
        let message = "Submission failed";
        try {
          const errorBody = await response.json();
          message = errorBody.message || message;
        } catch {
          // response body wasn't JSON
        }
        throw new Error(message);
      }

      const result = await response.json();

      toast.success("Quote Started!", {
        description: `Submission ID: ${result.submissionId}`,
      });

      // Reset form and close sheet
      setFormData(getDefaultDates());
      setOpen(false);

      // Notify parent of success
      onSuccess?.();
    } catch (err) {
      toast.error("Failed to start quote", {
        description: err instanceof Error ? err.message : "An unexpected error occurred",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>{children}</SheetTrigger>
      <SheetContent side="right" className="flex w-full flex-col sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>New Quote</SheetTitle>
          <SheetDescription>Create a new policy quote</SheetDescription>
        </SheetHeader>

        <ScrollArea className="flex-1 px-4">
          <div className="pb-4">
            <LayoutRenderer
              layout={gadgetPolicyLayout}
              data={formData}
              onChange={handleChange}
            />
          </div>
        </ScrollArea>

        <SheetFooter>
          <Button
            className="w-full"
            onClick={handleSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {isSubmitting ? "Submitting..." : "Submit Quote"}
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
