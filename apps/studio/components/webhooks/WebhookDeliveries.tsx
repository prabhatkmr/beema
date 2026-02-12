'use client';

import { useState, useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '@beema/ui';

export function WebhookDeliveries() {
  const [deliveries, setDeliveries] = useState([]);

  useEffect(() => {
    fetchDeliveries();
    const interval = setInterval(fetchDeliveries, 5000); // Refresh every 5s
    return () => clearInterval(interval);
  }, []);

  const fetchDeliveries = async () => {
    const response = await fetch('/api/webhooks/deliveries?limit=10');
    const data = await response.json();
    setDeliveries(data.deliveries || []);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Recent Deliveries</CardTitle>
      </CardHeader>
      <CardContent>
        {deliveries.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-8">
            No deliveries yet
          </p>
        ) : (
          <div className="space-y-3">
            {deliveries.map((delivery: any) => (
              <div
                key={delivery.delivery_id}
                className="p-3 border rounded-lg"
              >
                <div className="flex justify-between items-start mb-2">
                  <p className="text-sm font-medium">{delivery.event_type}</p>
                  {delivery.status === 'success' ? (
                    <span className="px-2 py-1 text-xs bg-green-100 text-green-800 rounded">
                      ✓ Delivered
                    </span>
                  ) : (
                    <span className="px-2 py-1 text-xs bg-red-100 text-red-800 rounded">
                      ✗ Failed
                    </span>
                  )}
                </div>
                <p className="text-xs text-gray-500">
                  {new Date(delivery.delivered_at).toLocaleString()}
                </p>
                {delivery.status_code && (
                  <p className="text-xs text-gray-600 mt-1">
                    HTTP {delivery.status_code}
                  </p>
                )}
                {delivery.error_message && (
                  <p className="text-xs text-red-600 mt-1">
                    {delivery.error_message}
                  </p>
                )}
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
