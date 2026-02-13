'use client';

import { useState, useEffect, useCallback } from 'react';
import { Button } from '@beema/ui';
import type { JobType } from '@/types/batch-schedule';
import { JOB_PARAM_TEMPLATES } from '@/types/batch-schedule';

interface JobParamsEditorProps {
  value: Record<string, unknown>;
  jobType: JobType | '';
  onChange: (params: Record<string, unknown>) => void;
}

export function JobParamsEditor({ value, jobType, onChange }: JobParamsEditorProps) {
  const [rawJson, setRawJson] = useState(() => JSON.stringify(value, null, 2));
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    if (!isEditing) {
      setRawJson(JSON.stringify(value, null, 2));
    }
  }, [value, isEditing]);

  const handleChange = useCallback((text: string) => {
    setRawJson(text);
    try {
      const parsed = JSON.parse(text);
      if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
        setError('Must be a JSON object');
        return;
      }
      setError(null);
      onChange(parsed);
    } catch {
      setError('Invalid JSON syntax');
    }
  }, [onChange]);

  const loadTemplate = useCallback(() => {
    if (!jobType || !(jobType in JOB_PARAM_TEMPLATES)) return;
    const template = JOB_PARAM_TEMPLATES[jobType as JobType];
    const text = JSON.stringify(template, null, 2);
    setRawJson(text);
    setError(null);
    onChange(template);
  }, [jobType, onChange]);

  const formatJson = useCallback(() => {
    try {
      const parsed = JSON.parse(rawJson);
      const formatted = JSON.stringify(parsed, null, 2);
      setRawJson(formatted);
      setError(null);
    } catch {
      setError('Cannot format: Invalid JSON');
    }
  }, [rawJson]);

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-gray-700">Job Parameters (JSON)</span>
        <div className="flex gap-1.5">
          {jobType && jobType in JOB_PARAM_TEMPLATES && (
            <button
              type="button"
              onClick={loadTemplate}
              className="text-xs px-2 py-0.5 rounded bg-blue-50 text-blue-600 hover:bg-blue-100 border border-blue-200"
            >
              Load Template
            </button>
          )}
          <button
            type="button"
            onClick={formatJson}
            className="text-xs px-2 py-0.5 rounded bg-gray-50 text-gray-600 hover:bg-gray-100 border border-gray-200"
          >
            Format
          </button>
        </div>
      </div>

      <div className="relative">
        <textarea
          value={rawJson}
          onChange={(e) => handleChange(e.target.value)}
          onFocus={() => setIsEditing(true)}
          onBlur={() => setIsEditing(false)}
          rows={8}
          className={`w-full font-mono text-sm border rounded-md px-3 py-2 focus:outline-none focus:ring-2 ${
            error
              ? 'border-red-300 focus:ring-red-200'
              : 'border-gray-300 focus:ring-blue-200'
          }`}
          spellCheck={false}
        />
        {/* Line numbers gutter */}
        <div className="absolute top-0 left-0 w-8 h-full pointer-events-none opacity-0">
          {rawJson.split('\n').map((_, i) => (
            <div key={i} className="text-xs text-gray-300 text-right pr-1 leading-5">
              {i + 1}
            </div>
          ))}
        </div>
      </div>

      {error && (
        <p className="text-xs text-red-600">{error}</p>
      )}

      {!error && rawJson !== '{}' && (
        <p className="text-xs text-gray-400">
          {Object.keys(value).length} parameter{Object.keys(value).length !== 1 ? 's' : ''} configured
        </p>
      )}
    </div>
  );
}
