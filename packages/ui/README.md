# @beema/ui

Shared UI components for the Beema platform.

## Components

- **Button** - Button component with variants (primary, secondary, outline, ghost)
- **Card** - Card container with header, title, and content
- **Input** - Input field with error state
- **Label** - Form label with required indicator

## Usage

```tsx
import { Button, Card, Input, Label } from '@beema/ui';

function MyComponent() {
  return (
    <Card>
      <Label required>Name</Label>
      <Input placeholder="Enter name" />
      <Button variant="primary">Submit</Button>
    </Card>
  );
}
```

## Development

This package is consumed by:
- apps/studio
- (future) apps/admin-portal
