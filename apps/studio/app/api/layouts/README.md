# Layout API Routes

## Endpoints

### POST /api/layouts/validate
Validates a layout against local rules and beema-kernel metadata schema.

**Request:**
```json
{
  "layout_name": "Policy Form",
  "layout_type": "form",
  "market_context": "RETAIL",
  "fields": [...]
}
```

**Response:**
```json
{
  "valid": true,
  "message": "Layout validated successfully",
  "warnings": ["Optional warnings"]
}
```

### POST /api/layouts/save
Validates and saves a layout to beema-kernel.

**Response:**
```json
{
  "success": true,
  "layout_id": "uuid",
  "message": "Layout saved successfully"
}
```

### GET /api/layouts
Fetches layouts from beema-kernel.

**Query Parameters:**
- `marketContext`: Filter by market context
- `layoutType`: Filter by layout type

**Response:**
```json
{
  "layouts": [...],
  "count": 5
}
```

## Error Handling

All endpoints include graceful degradation:
- If beema-kernel is unavailable, validation uses local rules only
- Warnings are returned instead of errors for unavailable services
- Save operations can work in offline mode with appropriate warnings

## Environment Variables

Required in `.env.local`:
```bash
BEEMA_KERNEL_URL=http://localhost:8080
NEXT_PUBLIC_API_URL=http://localhost:3000
```
