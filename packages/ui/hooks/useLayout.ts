'use client';

import { useState, useEffect } from 'react';
import { LayoutSchema } from '../types/layout';

export interface UseLayoutOptions {
  context: string;
  objectType: string;
  marketContext?: string;
  tenantId?: string;
  userRole?: string;
}

export function useLayout(options: UseLayoutOptions) {
  const [layout, setLayout] = useState<LayoutSchema | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    async function fetchLayout() {
      setLoading(true);
      setError(null);

      try {
        const params = new URLSearchParams();
        if (options.marketContext) {
          params.append('marketContext', options.marketContext);
        }

        const headers: Record<string, string> = {};
        if (options.tenantId) {
          headers['X-Tenant-ID'] = options.tenantId;
        }
        if (options.userRole) {
          headers['X-User-Role'] = options.userRole;
        }

        const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
        const url = `${apiUrl}/api/v1/layouts/${options.context}/${options.objectType}?${params}`;

        const response = await fetch(url, { headers });

        if (!response.ok) {
          throw new Error(`Failed to fetch layout: ${response.statusText}`);
        }

        const data = await response.json();
        setLayout(data);
      } catch (err) {
        setError(err as Error);
      } finally {
        setLoading(false);
      }
    }

    fetchLayout();
  }, [options.context, options.objectType, options.marketContext, options.tenantId, options.userRole]);

  return { layout, loading, error };
}
