import { useState, useEffect, useCallback } from 'react';
import type { InfrastructureStatusResponse } from '@/types/infrastructure';

const POLL_INTERVAL = 30_000;

export function useInfrastructureStatus() {
  const [data, setData] = useState<InfrastructureStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStatus = useCallback(async () => {
    try {
      const response = await fetch('/api/infrastructure/status');
      if (!response.ok) throw new Error('Failed to fetch infrastructure status');
      const result: InfrastructureStatusResponse = await response.json();
      setData(result);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [fetchStatus]);

  return { data, loading, error, refetch: fetchStatus };
}
