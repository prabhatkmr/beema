import { serve } from 'inngest/next';
import { inngest } from '@/lib/inngest/client';
import { webhookDispatcher } from '@/inngest/webhook-dispatcher';

// Serve Inngest functions
export const { GET, POST, PUT } = serve({
  client: inngest,
  functions: [
    webhookDispatcher,
  ],
});
