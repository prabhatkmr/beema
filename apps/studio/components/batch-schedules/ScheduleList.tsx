'use client';

import { useState } from 'react';
import { Button } from '@beema/ui';
import type { ScheduleResponse } from '@/types/batch-schedule';
import { JOB_TYPES } from '@/types/batch-schedule';

interface ScheduleListProps {
  schedules: ScheduleResponse[];
  loading: boolean;
  onEdit: (schedule: ScheduleResponse) => void;
  onDelete: (id: string) => void;
  onTrigger: (id: string) => void;
  onTogglePause: (id: string, currentlyActive: boolean) => void;
}

function getJobTypeLabel(jobType: string): string {
  return JOB_TYPES.find((jt) => jt.value === jobType)?.label || jobType;
}

function formatNextRun(nextRun: string | null): string {
  if (!nextRun) return 'N/A';
  const date = new Date(nextRun);
  if (isNaN(date.getTime())) return nextRun;
  return date.toLocaleString();
}

function ConfirmDialog({
  message,
  onConfirm,
  onCancel,
}: {
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-lg shadow-xl p-6 max-w-sm mx-4">
        <p className="text-sm text-gray-700 mb-4">{message}</p>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" size="sm" onClick={onCancel}>
            Cancel
          </Button>
          <Button variant="primary" size="sm" onClick={onConfirm}>
            Confirm
          </Button>
        </div>
      </div>
    </div>
  );
}

export function ScheduleList({
  schedules,
  loading,
  onEdit,
  onDelete,
  onTrigger,
  onTogglePause,
}: ScheduleListProps) {
  const [confirmAction, setConfirmAction] = useState<{
    type: 'delete' | 'trigger';
    id: string;
    name: string;
  } | null>(null);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
        <span className="ml-3 text-sm text-gray-500">Loading schedules...</span>
      </div>
    );
  }

  if (schedules.length === 0) {
    return (
      <div className="text-center py-12">
        <div className="text-gray-400 text-4xl mb-3">&#128197;</div>
        <p className="text-gray-500 text-sm">No schedules configured yet.</p>
        <p className="text-gray-400 text-xs mt-1">
          Click &quot;New Schedule&quot; to create your first batch schedule.
        </p>
      </div>
    );
  }

  return (
    <>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Job Type</th>
              <th className="px-4 py-3">Cron</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Next Run</th>
              <th className="px-4 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {schedules.map((schedule) => (
              <tr key={schedule.id} className="hover:bg-gray-50 transition-colors">
                <td className="px-4 py-3">
                  <span className="font-medium text-gray-900">{schedule.schedule_id}</span>
                </td>
                <td className="px-4 py-3">
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-700">
                    {getJobTypeLabel(schedule.job_type)}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <code className="text-xs bg-gray-100 px-1.5 py-0.5 rounded font-mono">
                    {schedule.cron_expression}
                  </code>
                </td>
                <td className="px-4 py-3">
                  <span
                    className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                      schedule.is_active
                        ? 'bg-green-100 text-green-700'
                        : 'bg-yellow-100 text-yellow-700'
                    }`}
                  >
                    <span
                      className={`w-1.5 h-1.5 rounded-full mr-1.5 ${
                        schedule.is_active ? 'bg-green-500' : 'bg-yellow-500'
                      }`}
                    />
                    {schedule.is_active ? 'Active' : 'Paused'}
                  </span>
                </td>
                <td className="px-4 py-3 text-gray-600 text-xs">
                  {formatNextRun(schedule.next_run_time)}
                </td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-1.5">
                    <button
                      onClick={() => onEdit(schedule)}
                      className="px-2 py-1 text-xs rounded border border-gray-200 text-gray-600 hover:bg-gray-50"
                      title="Edit"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => onTogglePause(schedule.id, schedule.is_active)}
                      className="px-2 py-1 text-xs rounded border border-gray-200 text-gray-600 hover:bg-gray-50"
                      title={schedule.is_active ? 'Pause' : 'Unpause'}
                    >
                      {schedule.is_active ? 'Pause' : 'Resume'}
                    </button>
                    <button
                      onClick={() =>
                        setConfirmAction({ type: 'trigger', id: schedule.id, name: schedule.schedule_id })
                      }
                      className="px-2 py-1 text-xs rounded border border-blue-200 text-blue-600 hover:bg-blue-50"
                      title="Run Now"
                    >
                      Run Now
                    </button>
                    <button
                      onClick={() =>
                        setConfirmAction({ type: 'delete', id: schedule.id, name: schedule.schedule_id })
                      }
                      className="px-2 py-1 text-xs rounded border border-red-200 text-red-600 hover:bg-red-50"
                      title="Delete"
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {confirmAction && (
        <ConfirmDialog
          message={
            confirmAction.type === 'delete'
              ? `Are you sure you want to delete schedule "${confirmAction.name}"? This action cannot be undone.`
              : `Trigger an immediate run of "${confirmAction.name}"?`
          }
          onConfirm={() => {
            if (confirmAction.type === 'delete') {
              onDelete(confirmAction.id);
            } else {
              onTrigger(confirmAction.id);
            }
            setConfirmAction(null);
          }}
          onCancel={() => setConfirmAction(null)}
        />
      )}
    </>
  );
}
