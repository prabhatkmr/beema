# Accessibility (A11Y) Guide

The Beema Portal is built with accessibility as a core principle, ensuring all users can navigate and interact with the platform effectively.

## Implemented Features

### 1. **Semantic HTML**
- Proper heading hierarchy (h1 → h2 → h3)
- Semantic elements (`<nav>`, `<main>`, `<section>`)
- Meaningful link text

### 2. **ARIA Labels & Roles**
- All interactive elements have descriptive labels
- Screen reader announcements for dynamic content
- Proper role attributes for custom components

### 3. **Keyboard Navigation**
- All interactive elements are keyboard accessible
- Visible focus indicators
- Logical tab order
- Keyboard shortcuts documented

### 4. **Color & Contrast**
- WCAG AA compliant (4.5:1 minimum)
- Color is not the only indicator of information
- High contrast focus indicators

### 5. **Screen Reader Support**
- Descriptive labels for all form controls
- Status announcements for updates
- Hidden content properly marked
- Language changes announced

### 6. **Motion & Animation**
- Respects `prefers-reduced-motion`
- Animations can be disabled
- Smooth but not excessive transitions

## Testing Checklist

### Keyboard Navigation
- [ ] Tab through all interactive elements
- [ ] Enter/Space activates buttons and links
- [ ] Escape closes modals and dropdowns
- [ ] Arrow keys navigate within components
- [ ] Focus is visible at all times

### Screen Reader Testing
- [ ] VoiceOver (macOS): Cmd + F5
- [ ] NVDA (Windows): Download from nvaccess.org
- [ ] JAWS (Windows): Commercial license
- [ ] TalkBack (Android): Settings → Accessibility
- [ ] ChromeVox (Chrome Extension)

### Visual Testing
- [ ] 200% zoom - content is readable
- [ ] High contrast mode works
- [ ] Color blindness simulation
- [ ] Text spacing adjustments work

## Accessibility Features by Component

### Language Switcher
- **ARIA Label**: "Language selector"
- **Screen Reader**: Announces language changes
- **Keyboard**: Standard select navigation

### App Launcher Cards
- **ARIA Label**: Full description with pending count
- **Keyboard**: Focus ring visible, Enter to activate
- **Screen Reader**: Reads title, description, and badge

### Stats Cards
- **Semantic**: Proper heading for labels
- **ARIA**: Icons marked as decorative
- **Screen Reader**: Meaningful value descriptions

### Navigation
- **Skip Link**: Jump to main content
- **Landmark**: `<nav>` role for site navigation
- **Current Page**: aria-current attribute

## Accessibility Utilities

### `announceToScreenReader(message, priority)`
Announce dynamic content changes to screen readers:

```typescript
import { announceToScreenReader } from '@/lib/accessibility-utils';

// Polite announcement (default)
announceToScreenReader('Item added to cart');

// Assertive (interrupts current reading)
announceToScreenReader('Error: Form submission failed', 'assertive');
```

### `useFocusTrap(isActive)`
Trap focus within modals:

```typescript
import { useFocusTrap } from '@/lib/accessibility-utils';

function Modal({ isOpen }) {
  const trapRef = useFocusTrap(isOpen);
  return <div ref={trapRef}>...</div>;
}
```

### `createKeyboardHandler(onClick)`
Add keyboard support to custom interactive elements:

```typescript
import { createKeyboardHandler } from '@/lib/accessibility-utils';

<div
  onClick={handleClick}
  onKeyDown={createKeyboardHandler(handleClick)}
  tabIndex={0}
  role="button"
>
  Click me
</div>
```

### `prefersReducedMotion()`
Check user's motion preferences:

```typescript
import { prefersReducedMotion } from '@/lib/accessibility-utils';

const shouldAnimate = !prefersReducedMotion();
```

## CSS Classes

### Screen Reader Only
`.sr-only` - Hide visually but keep for screen readers:

```html
<span class="sr-only">Additional context for screen readers</span>
```

### Focus Visible
All interactive elements have `focus:ring-2 focus:ring-blue-500` classes for clear focus indication.

## WCAG 2.1 Level AA Compliance

### Perceivable
- ✅ Text alternatives for images
- ✅ Captions and alternatives for multimedia
- ✅ Adaptable content structure
- ✅ Sufficient color contrast (4.5:1)

### Operable
- ✅ All functionality via keyboard
- ✅ Sufficient time for reading
- ✅ No seizure-inducing content
- ✅ Clear navigation and orientation

### Understandable
- ✅ Readable text (language declared)
- ✅ Predictable navigation
- ✅ Input assistance for forms
- ✅ Error identification and suggestions

### Robust
- ✅ Valid HTML markup
- ✅ Compatible with assistive technologies
- ✅ Future-proof implementations

## Common Issues to Avoid

### ❌ Don't
- Use `div` with `onClick` without keyboard support
- Rely only on color to convey information
- Use auto-playing media without controls
- Create keyboard traps
- Use `tabindex` > 0
- Remove focus indicators

### ✅ Do
- Use semantic HTML elements
- Provide multiple ways to convey information
- Include play/pause controls for media
- Test with keyboard only
- Use logical tab order (tabindex="0" or native)
- Keep focus indicators visible

## Resources

- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [MDN Accessibility](https://developer.mozilla.org/en-US/docs/Web/Accessibility)
- [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
- [Deque axe DevTools](https://www.deque.com/axe/devtools/)

## Automated Testing

Run accessibility checks with:

```bash
# Install dependencies
pnpm add -D @axe-core/react eslint-plugin-jsx-a11y

# Run tests
pnpm run test:a11y
```

## Reporting Issues

If you discover accessibility issues:

1. Document the issue with screenshots
2. Include browser and assistive technology used
3. Describe expected vs actual behavior
4. Submit to the development team

Remember: Accessibility is not a feature—it's a fundamental requirement.
