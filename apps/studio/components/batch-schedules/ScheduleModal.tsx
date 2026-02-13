'use client';

import { useState, useEffect, useCallback } from 'react';
import { Button, Input, Label } from '@beema/ui';
import { CronHelper } from './CronHelper';
import { JobParamsEditor } from './JobParamsEditor';
import type { ScheduleResponse, ScheduleRequest, JobType } from '@/types/batch-schedule';
import { JOB_TYPES } from '@/types/batch-schedule';

interface ScheduleModalProps {
  schedule: ScheduleResponse | null;
  onSave: (data: ScheduleRequest) => Promise<void>;
  onClose: () => void;
}

interface FormState {
  schedule_id: string;
  job_type: JobType | '';
  cron_expression: string;
  is_active: boolean;
  job_params: Record<string, unknown>;
}

const initialForm: FormState = {
  schedule_id: '',
  job_type: '',
  cron_expression: '',
  is_active: true,
  job_params: {},
};

export function ScheduleModal({ schedule, onSave, onClose }: ScheduleModalProps) {
  const isEditing = !!schedule;
  const [form, setForm] = useState<FormState>(initialForm);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (schedule) {
      setForm({
        schedule_id: schedule.schedule_id,
        job_type: schedule.job_type as JobType,
        cron_expression: schedule.cron_expression,
        is_active: schedule.is_active,
        job_params: schedule.job_params || {},
      });
    } else {
      setForm(initialForm);
    }
  }, [schedule]);

  const validate = useCallback((): boolean => {
    const newErrors: Record<string, string> = {};

    if (!form.schedule_id.trim()) {
      newErrors.schedule_id = 'Schedule name is required';
    } else if (!/^[a-zA-Z0-9_-]+$/.test(form.schedule_id)) {
      newErrors.schedule_id = 'Only letters, numbers, hyphens, and underscores allowed';
    }

    if (!form.job_type) {
      newErrors.job_type = 'Job type is required';
    }

    if (!form.cron_expression.trim()) {
      newErrors.cron_expression = 'Cron expression is required';
    } else {
      const parts = form.cron_expression.trim().split(/\s+/);
      if (parts.length !== 5) {
        newErrors.cron_expression = 'Must have 5 fields (minute hour day month weekday)';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [form]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setSaving(true);
    try {
      await onSave({
        schedule_id: form.schedule_id,
        job_type: form.job_type as JobType,
        cron_expression: form.cron_expression,
        is_active: form.is_active,
        job_params: form.job_params,
      });
    } catch {
      // Error handled by parent
    } finally {
      setSaving(false);
    }
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={handleBackdropClick}
    >
      <div className="bg-white rounded-lg shadow-xl w-full max-w-xl mx-4 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <h2 className="text-lg font-semibold text-gray-900">
            {isEditing ? 'Edit Schedule' : 'New Schedule'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-xl leading-none"
          >
            &times;
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-4 space-y-4">
          {/* Schedule Name */}
          <div>
            <Label>Schedule Name</Label>
            <Input
              value={form.schedule_id}
              onChange={(e) => setForm((f) => ({ ...f, schedule_id: e.target.value }))}
              placeholder="e.g., renewals-daily, gl-export-monthly"
              disabled={isEditing}
            />
            {errors.schedule_id && (
              <p className="text-xs text-red-600 mt-1">{errors.schedule_id}</p>
            )}
          </div>

          {/* Job Type */}
          <div>
            <Label>Job Type</Label>
            <select
              value={form.job_type}
              onChange={(e) => setForm((f) => ({ ...f, job_type: e.target.value as JobType }))}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-200"
            >
              <option value="">Select job type...</option>
              {JOB_TYPES.map((jt) => (
                <option key={jt.value} value={jt.value}>
                  {jt.label}
                </option>
              ))}
            </select>
            {errors.job_type && (
              <p className="text-xs text-red-600 mt-1">{errors.job_type}</p>
            )}
          </div>

          {/* Cron Expression */}
          <div>
            <Label>Cron Expression</Label>
            <Input
              value={form.cron_expression}
              onChange={(e) => setForm((f) => ({ ...f, cron_expression: e.target.value }))}
              placeholder="0 2 * * *"
              className="font-mono"
            />
            {errors.cron_expression && (
              <p className="text-xs text-red-600 mt-1">{errors.cron_expression}</p>
            )}
            <div className="mt-2">
              <CronHelper
                value={form.cron_expression}
                onChange={(cron) => setForm((f) => ({ ...f, cron_expression: cron }))}
              />
            </div>
          </div>

          {/* Job Params */}
          <JobParamsEditor
            value={form.job_params}
            jobType={form.job_type}
            onChange={(params) => setForm((f) => ({ ...f, job_params: params }))}
          />

          {/* Active Toggle */}
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setForm((f) => ({ ...f, is_active: !f.is_active }))}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                form.is_active ? 'bg-blue-600' : 'bg-gray-300'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  form.is_active ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
            <span className="text-sm text-gray-700">
              {form.is_active ? 'Active' : 'Paused'}
            </span>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <Button variant="secondary" type="button" onClick={onClose}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={saving}>
              {saving ? 'Saving...' : isEditing ? 'Update Schedule' : 'Create Schedule'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
