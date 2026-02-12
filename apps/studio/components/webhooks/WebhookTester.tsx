'use client';

import { useState } from 'react';
import { Button, Card, CardHeader, CardTitle, CardContent } from '@beema/ui';

export function WebhookTester({ webhookId }: { webhookId: number }) {
  const [result, setResult] = useState<any>(null);
  const [testing, setTesting] = useState(false);

  const testWebhook = async () => {
    setTesting(true);
    setResult(null);

    try {
      const response = await fetch(`/api/webhooks/${webhookId}/test`, {
        method: 'POST',
      });

      const data = await response.json();
      setResult(data);
    } catch (error: any) {
      setResult({ error: error.message });
    } finally {
      setTesting(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Test Webhook</CardTitle>
      </CardHeader>
      <CardContent>
        <Button onClick={testWebhook} disabled={testing}>
          {testing ? 'Sending...' : 'Send Test Event'}
        </Button>

        {result && (
          <div className="mt-4 p-3 bg-gray-50 rounded text-sm">
            <pre>{JSON.stringify(result, null, 2)}</pre>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
