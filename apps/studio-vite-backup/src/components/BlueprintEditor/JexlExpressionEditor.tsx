import React, { useState, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import { Card } from '../ui/Card';
import { Button } from '../ui/Button';
import { CheckCircle, XCircle, Play } from 'lucide-react';
import { useValidateJexlMutation } from '../../hooks/useBlueprintQuery';
import { useBlueprintStore } from '../../stores/blueprintStore';

interface JexlExpressionEditorProps {
  value: string;
  onChange: (value: string) => void;
  testContext?: Record<string, any>;
}

export const JexlExpressionEditor: React.FC<JexlExpressionEditorProps> = ({
  value,
  onChange,
  testContext = {},
}) => {
  const [localValue, setLocalValue] = useState(value);
  const [validationResult, setValidationResult] = useState<any>(null);
  const validateMutation = useValidateJexlMutation();

  useEffect(() => {
    setLocalValue(value);
  }, [value]);

  const handleEditorChange = (newValue: string | undefined) => {
    if (newValue !== undefined) {
      setLocalValue(newValue);
    }
  };

  const handleBlur = () => {
    onChange(localValue);
  };

  const handleValidate = async () => {
    try {
      const result = await validateMutation.mutateAsync({
        expression: localValue,
        context: testContext,
      });
      setValidationResult(result);
    } catch (error) {
      setValidationResult({ isValid: false, error: 'Validation failed' });
    }
  };

  return (
    <Card title="JEXL Expression Editor" className="h-full flex flex-col">
      <div className="mb-3">
        <div className="flex items-center justify-between mb-2">
          <label className="text-sm font-medium text-gray-700">Expression</label>
          <Button size="sm" variant="ghost" onClick={handleValidate}>
            <Play size={14} className="mr-1" />
            Test
          </Button>
        </div>
        <div className="border border-gray-300 rounded-md overflow-hidden">
          <Editor
            height="150px"
            defaultLanguage="javascript"
            value={localValue}
            onChange={handleEditorChange}
            onBlur={handleBlur}
            options={{
              minimap: { enabled: false },
              lineNumbers: 'off',
              folding: false,
              fontSize: 13,
              padding: { top: 8, bottom: 8 },
              scrollBeyondLastLine: false,
              wordWrap: 'on',
            }}
            theme="vs-light"
          />
        </div>
      </div>

      {validationResult && (
        <div
          className={`p-3 rounded-md mb-3 ${
            validationResult.isValid ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'
          }`}
        >
          <div className="flex items-start gap-2">
            {validationResult.isValid ? (
              <CheckCircle size={18} className="text-green-600 flex-shrink-0 mt-0.5" />
            ) : (
              <XCircle size={18} className="text-red-600 flex-shrink-0 mt-0.5" />
            )}
            <div className="flex-1">
              <p
                className={`text-sm font-medium ${
                  validationResult.isValid ? 'text-green-800' : 'text-red-800'
                }`}
              >
                {validationResult.isValid ? 'Valid Expression' : 'Invalid Expression'}
              </p>
              {validationResult.error && (
                <p className="text-sm text-red-700 mt-1">{validationResult.error}</p>
              )}
              {validationResult.result !== undefined && (
                <div className="mt-2">
                  <p className="text-xs text-gray-600 font-medium mb-1">Result:</p>
                  <pre className="text-xs bg-white p-2 rounded border border-gray-200 overflow-x-auto">
                    {JSON.stringify(validationResult.result, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      <div className="flex-1 overflow-y-auto">
        <div className="space-y-3">
          <div>
            <h4 className="text-sm font-medium text-gray-700 mb-2">Common Functions</h4>
            <div className="space-y-1 text-xs">
              <div className="p-2 bg-gray-50 rounded">
                <code className="text-primary-600">source.field</code>
                <span className="text-gray-600 ml-2">Access source field</span>
              </div>
              <div className="p-2 bg-gray-50 rounded">
                <code className="text-primary-600">source.field || "default"</code>
                <span className="text-gray-600 ml-2">Default value</span>
              </div>
              <div className="p-2 bg-gray-50 rounded">
                <code className="text-primary-600">source.field * 1.05</code>
                <span className="text-gray-600 ml-2">Arithmetic operations</span>
              </div>
              <div className="p-2 bg-gray-50 rounded">
                <code className="text-primary-600">source.firstName + " " + source.lastName</code>
                <span className="text-gray-600 ml-2">String concatenation</span>
              </div>
              <div className="p-2 bg-gray-50 rounded">
                <code className="text-primary-600">
                  source.amount &gt; 1000 ? "high" : "low"
                </code>
                <span className="text-gray-600 ml-2">Conditional</span>
              </div>
            </div>
          </div>

          <div>
            <h4 className="text-sm font-medium text-gray-700 mb-2">Available Context</h4>
            <pre className="text-xs bg-gray-50 p-2 rounded border border-gray-200 overflow-x-auto">
              {JSON.stringify(testContext, null, 2)}
            </pre>
          </div>
        </div>
      </div>
    </Card>
  );
};
