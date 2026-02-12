# Layout Builder

The Layout Builder is a visual drag-and-drop interface for designing dynamic form layouts in the Beema Studio application. It allows users to create, customize, and manage form layouts with various field types and validation rules.

## Features Overview

- **Drag-and-Drop Interface**: Intuitive field placement using @dnd-kit
- **Multiple Field Types**: Text, number, date, select, checkbox, radio, textarea, and file upload
- **Field Customization**: Configure labels, names, placeholders, and validation rules
- **Real-time Preview**: See field changes instantly in the canvas
- **Layout Persistence**: Save and load layouts from localStorage
- **Import/Export**: Share layouts as JSON files
- **Reordering**: Drag fields within the canvas to reorder them

## How to Use the Layout Builder

### Creating a New Layout

1. Navigate to the Layout Builder page using the navigation menu
2. Click "New Layout" in the sidebar
3. Enter a name for your layout
4. Click "Create Layout"

### Adding Fields to Your Layout

1. **From the Sidebar**: View all available field types in the left sidebar
2. **Drag to Canvas**: Click and drag a field type from the sidebar to the canvas area
3. **Drop on Canvas**: Release the field in the canvas to add it to your layout

### Editing Field Properties

1. **Select a Field**: Click on any field in the canvas to select it
2. **Properties Panel**: The right panel displays editable properties for the selected field
3. **Common Properties**:
   - **Label**: The display label for the field
   - **Field Name**: The key used in form data (e.g., "firstName")
   - **Required**: Toggle whether the field is mandatory
   - **Placeholder**: Hint text shown in the field (for text-based inputs)
   - **Default Value**: Pre-filled value for the field

4. **Type-Specific Properties**:
   - **Select/Radio**: Manage options (label and value pairs)
   - **Number**: Set min/max values
   - **Text**: Set min/max length

### Reordering Fields

1. **Grab Handle**: Hover over a field to see the grip icon (⋮⋮)
2. **Drag**: Click and drag the grip icon to move the field
3. **Drop**: Release at the desired position to reorder

### Deleting Fields

1. **Select Field**: Click on the field you want to delete
2. **Delete Button**: Click the trash icon in the field card
3. **Confirm**: Confirm the deletion in the dialog

### Saving Your Layout

1. **Save Button**: Click the "Save Layout" button in the toolbar
2. **Auto-save**: Changes are saved to the current layout
3. **Version Control**: Each save increments the version number

### Managing Layouts

#### Switching Layouts
- Click any layout in the sidebar to load it

#### Deleting Layouts
- Hover over a layout in the sidebar
- Click the trash icon that appears
- Confirm deletion

### Exporting Layouts

1. **Export Button**: Click "Export" in the toolbar
2. **Copy to Clipboard**: Copy the JSON to share or backup
3. **Download File**: Download the layout as a .json file

### Importing Layouts

1. **Import Button**: Click "Import" in the toolbar
2. **Paste JSON**: Paste the layout JSON into the text area
3. **Import Layout**: Click "Import Layout" to create a new layout from the JSON

## Field Types Reference

### Text Input
- **Use for**: Short text entries (names, emails, etc.)
- **Properties**: Label, name, placeholder, required, default value, min/max length

### Number Input
- **Use for**: Numeric values (age, quantity, price)
- **Properties**: Label, name, placeholder, required, default value, min/max value

### Date Picker
- **Use for**: Date selection
- **Properties**: Label, name, required, default value

### Dropdown (Select)
- **Use for**: Single selection from multiple options
- **Properties**: Label, name, required, options (label/value pairs)

### Checkbox
- **Use for**: Boolean yes/no choices
- **Properties**: Label, name, required, default value (checked/unchecked)

### Radio Group
- **Use for**: Single selection from multiple options (displayed as radio buttons)
- **Properties**: Label, name, required, options (label/value pairs)

### Text Area
- **Use for**: Long text entries (descriptions, comments)
- **Properties**: Label, name, placeholder, required, default value

### File Upload
- **Use for**: File attachments
- **Properties**: Label, name, required

## Keyboard Shortcuts

Currently, the Layout Builder primarily uses mouse interactions. Future versions may include:
- `Cmd/Ctrl + S`: Save layout
- `Delete`: Remove selected field
- `Cmd/Ctrl + Z`: Undo last change
- `Cmd/Ctrl + D`: Duplicate selected field

## Layout JSON Structure

Layouts are stored in the following format:

```json
{
  "id": "unique-uuid",
  "name": "Contact Form",
  "fields": [
    {
      "id": "field-uuid",
      "type": "text",
      "label": "First Name",
      "name": "firstName",
      "required": true,
      "placeholder": "Enter your first name"
    }
  ],
  "metadata": {
    "createdAt": "2024-01-15T10:00:00.000Z",
    "updatedAt": "2024-01-15T12:30:00.000Z",
    "version": 3
  }
}
```

## Integration with beema-kernel

The Layout Builder generates JSON configurations that can be consumed by:

1. **Form Renderers**: Dynamic form generation based on layout definitions
2. **Validation Services**: Server-side validation using field rules
3. **Data Processors**: Transform form submissions according to field mappings
4. **UI Components**: Render forms in the Beema platform applications

### Example Integration

```typescript
import { useLayoutStore } from '@beema/studio/stores/layoutStore';

// Load a layout
const { loadLayout, currentLayout } = useLayoutStore();
loadLayout('layout-id');

// Render fields dynamically
currentLayout?.fields.map(field => (
  <FormField key={field.id} config={field} />
));
```

## Storage

Layouts are stored in the browser's localStorage under the key `beema-layouts`. This means:

- **Persistence**: Layouts survive page refreshes
- **Local Only**: Layouts are not synced across devices or browsers
- **Export for Backup**: Use the export feature to backup important layouts

### Future Storage Options

Future versions may include:
- Cloud storage via beema-kernel API
- Multi-user collaboration
- Version history and rollback
- Layout templates library

## Best Practices

1. **Naming Conventions**:
   - Use descriptive layout names (e.g., "Customer Onboarding Form")
   - Use camelCase for field names (e.g., "firstName", "emailAddress")
   - Keep labels user-friendly and concise

2. **Field Organization**:
   - Group related fields together
   - Place required fields at the top
   - Use consistent field types for similar data

3. **Validation**:
   - Mark fields as required when necessary
   - Set appropriate min/max values for numbers
   - Use placeholders to guide users

4. **Performance**:
   - Avoid creating layouts with hundreds of fields
   - Consider breaking large forms into sections (future feature)

5. **Maintenance**:
   - Regularly export layouts for backup
   - Document custom validation rules
   - Test layouts before deployment

## Troubleshooting

### Fields not dragging
- Ensure you're clicking and holding on the field tile in the sidebar
- Try refreshing the page if drag-and-drop stops working

### Properties not saving
- Check that you've clicked the "Save Layout" button
- Verify that localStorage is enabled in your browser

### Layout not loading
- Check browser console for errors
- Verify the layout ID exists in localStorage
- Try exporting and re-importing the layout

### Import failing
- Validate JSON syntax using a JSON validator
- Ensure all required fields are present in the JSON
- Check that field types are valid

## Technical Details

### Architecture
- **State Management**: Zustand for global layout state
- **Drag & Drop**: @dnd-kit/core and @dnd-kit/sortable
- **Storage**: Browser localStorage with JSON serialization
- **Styling**: Tailwind CSS with custom components

### Dependencies
- `@dnd-kit/core`: ^6.1.0
- `@dnd-kit/sortable`: ^8.0.0
- `zustand`: ^4.4.7
- `uuid`: ^9.0.1
- `lucide-react`: ^0.312.0

### File Structure
```
src/
├── components/
│   └── LayoutBuilder/
│       ├── LayoutBuilder.tsx          # Main component
│       ├── FieldsSidebar.tsx          # Draggable fields sidebar
│       ├── Canvas.tsx                 # Drop zone canvas
│       ├── DroppableField.tsx         # Individual field component
│       ├── FieldPropertiesPanel.tsx   # Edit field properties
│       └── types.ts                   # TypeScript types
├── stores/
│   └── layoutStore.ts                 # Zustand store
└── pages/
    └── LayoutBuilderPage.tsx          # Page wrapper
```

## Future Enhancements

Planned features for future versions:

- [ ] Grid layout support (multiple columns)
- [ ] Conditional field visibility rules
- [ ] Custom validation rules with JEXL expressions
- [ ] Field groups and sections
- [ ] Undo/redo functionality
- [ ] Keyboard shortcuts
- [ ] Field templates and presets
- [ ] Multi-step form support
- [ ] Form preview mode with sample data
- [ ] Integration with beema-kernel API
- [ ] Collaboration features (comments, sharing)
- [ ] Accessibility enhancements
- [ ] Mobile responsive builder
- [ ] Dark mode support

## Support

For issues or questions about the Layout Builder:
1. Check this documentation
2. Review the code examples in the component files
3. Contact the Beema development team

---

**Version**: 0.1.0
**Last Updated**: 2024-02-12
**Maintainer**: Beema Studio Team
