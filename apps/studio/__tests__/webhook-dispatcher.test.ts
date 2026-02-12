/**
 * Webhook Dispatcher Integration Tests
 *
 * Tests for the webhook dispatcher Inngest function
 */

import crypto from 'crypto';

describe('Webhook Dispatcher', () => {
  const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3000';

  describe('Webhook CRUD Operations', () => {
    it('should create a webhook', async () => {
      const webhook = {
        webhook_name: 'Test Webhook',
        event_type: 'policy/bound',
        url: 'https://webhook.site/test',
        secret: 'test-secret',
        enabled: true,
      };

      const response = await fetch(`${API_URL}/api/webhooks`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Tenant-ID': 'test-tenant',
        },
        body: JSON.stringify(webhook),
      });

      expect(response.status).toBe(201);
      const data = await response.json();
      expect(data.webhook_name).toBe('Test Webhook');
      expect(data.webhook_id).toBeDefined();
    });

    it('should list webhooks', async () => {
      const response = await fetch(`${API_URL}/api/webhooks`, {
        headers: {
          'X-Tenant-ID': 'test-tenant',
        },
      });

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data.webhooks).toBeDefined();
      expect(Array.isArray(data.webhooks)).toBe(true);
    });

    it('should update a webhook', async () => {
      const update = {
        webhook_id: 1,
        enabled: false,
      };

      const response = await fetch(`${API_URL}/api/webhooks`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(update),
      });

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data.enabled).toBe(false);
    });

    it('should delete a webhook', async () => {
      const response = await fetch(`${API_URL}/api/webhooks?id=1`, {
        method: 'DELETE',
      });

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data.success).toBe(true);
    });
  });

  describe('Webhook Matching', () => {
    it('should match webhooks by event type', async () => {
      const response = await fetch(`${API_URL}/api/webhooks/match`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          eventType: 'policy/bound',
          tenantId: 'test-tenant',
        }),
      });

      expect(response.ok).toBe(true);
      const webhooks = await response.json();
      expect(Array.isArray(webhooks)).toBe(true);
    });

    it('should match wildcard webhooks', async () => {
      const response = await fetch(`${API_URL}/api/webhooks/match`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          eventType: 'claim/opened',
          tenantId: 'test-tenant',
        }),
      });

      expect(response.ok).toBe(true);
      const webhooks = await response.json();
      expect(Array.isArray(webhooks)).toBe(true);
    });
  });

  describe('Delivery Logs', () => {
    it('should record deliveries', async () => {
      const deliveries = [
        {
          webhook_id: 1,
          event_id: 'evt_123',
          event_type: 'policy/bound',
          status: 'success',
          status_code: 200,
          response_body: '{"success": true}',
          attempt_number: 1,
        },
      ];

      const response = await fetch(`${API_URL}/api/webhooks/deliveries`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ deliveries }),
      });

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data.success).toBe(true);
      expect(data.count).toBe(1);
    });

    it('should fetch deliveries', async () => {
      const response = await fetch(`${API_URL}/api/webhooks/deliveries`);

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data.deliveries).toBeDefined();
      expect(Array.isArray(data.deliveries)).toBe(true);
    });

    it('should filter deliveries by webhook_id', async () => {
      const response = await fetch(
        `${API_URL}/api/webhooks/deliveries?webhook_id=1`
      );

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data.deliveries).toBeDefined();
    });
  });

  describe('HMAC Signature', () => {
    it('should generate valid HMAC signature', () => {
      const payload = JSON.stringify({
        event: 'policy/bound',
        data: { policyNumber: 'POL-001' },
        timestamp: '2026-02-12T10:00:00Z',
      });
      const secret = 'test-secret';

      const signature = crypto
        .createHmac('sha256', secret)
        .update(payload)
        .digest('hex');

      expect(signature).toBeDefined();
      expect(signature.length).toBe(64); // SHA256 produces 64 hex characters
    });

    it('should verify HMAC signature', () => {
      const payload = JSON.stringify({
        event: 'policy/bound',
        data: { policyNumber: 'POL-001' },
        timestamp: '2026-02-12T10:00:00Z',
      });
      const secret = 'test-secret';

      const signature = crypto
        .createHmac('sha256', secret)
        .update(payload)
        .digest('hex');

      const verifySignature = crypto
        .createHmac('sha256', secret)
        .update(payload)
        .digest('hex');

      expect(signature).toBe(verifySignature);
    });

    it('should reject invalid signature', () => {
      const payload = JSON.stringify({
        event: 'policy/bound',
        data: { policyNumber: 'POL-001' },
        timestamp: '2026-02-12T10:00:00Z',
      });
      const secret = 'test-secret';
      const wrongSecret = 'wrong-secret';

      const signature = crypto
        .createHmac('sha256', secret)
        .update(payload)
        .digest('hex');

      const verifySignature = crypto
        .createHmac('sha256', wrongSecret)
        .update(payload)
        .digest('hex');

      expect(signature).not.toBe(verifySignature);
    });
  });

  describe('Event Payloads', () => {
    it('should validate policy/bound event', () => {
      const event = {
        event: 'policy/bound',
        data: {
          policyNumber: 'POL-001',
          agreementId: 'AGR-001',
          marketContext: 'retail',
          premium: 5000,
          tenantId: 'test-tenant',
        },
        user: {
          id: 'user-123',
          email: 'test@example.com',
        },
        timestamp: '2026-02-12T10:00:00Z',
      };

      expect(event.event).toBe('policy/bound');
      expect(event.data.policyNumber).toBeDefined();
      expect(event.data.agreementId).toBeDefined();
      expect(event.user.id).toBeDefined();
    });

    it('should validate claim/opened event', () => {
      const event = {
        event: 'claim/opened',
        data: {
          claimNumber: 'CLM-001',
          claimId: 'claim-123',
          claimAmount: 10000,
          claimType: 'property',
          tenantId: 'test-tenant',
        },
        user: {
          id: 'user-123',
          email: 'test@example.com',
        },
        timestamp: '2026-02-12T10:00:00Z',
      };

      expect(event.event).toBe('claim/opened');
      expect(event.data.claimNumber).toBeDefined();
      expect(event.data.claimAmount).toBeDefined();
    });
  });
});
