'use client';

import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { Loader2, CheckCircle2, XCircle } from 'lucide-react';
import { toast } from 'sonner';
import { format, addYears } from 'date-fns';
import { LayoutRenderer } from '@/components/dynamic/LayoutRenderer';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { Layout, Field, LayoutResponse } from '@/types/layout';

type SubmitResult =
  | { status: 'success'; submissionId: string }
  | { status: 'error'; message: string; detail?: string };

export default function NewProductPage() {
  const params = useParams<{ product: string }>();
  const product = params.product;
  const t = useTranslations('newProduct');
  const tc = useTranslations('common');

  const [layout, setLayout] = useState<Layout | null>(null);
  const [formData, setFormData] = useState<Record<string, any>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [debugOpen, setDebugOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitResult, setSubmitResult] = useState<SubmitResult | null>(null);

  useEffect(() => {
    async function fetchLayout() {
      setLoading(true);
      setError(null);
      try {
        const res = await fetch(`/api/kernel/layouts/${product}`);
        if (!res.ok) {
          throw new Error(
            res.status === 404
              ? `No layout found for product "${product}"`
              : `Failed to fetch layout (${res.status})`
          );
        }
        const data: LayoutResponse = await res.json();
        setLayout(ensureDateFields(data.layout));
      } catch (err) {
        setError(err instanceof Error ? err.message : tc('error'));
      } finally {
        setLoading(false);
      }
    }

    fetchLayout();
  }, [product, tc]);

  // Ensure the layout has a Policy Dates region with start/expiration date fields.
  // If the API layout already has date fields, keep them as-is.
  function ensureDateFields(apiLayout: Layout): Layout {
    const allFields = apiLayout.regions.flatMap((r) => r.fields);
    const hasDateField = allFields.some(
      (f) => f.type === 'DATE' || f.type === 'DATE_PICKER' || f.id === 'start_date' || f.id === 'expiration_date'
    );

    if (hasDateField) {
      // Set default values for existing date fields
      const today = format(new Date(), 'yyyy-MM-dd');
      const oneYearLater = format(addYears(new Date(), 1), 'yyyy-MM-dd');
      setFormData((prev) => ({
        start_date: today,
        expiration_date: oneYearLater,
        ...prev,
      }));
      return apiLayout;
    }

    const today = format(new Date(), 'yyyy-MM-dd');
    const oneYearLater = format(addYears(new Date(), 1), 'yyyy-MM-dd');

    setFormData((prev) => ({
      start_date: today,
      expiration_date: oneYearLater,
      ...prev,
    }));

    const dateFields: Field[] = [
      {
        id: 'start_date',
        label: 'Policy Start Date',
        type: 'DATE',
        required: true,
        placeholder: 'Select start date',
      },
      {
        id: 'expiration_date',
        label: 'Expiration Date',
        type: 'DATE',
        required: true,
        placeholder: 'Select expiration date',
      },
    ];

    return {
      regions: [
        ...apiLayout.regions,
        {
          id: 'policy_dates',
          label: 'Policy Dates',
          columns: 2,
          fields: dateFields,
        },
      ],
    };
  }

  const handleChange = (fieldId: string, value: any) => {
    setFormData((prev) => ({ ...prev, [fieldId]: value }));
  };

  const validateForm = useCallback((): boolean => {
    if (!layout) return false;

    const requiredFields = layout.regions
      .flatMap((region) => region.fields)
      .filter((field) => field.required);

    const missingFields = requiredFields.filter((field) => {
      const value = formData[field.id];
      return value === undefined || value === null || value === '';
    });

    if (missingFields.length > 0) {
      toast.error(t('fillRequired'), {
        description: missingFields.map((f) => f.label).join(', '),
      });
      return false;
    }

    return true;
  }, [layout, formData, t]);

  const handleSubmit = async () => {
    if (!validateForm()) return;

    setIsSubmitting(true);
    setSubmitResult(null);

    try {
      const response = await fetch('/api/kernel/submissions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Tenant-ID': 'default-tenant',
        },
        body: JSON.stringify({
          product,
          data: formData,
        }),
      });

      if (!response.ok) {
        let message = t('submissionFailed');
        let detail: string | undefined;
        try {
          const errorBody = await response.json();
          message = errorBody.message || message;
        } catch {
          // response body wasn't JSON
        }

        if (response.status === 502) {
          message = t('backendUnavailable');
          detail = t('backendUnavailableDetail');
        } else if (response.status === 404) {
          message = t('endpointNotFound');
          detail = t('endpointNotFoundDetail');
        } else if (response.status >= 500) {
          detail = t('serverError');
        }

        setSubmitResult({ status: 'error', message, detail });
        return;
      }

      const result = await response.json();

      setSubmitResult({ status: 'success', submissionId: result.submissionId });
      toast.success(t('quoteStarted'), {
        description: t('submissionIdLabel', { id: result.submissionId }),
      });

      // Reset form after successful submission
      setFormData({});
    } catch (err) {
      setSubmitResult({
        status: 'error',
        message: t('networkError'),
        detail: err instanceof Error ? err.message : undefined,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          <p className="text-sm text-muted-foreground">{t('loading', { product })}</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center py-20">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle className="text-destructive">{t('errorTitle')}</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">{error}</p>
            <Button
              variant="outline"
              className="mt-4"
              onClick={() => window.location.reload()}
            >
              {tc('retry')}
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
        <h2 className="text-2xl font-semibold capitalize">{t('title', { product })}</h2>
        <p className="text-sm text-muted-foreground">
          {t('subtitle', { product })}
        </p>
      </div>

      <LayoutRenderer layout={layout} data={formData} onChange={handleChange} />

      {submitResult?.status === 'error' && (
        <div className="flex items-start gap-3 rounded-lg border border-red-200 bg-red-50 p-4" role="alert" aria-live="assertive">
          <XCircle className="h-5 w-5 text-red-600 mt-0.5 shrink-0" aria-hidden="true" />
          <div className="flex-1">
            <p className="text-sm font-medium text-red-800">{submitResult.message}</p>
            {submitResult.detail && (
              <p className="mt-1 text-sm text-red-600">{submitResult.detail}</p>
            )}
          </div>
          <button
            onClick={() => setSubmitResult(null)}
            className="text-red-400 hover:text-red-600"
            aria-label={t('dismiss')}
          >
            <XCircle className="h-4 w-4" aria-hidden="true" />
          </button>
        </div>
      )}

      {submitResult?.status === 'success' && (
        <div className="flex items-start gap-3 rounded-lg border border-green-200 bg-green-50 p-4" role="status">
          <CheckCircle2 className="h-5 w-5 text-green-600 mt-0.5 shrink-0" aria-hidden="true" />
          <div className="flex-1">
            <p className="text-sm font-medium text-green-800">{t('quoteWorkflowStarted')}</p>
            <p className="mt-1 text-sm text-green-600">
              {t('submissionIdLabel', { id: submitResult.submissionId })}
            </p>
          </div>
        </div>
      )}

      <div className="flex items-center gap-3">
        <Button onClick={handleSubmit} disabled={isSubmitting}>
          {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" aria-hidden="true" />}
          {isSubmitting ? t('submitting') : t('submit')}
        </Button>
        <Button
          variant="outline"
          onClick={() => setDebugOpen((prev) => !prev)}
        >
          {debugOpen ? t('hideDebug') : t('showDebug')}
        </Button>
      </div>

      {debugOpen && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium">{t('formState')}</CardTitle>
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
