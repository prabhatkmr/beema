"use client";

import { useState } from "react";
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

export function NewPolicySheet({ children }: NewPolicySheetProps) {
  const [formData, setFormData] = useState<Record<string, any>>({});

  const handleChange = (fieldId: string, value: any) => {
    setFormData((prev) => ({ ...prev, [fieldId]: value }));
  };

  return (
    <Sheet>
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
          <Button className="w-full">Submit Quote</Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
