interface ValidationPanelProps {
  errors: string[];
  warnings?: string[];
  onClose: () => void;
}

export function ValidationPanel({ errors, warnings, onClose }: ValidationPanelProps) {
  if (errors.length === 0 && (!warnings || warnings.length === 0)) {
    return null;
  }

  return (
    <div className="fixed bottom-4 right-4 max-w-md bg-white border rounded-lg shadow-lg p-4">
      <div className="flex justify-between items-start mb-2">
        <h4 className="font-semibold">Validation Results</h4>
        <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
          ✕
        </button>
      </div>

      {errors.length > 0 && (
        <div className="mb-2">
          <p className="text-sm font-medium text-red-600 mb-1">Errors:</p>
          <ul className="text-sm text-red-600 list-disc list-inside">
            {errors.map((error, i) => (
              <li key={i}>{error}</li>
            ))}
          </ul>
        </div>
      )}

      {warnings && warnings.length > 0 && (
        <div>
          <p className="text-sm font-medium text-yellow-600 mb-1">Warnings:</p>
          <ul className="text-sm text-yellow-600 list-disc list-inside">
            {warnings.map((warning, i) => (
              <li key={i}>{warning}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
