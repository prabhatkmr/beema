'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { LayoutRenderer } from '@/components/dynamic/LayoutRenderer';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { Layout, LayoutResponse } from '@/types/layout';

export default function NewProductPage() {
  const params = useParams<{ product: string }>();
  const product = params.product;

  const [layout, setLayout] = useState<Layout | null>(null);
  const [formData, setFormData] = useState<Record<string, any>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [debugOpen, setDebugOpen] = useState(false);

  useEffect(() => {
    async function fetchLayout() {
      setLoading(true);
      setError(null);
      try {
        const res = await fetch(`http://localhost:8080/api/v1/layouts/${product}`);
        if (!res.ok) {
          throw new Error(
            res.status === 404
              ? `No layout found for product "${product}"`
              : `Failed to fetch layout (${res.status})`
          );
        }
        const data: LayoutResponse = await res.json();
        setLayout(data.layout);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'An unexpected error occurred');
      } finally {
        setLoading(false);
      }
    }

    fetchLayout();
  }, [product]);

  const handleChange = (fieldId: string, value: any) => {
    setFormData((prev) => ({ ...prev, [fieldId]: value }));
  };

  const handleSubmit = () => {
    console.log('Form submission for product:', product);
    console.log('Form data:', JSON.stringify(formData, null, 2));
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          <p className="text-sm text-muted-foreground">Loading {product} form...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center py-20">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle className="text-destructive">Error</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">{error}</p>
            <Button
              variant="outline"
              className="mt-4"
              onClick={() => window.location.reload()}
            >
              Retry
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!layout) {
    return null;
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold capitalize">New {product}</h2>
        <p className="text-sm text-muted-foreground">
          Fill in the details below to create a new {product} record.
        </p>
      </div>

      <LayoutRenderer layout={layout} data={formData} onChange={handleChange} />

      <div className="flex items-center gap-3">
        <Button onClick={handleSubmit}>Submit</Button>
        <Button
          variant="outline"
          onClick={() => setDebugOpen((prev) => !prev)}
        >
          {debugOpen ? 'Hide' : 'Show'} Debug
        </Button>
      </div>

      {debugOpen && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium">Form State (Debug)</CardTitle>
          </CardHeader>
          <CardContent>
            <pre className="overflow-auto rounded bg-muted p-4 text-xs">
              {JSON.stringify(formData, null, 2)}
            </pre>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
