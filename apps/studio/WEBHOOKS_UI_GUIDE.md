# Webhooks UI Guide

## Overview
The Webhooks tab in Studio allows users to register, manage, and monitor webhook endpoints for real-time event notifications.

## Features

- **Create Webhooks**: Register URLs to receive events
- **Event Selection**: Choose specific events or all events
- **Secret Management**: Auto-generated HMAC signing secrets
- **Custom Headers**: Add authentication headers
- **Delivery Monitoring**: View recent webhook deliveries
- **Test Webhooks**: Send test events

## Usage

### 1. Create Webhook

1. Navigate to Webhooks tab
2. Click "Create Webhook"
3. Enter:
   - Name (e.g., "Slack Notifications")
   - Event type (e.g., "claim/opened" or "*" for all)
   - URL (must be HTTPS)
   - Secret (auto-generated)
4. Add custom headers if needed
5. Click "Create Webhook"

### 2. Test Webhook

1. Click "Test" on any webhook
2. A test event will be sent
3. Check delivery status in Recent Deliveries

### 3. Monitor Deliveries

Recent Deliveries panel shows:
- Event type
- Delivery status (success/failed)
- Timestamp
- HTTP status code
- Error messages (if failed)

## Webhook Verification

Verify signatures in your endpoint:

```javascript
const crypto = require('crypto');

app.post('/webhook', (req, res) => {
  const signature = req.headers['x-beema-signature'];
  const payload = JSON.stringify(req.body);
  const secret = 'your-webhook-secret';

  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(payload)
    .digest('hex');

  if (`sha256=${expectedSignature}` !== signature) {
    return res.status(401).send('Invalid signature');
  }

  // Process event
  console.log('Event:', req.body);
  res.status(200).send('OK');
});
```

## Payload Structure

```json
{
  "event": "claim/opened",
  "data": {
    "claimNumber": "CLM-001",
    "claimId": "claim-123",
    "claimAmount": 5000.00,
    "claimType": "motor_accident"
  },
  "user": {
    "id": "user-1",
    "email": "user@beema.io"
  },
  "timestamp": 1706800000000
}
```

## Available Events

- `*` - All Events
- `policy/bound` - Policy Bound
- `policy/renewed` - Policy Renewed
- `policy/cancelled` - Policy Cancelled
- `claim/opened` - Claim Opened
- `claim/updated` - Claim Updated
- `claim/settled` - Claim Settled
- `agreement/created` - Agreement Created
- `agreement/updated` - Agreement Updated

## Custom Headers

Add custom headers to webhook requests for authentication or routing:

```json
{
  "Authorization": "Bearer your-api-token",
  "X-Custom-Header": "custom-value"
}
```

## Retry Behavior

Webhooks are automatically retried on failure:
- Maximum 3 attempts
- Exponential backoff starting at 1 second
- Failed deliveries are logged for monitoring

## Security Best Practices

1. **Always use HTTPS URLs** - HTTP endpoints are not supported
2. **Verify signatures** - Use the secret to validate webhook authenticity
3. **Store secrets securely** - Never commit secrets to version control
4. **Use custom headers** - Add additional authentication layers if needed
5. **Monitor deliveries** - Check the deliveries panel for failures

## API Endpoints

### List Webhooks
```
GET /api/webhooks
```

### Create Webhook
```
POST /api/webhooks
```

### Update Webhook
```
PUT /api/webhooks/:id
```

### Delete Webhook
```
DELETE /api/webhooks/:id
```

### Test Webhook
```
POST /api/webhooks/:id/test
```

### Get Deliveries
```
GET /api/webhooks/deliveries?limit=10
```

## Troubleshooting

### Webhook not receiving events
- Verify the webhook is enabled
- Check the URL is accessible from the internet
- Ensure HTTPS is used (HTTP not supported)
- Check firewall rules

### Signature verification failing
- Ensure you're using the correct secret
- Verify the payload is being hashed correctly
- Check the signature header name is `x-beema-signature`

### Deliveries showing as failed
- Check the endpoint logs for errors
- Verify the endpoint returns a 2xx status code
- Ensure the endpoint responds within 30 seconds
- Review the error message in the deliveries panel

## Integration Examples

### Slack Integration

```javascript
app.post('/webhook', async (req, res) => {
  const event = req.body;

  if (event.event === 'claim/opened') {
    await slack.chat.postMessage({
      channel: '#claims',
      text: `New claim opened: ${event.data.claimNumber}`,
    });
  }

  res.status(200).send('OK');
});
```

### Email Notification

```javascript
app.post('/webhook', async (req, res) => {
  const event = req.body;

  if (event.event === 'policy/bound') {
    await sendEmail({
      to: event.user.email,
      subject: 'Policy Bound Confirmation',
      body: `Your policy has been bound successfully.`,
    });
  }

  res.status(200).send('OK');
});
```

## Next Steps

- Set up your first webhook endpoint
- Configure event subscriptions
- Monitor delivery success rates
- Implement retry logic in your application
- Set up alerts for failed deliveries
