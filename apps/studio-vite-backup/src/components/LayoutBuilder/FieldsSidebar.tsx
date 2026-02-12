import React from 'react';
import { useDraggable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import {
  Type,
  Hash,
  Calendar,
  ChevronDown,
  CheckSquare,
  Circle,
  AlignLeft,
  Upload,
} from 'lucide-react';
import { FieldTemplate } from './types';

const fieldTemplates: FieldTemplate[] = [
  {
    type: 'text',
    icon: 'Type',
    label: 'Text Input',
    defaultConfig: {
      type: 'text',
      label: 'Text Field',
      name: 'textField',
      required: false,
      placeholder: 'Enter text...',
    },
  },
  {
    type: 'number',
    icon: 'Hash',
    label: 'Number Input',
    defaultConfig: {
      type: 'number',
      label: 'Number Field',
      name: 'numberField',
      required: false,
      placeholder: 'Enter number...',
    },
  },
  {
    type: 'date',
    icon: 'Calendar',
    label: 'Date Picker',
    defaultConfig: {
      type: 'date',
      label: 'Date Field',
      name: 'dateField',
      required: false,
    },
  },
  {
    type: 'select',
    icon: 'ChevronDown',
    label: 'Dropdown',
    defaultConfig: {
      type: 'select',
      label: 'Select Field',
      name: 'selectField',
      required: false,
      options: [
        { label: 'Option 1', value: 'option1' },
        { label: 'Option 2', value: 'option2' },
      ],
    },
  },
  {
    type: 'checkbox',
    icon: 'CheckSquare',
    label: 'Checkbox',
    defaultConfig: {
      type: 'checkbox',
      label: 'Checkbox Field',
      name: 'checkboxField',
      required: false,
    },
  },
  {
    type: 'radio',
    icon: 'Circle',
    label: 'Radio Group',
    defaultConfig: {
      type: 'radio',
      label: 'Radio Field',
      name: 'radioField',
      required: false,
      options: [
        { label: 'Option 1', value: 'option1' },
        { label: 'Option 2', value: 'option2' },
      ],
    },
  },
  {
    type: 'textarea',
    icon: 'AlignLeft',
    label: 'Text Area',
    defaultConfig: {
      type: 'textarea',
      label: 'Text Area',
      name: 'textareaField',
      required: false,
      placeholder: 'Enter long text...',
    },
  },
  {
    type: 'file',
    icon: 'Upload',
    label: 'File Upload',
    defaultConfig: {
      type: 'file',
      label: 'File Upload',
      name: 'fileField',
      required: false,
    },
  },
];

const iconMap: Record<string, React.ComponentType<{ className?: string }>> = {
  Type,
  Hash,
  Calendar,
  ChevronDown,
  CheckSquare,
  Circle,
  AlignLeft,
  Upload,
};

interface DraggableFieldProps {
  template: FieldTemplate;
}

const DraggableField: React.FC<DraggableFieldProps> = ({ template }) => {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `template-${template.type}`,
    data: template,
  });

  const style = {
    transform: CSS.Translate.toString(transform),
    opacity: isDragging ? 0.5 : 1,
  };

  const IconComponent = iconMap[template.icon];

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
      className="flex items-center gap-3 p-3 bg-white border border-gray-200 rounded-lg cursor-move hover:border-blue-400 hover:shadow-sm transition-all"
    >
      {IconComponent && <IconComponent className="w-5 h-5 text-gray-600" />}
      <span className="text-sm font-medium text-gray-700">{template.label}</span>
    </div>
  );
};

export const FieldsSidebar: React.FC = () => {
  return (
    <div className="w-64 bg-white border-r border-gray-200 p-4 overflow-y-auto">
      <div className="mb-4">
        <h2 className="text-lg font-semibold text-gray-800">Field Types</h2>
        <p className="text-sm text-gray-500 mt-1">Drag fields to the canvas</p>
      </div>
      <div className="space-y-2">
        {fieldTemplates.map((template) => (
          <DraggableField key={template.type} template={template} />
        ))}
      </div>
    </div>
  );
};
