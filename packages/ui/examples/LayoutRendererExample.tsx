import * as React from 'react';
import { LayoutRenderer } from '../components/LayoutRenderer';
import { useLayout } from '../hooks/useLayout';

export function PolicyFormExample() {
  const { layout, loading, error } = useLayout({
    context: 'policy',
    objectType: 'motor_comprehensive',
    marketContext: 'RETAIL',
    userRole: 'underwriter',
  });

  const [formData, setFormData] = React.useState({});

  const handleChange = (fieldId: string, value: any) => {
    setFormData((prev) => ({ ...prev, [fieldId]: value }));
    console.log('Field changed:', fieldId, value);
  };

  if (loading) {
    return <div>Loading layout...</div>;
  }

  if (error) {
    return <div>Error loading layout: {error.message}</div>;
  }

  if (!layout) {
    return <div>No layout found</div>;
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      <LayoutRenderer
        schema={layout}
        data={formData}
        onChange={handleChange}
      />

      <div className="mt-8 p-4 bg-gray-100 rounded">
        <h3 className="font-bold mb-2">Form Data:</h3>
        <pre>{JSON.stringify(formData, null, 2)}</pre>
      </div>
    </div>
  );
}
