'use client';

import { useState, useEffect, useCallback } from 'react';
import { Button, Card, CardHeader, CardTitle, CardContent } from '@beema/ui';
import { ScheduleList, ScheduleModal } from '@/components/batch-schedules';
import type { ScheduleResponse, ScheduleRequest } from '@/types/batch-schedule';
import * as api from '@/lib/api/schedules';

export default function BatchSchedulesPage() {
  const [schedules, setSchedules] = useState<ScheduleResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<ScheduleResponse | null>(null);

  const fetchSchedules = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.listSchedules();
      setSchedules(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load schedules');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSchedules();
  }, [fetchSchedules]);

  const handleSave = async (data: ScheduleRequest) => {
    if (editingSchedule) {
      await api.updateSchedule(editingSchedule.id, data);
    } else {
      await api.createSchedule(data);
    }
    setShowModal(false);
    setEditingSchedule(null);
    await fetchSchedules();
  };

  const handleEdit = (schedule: ScheduleResponse) => {
    setEditingSchedule(schedule);
    setShowModal(true);
  };

  const handleDelete = async (id: string) => {
    try {
      await api.deleteSchedule(id);
      await fetchSchedules();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete schedule');
    }
  };

  const handleTrigger = async (id: string) => {
    try {
      await api.triggerSchedule(id);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to trigger schedule');
    }
  };

  const handleTogglePause = async (id: string, currentlyActive: boolean) => {
    try {
      if (currentlyActive) {
        await api.pauseSchedule(id);
      } else {
        await api.unpauseSchedule(id);
      }
      await fetchSchedules();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update schedule status');
    }
  };

  const handleNewSchedule = () => {
    setEditingSchedule(null);
    setShowModal(true);
  };

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Batch Schedules</h1>
          <p className="text-sm text-gray-500 mt-1">
            Manage automated batch job schedules for this tenant
          </p>
        </div>
        <Button variant="primary" onClick={handleNewSchedule}>
          New Schedule
        </Button>
      </div>

      {error && (
        <div className="mb-4 px-4 py-3 rounded-md bg-red-50 border border-red-200">
          <div className="flex items-center justify-between">
            <p className="text-sm text-red-700">{error}</p>
            <button
              onClick={() => setError(null)}
              className="text-red-400 hover:text-red-600 text-sm"
            >
              Dismiss
            </button>
          </div>
        </div>
      )}

      <Card>
        <CardContent>
          <ScheduleList
            schedules={schedules}
            loading={loading}
            onEdit={handleEdit}
            onDelete={handleDelete}
            onTrigger={handleTrigger}
            onTogglePause={handleTogglePause}
          />
        </CardContent>
      </Card>

      {showModal && (
        <ScheduleModal
          schedule={editingSchedule}
          onSave={handleSave}
          onClose={() => {
            setShowModal(false);
            setEditingSchedule(null);
          }}
        />
      )}
    </div>
  );
}
