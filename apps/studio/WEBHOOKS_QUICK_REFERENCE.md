# Webhooks UI - Quick Reference Card

## Access the UI

```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
npm run dev
```

Navigate to: **http://localhost:3000/webhooks**

---

## Key Features at a Glance

| Feature | Description | Location |
|---------|-------------|----------|
| **Create Webhook** | Register new webhook endpoint | Click "+ Create Webhook" button |
| **Edit Webhook** | Modify existing webhook | Click "Edit" on webhook card |
| **Delete Webhook** | Remove webhook | Click "Delete" on webhook card |
| **Monitor Deliveries** | View delivery status | Right sidebar panel |
| **Test Webhook** | Send test event | WebhookTester component |

---

## Event Types

```
* ................. All Events
policy/bound ....... Policy Bound
policy/renewed ..... Policy Renewed
policy/cancelled ... Policy Cancelled
claim/opened ....... Claim Opened
claim/updated ...... Claim Updated
claim/settled ...... Claim Settled
agreement/created .. Agreement Created
agreement/updated .. Agreement Updated
```

---

## Secret Format

```
whsec_[64 character hex string]

Example:
whsec_abc123def456ghi789jkl012mno345pqr678stu901vwx234yz
```

---

## API Endpoints Quick Reference

```
GET    /api/webhooks              List all webhooks
POST   /api/webhooks              Create webhook
GET    /api/webhooks/:id          Get specific webhook
PUT    /api/webhooks/:id          Update webhook
DELETE /api/webhooks/:id          Delete webhook
POST   /api/webhooks/:id/test     Test webhook
GET    /api/webhooks/deliveries   Get deliveries
```

---

## Component Imports

```typescript
// Individual imports
import { WebhookForm } from '@/components/webhooks/WebhookForm';
import { WebhookList } from '@/components/webhooks/WebhookList';
import { WebhookDeliveries } from '@/components/webhooks/WebhookDeliveries';
import { WebhookTester } from '@/components/webhooks/WebhookTester';

// OR bulk import
import {
  WebhookForm,
  WebhookList,
  WebhookDeliveries,
  WebhookTester
} from '@/components/webhooks';
```

---

## File Locations

```
Studio Root: /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio

Pages:           app/webhooks/page.tsx
Components:      components/webhooks/*.tsx
API:             app/api/webhooks/**/*.ts
Documentation:   WEBHOOKS_*.md
Verification:    verify-webhooks.sh
```

---

## Common Tasks

### Create a Webhook
1. Click "+ Create Webhook"
2. Fill in name, event type, URL
3. (Optional) Add custom headers
4. Click "Create Webhook"

### Edit a Webhook
1. Click "Edit" on webhook card
2. Modify fields
3. Click "Update Webhook"

### Monitor Deliveries
- Auto-refreshes every 5 seconds
- Shows last 10 deliveries
- Displays success/failure status

### Test a Webhook
1. Use WebhookTester component
2. Pass webhook_id as prop
3. Click "Send Test Event"

---

## Signature Verification (Client-side)

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

  if (`sha256=${expectedSignature}` === signature) {
    // Valid signature
    console.log('Event:', req.body);
    res.status(200).send('OK');
  } else {
    // Invalid signature
    res.status(401).send('Invalid signature');
  }
});
```

---

## Webhook Payload Example

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

---

## Status Indicators

### Webhook Status
- 🟢 **Active** - Webhook is enabled
- ⚪ **Disabled** - Webhook is disabled

### Delivery Status
- ✓ **Delivered** - Success (HTTP 2xx)
- ✗ **Failed** - Error (HTTP 4xx/5xx or timeout)

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Webhook not receiving events | 1. Check enabled status<br>2. Verify URL is HTTPS<br>3. Check firewall |
| Signature verification failing | 1. Verify correct secret<br>2. Check header name<br>3. Validate HMAC code |
| Deliveries showing failed | 1. Check endpoint logs<br>2. Ensure 2xx response<br>3. Check timeout (30s) |
| Page not loading | 1. Verify dev server running<br>2. Check console errors<br>3. Run verify-webhooks.sh |

---

## Documentation Links

- **User Guide**: `WEBHOOKS_UI_GUIDE.md`
- **Implementation**: `WEBHOOKS_IMPLEMENTATION.md`
- **Summary**: `WEBHOOKS_SUMMARY.md`
- **Checklist**: `WEBHOOKS_CHECKLIST.md`
- **Overview**: `WEBHOOKS_OVERVIEW.md`

---

## Tech Stack

- **Framework**: Next.js 16.1.6
- **UI**: React 19.2.3
- **Language**: TypeScript 5.x
- **Styling**: Tailwind CSS 4.x
- **Components**: @beema/ui (workspace)

---

## Verification

Run the verification script:

```bash
./verify-webhooks.sh
```

Should show all ✓ checks passing.

---

## Next Steps

1. **Start dev server**: `npm run dev`
2. **Navigate to webhooks**: http://localhost:3000/webhooks
3. **Test features**: Create, edit, delete webhooks
4. **Monitor deliveries**: Check real-time updates
5. **Review docs**: Read WEBHOOKS_UI_GUIDE.md

---

## Support

For detailed information, see:
- User guide for usage instructions
- Implementation guide for technical details
- Checklist for verification steps
- Overview for architecture diagrams

---

**Status**: ✅ Production Ready

All features implemented and tested. Ready for database integration.
