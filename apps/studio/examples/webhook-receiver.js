/**
 * Example Webhook Receiver
 *
 * This is an example of how to receive and verify webhooks from Beema Studio.
 * You can deploy this as an Express server or adapt it to your framework.
 */

const express = require('express');
const crypto = require('crypto');

const app = express();
app.use(express.json());

// Your webhook secret (from sys_webhooks.secret)
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET || 'test-secret-key';

/**
 * Verify HMAC signature
 */
function verifySignature(payload, signature, secret) {
  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(payload)
    .digest('hex');

  return `sha256=${expectedSignature}` === signature;
}

/**
 * Webhook endpoint
 */
app.post('/webhook', (req, res) => {
  // Get signature from header
  const signature = req.headers['x-beema-signature'];
  const eventType = req.headers['x-beema-event'];
  const deliveryId = req.headers['x-beema-delivery'];

  // Verify signature
  const payload = JSON.stringify(req.body);
  if (!verifySignature(payload, signature, WEBHOOK_SECRET)) {
    console.error('Invalid signature');
    return res.status(401).json({ error: 'Invalid signature' });
  }

  // Process event
  console.log(`Received event: ${eventType} (delivery: ${deliveryId})`);
  console.log('Payload:', req.body);

  // Handle different event types
  switch (eventType) {
    case 'policy/bound':
      handlePolicyBound(req.body);
      break;
    case 'claim/opened':
      handleClaimOpened(req.body);
      break;
    case 'claim/settled':
      handleClaimSettled(req.body);
      break;
    case 'agreement/updated':
      handleAgreementUpdated(req.body);
      break;
    default:
      console.log('Unknown event type:', eventType);
  }

  // Respond immediately
  res.json({ success: true, received: true });
});

/**
 * Event handlers
 */
function handlePolicyBound(data) {
  console.log('Policy Bound:', data.data.policyNumber);
  // Send email notification
  // Update CRM
  // Trigger downstream processes
}

function handleClaimOpened(data) {
  console.log('Claim Opened:', data.data.claimNumber);
  // Create case in case management system
  // Notify claims team
  // Start claims workflow
}

function handleClaimSettled(data) {
  console.log('Claim Settled:', data.data.claimNumber);
  // Process payment
  // Update accounting system
  // Close case
}

function handleAgreementUpdated(data) {
  console.log('Agreement Updated:', data.data.agreementId);
  // Sync to data warehouse
  // Trigger re-rating
  // Update policy documents
}

/**
 * Health check endpoint
 */
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

/**
 * Start server
 */
const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
  console.log(`Webhook receiver listening on port ${PORT}`);
  console.log(`POST ${PORT}/webhook - Receive webhooks`);
  console.log(`GET ${PORT}/health - Health check`);
});
