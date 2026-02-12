import * as React from 'react';
import { LayoutRenderer } from '../components/dynamic/LayoutRenderer';
import type { DynamicLayoutSchema } from '../types/layout';

const sampleLayout: DynamicLayoutSchema = {
  title: 'Motor Policy Form',
  regions: [
    {
      id: 'vehicle-region',
      title: 'Vehicle Information',
      layout: 'grid',
      columns: 1,
      sections: [
        {
          id: 'vehicle-details',
          title: 'Vehicle Details',
          layout: 'grid',
          columns: 2,
          fields: [
            { id: 'make', label: 'Make', widget_type: 'TEXT', required: true },
            { id: 'model', label: 'Model', widget_type: 'TEXT', required: true },
            { id: 'year', label: 'Year', widget_type: 'NUMBER', validation: { min: 1990, max: 2026 } },
            { id: 'value', label: 'Vehicle Value', widget_type: 'CURRENCY', required: true },
          ],
        },
      ],
    },
  ],
  sections: [
    {
      id: 'policy-details',
      title: 'Policy Details',
      layout: 'grid',
      columns: 2,
      fields: [
        { id: 'start_date', label: 'Start Date', widget_type: 'DATE', required: true },
        { id: 'end_date', label: 'End Date', widget_type: 'DATE', required: true },
        {
          id: 'coverage_type',
          label: 'Coverage Type',
          widget_type: 'SELECT',
          required: true,
          options: [
            { value: 'third_party', label: 'Third Party Only' },
            { value: 'comprehensive', label: 'Comprehensive' },
          ],
        },
        {
          id: 'has_ncb',
          label: 'Has No-Claims Bonus?',
          widget_type: 'CHECKBOX',
        },
        {
          id: 'ncb_years',
          label: 'NCB Years',
          widget_type: 'NUMBER',
          visible_if: 'has_ncb == true',
          validation: { min: 0, max: 20 },
        },
      ],
    },
    {
      id: 'notes-section',
      title: 'Additional Notes',
      fields: [
        { id: 'notes', label: 'Notes', widget_type: 'TEXTAREA' },
      ],
    },
  ],
};

export function DynamicLayoutExample() {
  const [data, setData] = React.useState<Record<string, any>>({});

  const handleChange = (fieldId: string, value: any) => {
    setData((prev) => ({ ...prev, [fieldId]: value }));
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <LayoutRenderer
        layout={sampleLayout}
        data={data}
        onChange={handleChange}
      />

      <div className="mt-8 p-4 bg-gray-100 rounded">
        <h3 className="font-bold mb-2">Form Data (debug):</h3>
        <pre className="text-sm">{JSON.stringify(data, null, 2)}</pre>
      </div>
    </div>
  );
}
