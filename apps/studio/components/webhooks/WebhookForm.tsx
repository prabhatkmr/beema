'use client';

import { useState } from 'react';
import { Button, Input, Label } from '@beema/ui';

interface WebhookFormProps {
  webhook?: any;
  onSave: () => void;
  onCancel: () => void;
}

const EVENT_TYPES = [
  { value: '*', label: 'All Events' },
  { value: 'policy/bound', label: 'Policy Bound' },
  { value: 'policy/renewed', label: 'Policy Renewed' },
  { value: 'policy/cancelled', label: 'Policy Cancelled' },
  { value: 'claim/opened', label: 'Claim Opened' },
  { value: 'claim/updated', label: 'Claim Updated' },
  { value: 'claim/settled', label: 'Claim Settled' },
  { value: 'agreement/created', label: 'Agreement Created' },
  { value: 'agreement/updated', label: 'Agreement Updated' },
];

export function WebhookForm({ webhook, onSave, onCancel }: WebhookFormProps) {
  const [formData, setFormData] = useState({
    webhook_name: webhook?.webhook_name || '',
    event_type: webhook?.event_type || '*',
    url: webhook?.url || '',
    secret: webhook?.secret || generateSecret(),
    enabled: webhook?.enabled !== false,
    headers: webhook?.headers || {},
  });

  const [customHeaders, setCustomHeaders] = useState<Array<{ key: string; value: string }>>(
    Object.entries(formData.headers).map(([key, value]) => ({ key, value: value as string }))
  );

  function generateSecret() {
    return 'whsec_' + Array.from(crypto.getRandomValues(new Uint8Array(32)))
      .map(b => b.toString(16).padStart(2, '0'))
      .join('');
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Convert custom headers to object
    const headers: Record<string, string> = {};
    customHeaders.forEach(({ key, value }) => {
      if (key && value) {
        headers[key] = value;
      }
    });

    const payload = {
      ...formData,
      headers,
      tenant_id: 'default', // TODO: Get from auth context
      created_by: 'current-user', // TODO: Get from auth context
    };

    const response = await fetch(
      webhook ? `/api/webhooks/${webhook.webhook_id}` : '/api/webhooks',
      {
        method: webhook ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      }
    );

    if (response.ok) {
      onSave();
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div>
        <Label required>Webhook Name</Label>
        <Input
          value={formData.webhook_name}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, webhook_name: e.target.value })}
          placeholder="My Webhook"
          required
        />
      </div>

      <div>
        <Label required>Event Type</Label>
        <select
          value={formData.event_type}
          onChange={(e: React.ChangeEvent<HTMLSelectElement>) => setFormData({ ...formData, event_type: e.target.value })}
          className="w-full border rounded px-3 py-2"
          required
        >
          {EVENT_TYPES.map(({ value, label }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        <p className="text-sm text-gray-500 mt-1">
          Choose specific event or "All Events" to receive everything
        </p>
      </div>

      <div>
        <Label required>Webhook URL</Label>
        <Input
          type="url"
          value={formData.url}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, url: e.target.value })}
          placeholder="https://your-app.com/webhooks"
          required
        />
        <p className="text-sm text-gray-500 mt-1">
          HTTPS endpoint that will receive event notifications
        </p>
      </div>

      <div>
        <Label required>Signing Secret</Label>
        <div className="flex gap-2">
          <Input
            value={formData.secret}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, secret: e.target.value })}
            placeholder="whsec_..."
            required
            readOnly
          />
          <Button
            type="button"
            variant="outline"
            onClick={() => setFormData({ ...formData, secret: generateSecret() })}
          >
            Regenerate
          </Button>
        </div>
        <p className="text-sm text-gray-500 mt-1">
          Used to verify webhook authenticity via HMAC signature
        </p>
      </div>

      <div>
        <Label>Custom Headers (Optional)</Label>
        <div className="space-y-2">
          {customHeaders.map((header, index) => (
            <div key={index} className="flex gap-2">
              <Input
                placeholder="Header-Name"
                value={header.key}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                  const newHeaders = [...customHeaders];
                  newHeaders[index].key = e.target.value;
                  setCustomHeaders(newHeaders);
                }}
              />
              <Input
                placeholder="value"
                value={header.value}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                  const newHeaders = [...customHeaders];
                  newHeaders[index].value = e.target.value;
                  setCustomHeaders(newHeaders);
                }}
              />
              <button
                type="button"
                onClick={() => setCustomHeaders(customHeaders.filter((_, i) => i !== index))}
                className="text-red-500 hover:text-red-700"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setCustomHeaders([...customHeaders, { key: '', value: '' }])}
          className="mt-2"
        >
          + Add Header
        </Button>
      </div>

      <div>
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={formData.enabled}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, enabled: e.target.checked })}
          />
          <span className="text-sm font-medium">Enabled</span>
        </label>
      </div>

      <div className="flex gap-2">
        <Button type="submit" variant="primary">
          {webhook ? 'Update Webhook' : 'Create Webhook'}
        </Button>
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  );
}
