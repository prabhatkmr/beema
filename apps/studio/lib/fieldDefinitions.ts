import { FieldBlockDefinition } from '@/types/layout';

export const FIELD_DEFINITIONS: FieldBlockDefinition[] = [
  {
    type: 'text',
    icon: '📝',
    label: 'Text Input',
    defaultProps: {
      label: 'Text Field',
      placeholder: 'Enter text',
      required: false,
      width: 'full',
    },
  },
  {
    type: 'number',
    icon: '🔢',
    label: 'Number Input',
    defaultProps: {
      label: 'Number Field',
      placeholder: '0',
      required: false,
      width: 'full',
    },
  },
  {
    type: 'email',
    icon: '📧',
    label: 'Email Input',
    defaultProps: {
      label: 'Email',
      placeholder: 'email@example.com',
      required: false,
      width: 'full',
      validation: [
        { type: 'email', message: 'Invalid email format' }
      ],
    },
  },
  {
    type: 'date',
    icon: '📅',
    label: 'Date Picker',
    defaultProps: {
      label: 'Date',
      required: false,
      width: 'full',
    },
  },
  {
    type: 'select',
    icon: '📋',
    label: 'Dropdown',
    defaultProps: {
      label: 'Select Option',
      required: false,
      width: 'full',
      options: [
        { label: 'Option 1', value: 'option1' },
        { label: 'Option 2', value: 'option2' },
      ],
    },
  },
  {
    type: 'checkbox',
    icon: '☑️',
    label: 'Checkbox',
    defaultProps: {
      label: 'Checkbox',
      required: false,
      width: 'full',
    },
  },
  {
    type: 'radio',
    icon: '🔘',
    label: 'Radio Group',
    defaultProps: {
      label: 'Choose One',
      required: false,
      width: 'full',
      options: [
        { label: 'Option 1', value: 'option1' },
        { label: 'Option 2', value: 'option2' },
      ],
    },
  },
  {
    type: 'textarea',
    icon: '📄',
    label: 'Text Area',
    defaultProps: {
      label: 'Long Text',
      placeholder: 'Enter detailed text...',
      required: false,
      width: 'full',
    },
  },
  {
    type: 'file',
    icon: '📎',
    label: 'File Upload',
    defaultProps: {
      label: 'Upload File',
      required: false,
      width: 'full',
    },
  },
];
