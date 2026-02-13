"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { LayoutRenderer } from "@/components/dynamic/LayoutRenderer";
import { Button } from "@/components/ui/button";
import type { Layout, LayoutResponse } from "@/types/layout";

const MOCK_LAYOUT: Layout = {
  regions: [
    {
      id: "insured_details",
      label: "Insured Details",
      columns: 2,
      fields: [
        {
          id: "first_name",
          label: "First Name",
          type: "TEXT",
          required: true,
        },
        {
          id: "coverage_amount",
          label: "Coverage",
          type: "CURRENCY",
          required: true,
        },
      ],
    },
  ],
};

export default function SubmissionPage() {
  const params = useParams<{ product: string }>();
  const product = params.product;

  const [layout, setLayout] = useState<Layout | null>(null);
  const [formData, setFormData] = useState<Record<string, any>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchLayout() {
      setLoading(true);
      try {
        const res = await fetch(
          `http://localhost:8080/api/v1/layouts/${product}`
        );
        if (!res.ok) throw new Error("API unavailable");
        const data: LayoutResponse = await res.json();
        setLayout(data.layout);
      } catch {
        setLayout(MOCK_LAYOUT);
      } finally {
        setLoading(false);
      }
    }

    fetchLayout();
  }, [product]);

  const handleChange = (fieldId: string, value: any) => {
    setFormData((prev) => ({ ...prev, [fieldId]: value }));
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          <p className="text-sm text-muted-foreground">
            Loading {product} submission...
          </p>
        </div>
      </div>
    );
  }

  if (!layout) return null;

  return (
    <div className="pb-24">
      <div className="mb-6">
        <h2 className="text-2xl font-semibold capitalize">
          {product} Submission
        </h2>
        <p className="text-sm text-muted-foreground">
          Complete the form below to submit a new {product} policy.
        </p>
      </div>

      <LayoutRenderer layout={layout} data={formData} onChange={handleChange} />

      <div className="fixed bottom-0 left-0 right-0 border-t bg-background p-4">
        <div className="container mx-auto flex justify-end">
          <Button
            size="lg"
            onClick={() =>
              console.log("Calculate Premium:", JSON.stringify(formData))
            }
          >
            Calculate Premium
          </Button>
        </div>
      </div>
    </div>
  );
}
