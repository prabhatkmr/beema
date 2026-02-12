'use client';

import { Button, Card, CardHeader, CardTitle, CardContent } from '@beema/ui';

interface WebhookListProps {
  webhooks: any[];
  onEdit: (webhook: any) => void;
  onDelete: (webhookId: number) => void;
}

export function WebhookList({ webhooks, onEdit, onDelete }: WebhookListProps) {
  if (webhooks.length === 0) {
    return (
      <Card>
        <CardContent className="py-12 text-center text-gray-400">
          <svg className="w-16 h-16 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          <p>No webhooks configured</p>
          <p className="text-sm mt-2">Create your first webhook to get started</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {webhooks.map((webhook) => (
        <Card key={webhook.webhook_id}>
          <CardHeader>
            <div className="flex justify-between items-start">
              <div>
                <CardTitle className="flex items-center gap-2">
                  {webhook.webhook_name}
                  {webhook.enabled ? (
                    <span className="px-2 py-1 text-xs bg-green-100 text-green-800 rounded">
                      Active
                    </span>
                  ) : (
                    <span className="px-2 py-1 text-xs bg-gray-100 text-gray-800 rounded">
                      Disabled
                    </span>
                  )}
                </CardTitle>
                <p className="text-sm text-gray-500 mt-1">
                  Event: {webhook.event_type === '*' ? 'All Events' : webhook.event_type}
                </p>
              </div>
              <div className="flex gap-2">
                <Button size="sm" variant="outline" onClick={() => onEdit(webhook)}>
                  Edit
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => onDelete(webhook.webhook_id)}
                  className="text-red-600 hover:bg-red-50"
                >
                  Delete
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <div>
                <p className="text-sm font-medium">URL</p>
                <p className="text-sm text-gray-600 font-mono">{webhook.url}</p>
              </div>
              <div>
                <p className="text-sm font-medium">Secret</p>
                <p className="text-sm text-gray-600 font-mono">
                  {webhook.secret.substring(0, 20)}...
                </p>
              </div>
              {Object.keys(webhook.headers || {}).length > 0 && (
                <div>
                  <p className="text-sm font-medium">Custom Headers</p>
                  <pre className="text-xs bg-gray-50 p-2 rounded mt-1">
                    {JSON.stringify(webhook.headers, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
