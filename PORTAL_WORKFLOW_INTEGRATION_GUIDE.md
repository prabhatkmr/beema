# Portal Integration Guide - Workflow Status Queries

## Overview

This guide shows how to update the Beema Portal to query Temporal workflows for job status instead of database tables.

**Key Change:** Replace database polling with workflow queries.

---

## Before & After

### ❌ Before: Database Polling

```typescript
// OLD: Poll database every 5 seconds
const [submission, setSubmission] = useState(null);

useEffect(() => {
  const interval = setInterval(async () => {
    const response = await fetch(`/api/v1/submissions/${submissionId}`);
    const data = await response.json();
    setSubmission(data);

    if (data.status === 'QUOTED') {
      // Update UI
    }
  }, 5000);  // Poll every 5 seconds

  return () => clearInterval(interval);
}, [submissionId]);
```

**Problems:**
- Wasteful (queries every 5 seconds even when no change)
- Stale (up to 5 second delay)
- Database load (every user polling)

### ✅ After: Workflow Queries

```typescript
// NEW: Query workflow state on-demand
const [workflowStatus, setWorkflowStatus] = useState(null);

const fetchStatus = async () => {
  const response = await fetch(`/api/v1/workflow/submission/${submissionId}/status`);
  const data = await response.json();
  setWorkflowStatus(data);
};

// Query once on mount
useEffect(() => {
  fetchStatus();
}, [submissionId]);

// Query when user takes action
const handleRequestQuote = async () => {
  await fetch(`/api/v1/workflow/submission/${submissionId}/quote`, { method: 'POST' });
  await fetchStatus();  // Refresh after action
};
```

**Benefits:**
- On-demand queries (no wasteful polling)
- Real-time state (workflow maintains current state)
- Lower server load

---

## React Hooks

### useSubmissionWorkflow

```typescript
import { useState, useEffect, useCallback } from 'react';

export interface SubmissionWorkflowStatus {
  submissionId: string;
  status: 'DRAFT' | 'QUOTED' | 'BOUND' | 'QUOTE_FAILED' | 'BIND_FAILED';
  quotedPremium: number | null;
  policyNumber: string | null;
  workflowId: string;
}

export function useSubmissionWorkflow(submissionId: string) {
  const [status, setStatus] = useState<SubmissionWorkflowStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchStatus = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/v1/workflow/submission/${submissionId}/status`);

      if (!response.ok) {
        throw new Error('Workflow not found');
      }

      const data = await response.json();
      setStatus(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [submissionId]);

  const requestQuote = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/v1/workflow/submission/${submissionId}/quote`, {
        method: 'POST',
      });

      if (!response.ok) {
        throw new Error('Failed to request quote');
      }

      // Refresh status after sending signal
      await fetchStatus();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [submissionId, fetchStatus]);

  const bind = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/v1/workflow/submission/${submissionId}/bind`, {
        method: 'POST',
      });

      if (!response.ok) {
        throw new Error('Failed to bind submission');
      }

      await fetchStatus();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [submissionId, fetchStatus]);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  return {
    status,
    loading,
    error,
    requestQuote,
    bind,
    refresh: fetchStatus,
  };
}
```

### useRenewalWorkflow

```typescript
export interface RenewalWorkflowStatus {
  policyNumber: string;
  status: 'CALCULATING' | 'AUTO_APPROVED' | 'PENDING_REVIEW' | 'APPROVED' | 'DECLINED' | 'COMPLETED';
  renewalPremium: number | null;
  premiumIncreasePercent: number | null;
  workflowId: string;
}

export function useRenewalWorkflow(policyNumber: string) {
  const [status, setStatus] = useState<RenewalWorkflowStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchStatus = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/v1/workflow/renewal/${policyNumber}/status`);

      if (!response.ok) {
        throw new Error('Workflow not found');
      }

      const data = await response.json();
      setStatus(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [policyNumber]);

  const underwriterReview = useCallback(async (
    approved: boolean,
    adjustedPremium?: number,
    notes?: string
  ) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/v1/workflow/renewal/${policyNumber}/review`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ approved, adjustedPremium, notes }),
      });

      if (!response.ok) {
        throw new Error('Failed to submit review');
      }

      await fetchStatus();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [policyNumber, fetchStatus]);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  return {
    status,
    loading,
    error,
    underwriterReview,
    refresh: fetchStatus,
  };
}
```

---

## React Components

### SubmissionStatusBadge

```typescript
import { useSubmissionWorkflow } from './hooks/useSubmissionWorkflow';

interface Props {
  submissionId: string;
}

export function SubmissionStatusBadge({ submissionId }: Props) {
  const { status, loading, error } = useSubmissionWorkflow(submissionId);

  if (loading) return <span className="badge badge-secondary">Loading...</span>;
  if (error) return <span className="badge badge-danger">Error</span>;
  if (!status) return null;

  const badgeClass = {
    DRAFT: 'badge-secondary',
    QUOTED: 'badge-info',
    BOUND: 'badge-success',
    QUOTE_FAILED: 'badge-danger',
    BIND_FAILED: 'badge-danger',
  }[status.status];

  return (
    <span className={`badge ${badgeClass}`}>
      {status.status}
      {status.quotedPremium && ` - $${status.quotedPremium.toFixed(2)}`}
    </span>
  );
}
```

### SubmissionDetailPage

```typescript
import { useSubmissionWorkflow } from './hooks/useSubmissionWorkflow';

export function SubmissionDetailPage({ submissionId }: { submissionId: string }) {
  const { status, loading, error, requestQuote, bind } = useSubmissionWorkflow(submissionId);

  if (loading && !status) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!status) return <div>Submission not found</div>;

  return (
    <div className="submission-detail">
      <h2>Submission {submissionId}</h2>

      <div className="status-section">
        <h3>Status</h3>
        <SubmissionStatusBadge submissionId={submissionId} />
      </div>

      {status.status === 'DRAFT' && (
        <button onClick={requestQuote} disabled={loading}>
          Request Quote
        </button>
      )}

      {status.status === 'QUOTED' && (
        <div className="quote-section">
          <h3>Quote</h3>
          <p>Premium: ${status.quotedPremium?.toFixed(2)}</p>
          <button onClick={bind} disabled={loading}>
            Bind Policy
          </button>
        </div>
      )}

      {status.status === 'BOUND' && (
        <div className="policy-section">
          <h3>Policy Created</h3>
          <p>Policy Number: {status.policyNumber}</p>
          <a href={`/policies/${status.policyNumber}`}>View Policy</a>
        </div>
      )}
    </div>
  );
}
```

### RenewalReviewPanel

```typescript
import { useRenewalWorkflow } from './hooks/useRenewalWorkflow';
import { useState } from 'react';

export function RenewalReviewPanel({ policyNumber }: { policyNumber: string }) {
  const { status, loading, underwriterReview } = useRenewalWorkflow(policyNumber);
  const [adjustedPremium, setAdjustedPremium] = useState('');
  const [notes, setNotes] = useState('');

  if (!status || status.status !== 'PENDING_REVIEW') {
    return null;
  }

  const handleApprove = () => {
    underwriterReview(
      true,
      adjustedPremium ? parseFloat(adjustedPremium) : undefined,
      notes || undefined
    );
  };

  const handleDecline = () => {
    underwriterReview(false, undefined, notes);
  };

  return (
    <div className="renewal-review-panel">
      <h3>Renewal Pending Review</h3>

      <div className="premium-info">
        <p>Renewal Premium: ${status.renewalPremium?.toFixed(2)}</p>
        <p>
          Increase: {((status.premiumIncreasePercent || 0) * 100).toFixed(2)}%
        </p>
      </div>

      <div className="review-form">
        <label>
          Adjusted Premium (optional):
          <input
            type="number"
            value={adjustedPremium}
            onChange={(e) => setAdjustedPremium(e.target.value)}
            placeholder={status.renewalPremium?.toFixed(2)}
          />
        </label>

        <label>
          Notes:
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Underwriter notes..."
          />
        </label>

        <div className="actions">
          <button onClick={handleApprove} disabled={loading} className="btn-success">
            Approve
          </button>
          <button onClick={handleDecline} disabled={loading} className="btn-danger">
            Decline
          </button>
        </div>
      </div>
    </div>
  );
}
```

---

## Real-Time Updates (Optional)

For real-time updates without polling, use Server-Sent Events (SSE) or WebSockets.

### Server-Sent Events (SSE)

**Backend:**
```java
@GetMapping("/workflow/submission/{submissionId}/stream")
public SseEmitter streamStatus(@PathVariable String submissionId) {
    SseEmitter emitter = new SseEmitter();

    executor.execute(() -> {
        try {
            while (!emitter.complete) {
                SubmissionWorkflow workflow = workflowClient.newWorkflowStub(...);
                String status = workflow.getStatus();

                emitter.send(SseEmitter.event()
                    .name("status-update")
                    .data(Map.of("status", status)));

                Thread.sleep(1000);  // Check every second
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });

    return emitter;
}
```

**Frontend:**
```typescript
useEffect(() => {
  const eventSource = new EventSource(`/api/v1/workflow/submission/${submissionId}/stream`);

  eventSource.addEventListener('status-update', (event) => {
    const data = JSON.parse(event.data);
    setStatus(data.status);
  });

  return () => eventSource.close();
}, [submissionId]);
```

---

## Migration Strategy

### Phase 1: Dual Mode
Run both old (database) and new (workflow) systems in parallel.

```typescript
// Fetch from both sources
const [dbStatus, setDbStatus] = useState(null);
const [workflowStatus, setWorkflowStatus] = useState(null);

useEffect(() => {
  // Old database query
  fetch(`/api/v1/submissions/${submissionId}`)
    .then(r => r.json())
    .then(setDbStatus);

  // New workflow query
  fetch(`/api/v1/workflow/submission/${submissionId}/status`)
    .then(r => r.json())
    .then(setWorkflowStatus);
}, [submissionId]);

// Compare and log differences
if (dbStatus && workflowStatus && dbStatus.status !== workflowStatus.status) {
  console.warn('Status mismatch:', { dbStatus, workflowStatus });
}

// Use workflow status (new source of truth)
const displayStatus = workflowStatus || dbStatus;
```

### Phase 2: Workflow Only
Remove database queries.

```typescript
// Only query workflow
const { status } = useSubmissionWorkflow(submissionId);
```

### Phase 3: Deprecate Database Tables
Once confident, drop old job tables.

```sql
-- After successful migration
DROP TABLE submissions_jobs;
DROP TABLE renewal_jobs;
```

---

## Testing

### Unit Tests

```typescript
import { renderHook, act } from '@testing-library/react-hooks';
import { useSubmissionWorkflow } from './useSubmissionWorkflow';

global.fetch = jest.fn();

describe('useSubmissionWorkflow', () => {
  it('fetches status on mount', async () => {
    (fetch as jest.Mock).mockResolvedValue({
      ok: true,
      json: async () => ({
        submissionId: 'SUB-001',
        status: 'DRAFT',
        quotedPremium: null,
      }),
    });

    const { result, waitForNextUpdate } = renderHook(() =>
      useSubmissionWorkflow('SUB-001')
    );

    await waitForNextUpdate();

    expect(result.current.status?.status).toBe('DRAFT');
  });

  it('sends quote signal', async () => {
    (fetch as jest.Mock).mockResolvedValue({ ok: true, json: async () => ({}) });

    const { result, waitForNextUpdate } = renderHook(() =>
      useSubmissionWorkflow('SUB-001')
    );

    await act(async () => {
      await result.current.requestQuote();
    });

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/workflow/submission/SUB-001/quote',
      { method: 'POST' }
    );
  });
});
```

---

## API Reference

### GET /api/v1/workflow/submission/{submissionId}/status

**Response:**
```typescript
{
  submissionId: string;
  status: 'DRAFT' | 'QUOTED' | 'BOUND' | 'QUOTE_FAILED' | 'BIND_FAILED';
  quotedPremium: number | null;
  policyNumber: string | null;
  workflowId: string;
}
```

### POST /api/v1/workflow/submission/{submissionId}/quote

Sends quote signal to workflow.

### POST /api/v1/workflow/submission/{submissionId}/bind

Sends bind signal to workflow.

### GET /api/v1/workflow/renewal/{policyNumber}/status

**Response:**
```typescript
{
  policyNumber: string;
  status: 'CALCULATING' | 'AUTO_APPROVED' | 'PENDING_REVIEW' | 'APPROVED' | 'DECLINED' | 'COMPLETED';
  renewalPremium: number | null;
  premiumIncreasePercent: number | null;
  workflowId: string;
}
```

### POST /api/v1/workflow/renewal/{policyNumber}/review

**Request:**
```typescript
{
  approved: boolean;
  adjustedPremium?: number;
  notes?: string;
}
```

---

## Monitoring

### Temporal UI

View all workflows at: http://localhost:8088

**Features:**
- Real-time workflow status
- Event history
- Pending signals
- Error details

### Custom Dashboard

```typescript
export function WorkflowDashboard() {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    fetch('/api/v1/workflow/stats')
      .then(r => r.json())
      .then(setStats);
  }, []);

  return (
    <div className="workflow-dashboard">
      <h2>Workflow Statistics</h2>
      <div className="stats-grid">
        <div className="stat">
          <span>Draft Submissions</span>
          <span>{stats?.draftCount}</span>
        </div>
        <div className="stat">
          <span>Quoted Submissions</span>
          <span>{stats?.quotedCount}</span>
        </div>
        <div className="stat">
          <span>Pending Renewals</span>
          <span>{stats?.pendingRenewalsCount}</span>
        </div>
      </div>
    </div>
  );
}
```

---

## Summary

✅ Replace database polling with workflow queries
✅ Use React hooks for workflow state management
✅ Send signals via REST API
✅ Optional: Real-time updates with SSE/WebSocket
✅ Migrate gradually with dual mode
✅ Monitor with Temporal UI

**Result:** Lower database load, real-time status, simpler code.
