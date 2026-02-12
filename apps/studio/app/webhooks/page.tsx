'use client';

import { useState, useEffect } from 'react';
import { Button, Card, CardHeader, CardTitle, CardContent } from '@beema/ui';
import { WebhookList } from '@/components/webhooks/WebhookList';
import { WebhookForm } from '@/components/webhooks/WebhookForm';
import { WebhookDeliveries } from '@/components/webhooks/WebhookDeliveries';

export default function WebhooksPage() {
  const [webhooks, setWebhooks] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editingWebhook, setEditingWebhook] = useState(null);

  useEffect(() => {
    fetchWebhooks();
  }, []);

  const fetchWebhooks = async () => {
    const response = await fetch('/api/webhooks');
    const data = await response.json();
    setWebhooks(data.webhooks || []);
  };

  const handleCreate = () => {
    setEditingWebhook(null);
    setShowForm(true);
  };

  const handleEdit = (webhook: any) => {
    setEditingWebhook(webhook);
    setShowForm(true);
  };

  const handleSave = async () => {
    setShowForm(false);
    setEditingWebhook(null);
    await fetchWebhooks();
  };

  const handleDelete = async (webhookId: number) => {
    if (!confirm('Delete this webhook?')) return;

    await fetch(`/api/webhooks/${webhookId}`, {
      method: 'DELETE',
    });

    await fetchWebhooks();
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8 flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">Webhooks</h1>
          <p className="text-gray-600 mt-2">
            Receive real-time notifications for events in your Beema account
          </p>
        </div>
        <Button onClick={handleCreate}>
          + Create Webhook
        </Button>
      </div>

      {showForm && (
        <div className="mb-8">
          <Card>
            <CardHeader>
              <CardTitle>
                {editingWebhook ? 'Edit Webhook' : 'Create Webhook'}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <WebhookForm
                webhook={editingWebhook}
                onSave={handleSave}
                onCancel={() => {
                  setShowForm(false);
                  setEditingWebhook(null);
                }}
              />
            </CardContent>
          </Card>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <WebhookList
            webhooks={webhooks}
            onEdit={handleEdit}
            onDelete={handleDelete}
          />
        </div>

        <div>
          <WebhookDeliveries />
        </div>
      </div>
    </div>
  );
}
