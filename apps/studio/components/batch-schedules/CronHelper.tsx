'use client';

import { useState, useEffect } from 'react';
import { Button } from '@beema/ui';
import { CRON_PRESETS } from '@/types/batch-schedule';

interface CronHelperProps {
  value: string;
  onChange: (cron: string) => void;
}

const CRON_PARTS = ['Minute', 'Hour', 'Day of Month', 'Month', 'Day of Week'];

function parseCronToReadable(cron: string): string {
  const parts = cron.trim().split(/\s+/);
  if (parts.length !== 5) return 'Invalid cron expression';

  const [minute, hour, dom, month, dow] = parts;

  if (cron === '* * * * *') return 'Every minute';
  if (minute !== '*' && hour === '*' && dom === '*' && month === '*' && dow === '*') {
    return `At minute ${minute} of every hour`;
  }
  if (minute !== '*' && hour !== '*' && dom === '*' && month === '*' && dow === '*') {
    return `Daily at ${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`;
  }
  if (minute !== '*' && hour !== '*' && dom === '*' && month === '*' && dow !== '*') {
    const dayNames: Record<string, string> = {
      '0': 'Sunday', '1': 'Monday', '2': 'Tuesday', '3': 'Wednesday',
      '4': 'Thursday', '5': 'Friday', '6': 'Saturday',
      '1-5': 'Mon-Fri', '0,6': 'Weekends',
    };
    const dayLabel = dayNames[dow] || `day ${dow}`;
    return `${dayLabel} at ${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`;
  }
  if (minute !== '*' && hour !== '*' && dom !== '*' && month === '*') {
    return `Day ${dom} of every month at ${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`;
  }

  return `${minute} ${hour} ${dom} ${month} ${dow}`;
}

function getNextRuns(cron: string, count: number = 3): string[] {
  const parts = cron.trim().split(/\s+/);
  if (parts.length !== 5) return [];

  const runs: string[] = [];
  const now = new Date();
  const current = new Date(now);
  current.setSeconds(0, 0);

  const [minExpr, hourExpr, domExpr, monthExpr, dowExpr] = parts;

  for (let i = 0; i < 1440 * 60 && runs.length < count; i++) {
    current.setMinutes(current.getMinutes() + 1);

    const min = current.getMinutes();
    const hour = current.getHours();
    const dom = current.getDate();
    const month = current.getMonth() + 1;
    const dow = current.getDay();

    if (
      matchesCronField(minExpr, min, 0, 59) &&
      matchesCronField(hourExpr, hour, 0, 23) &&
      matchesCronField(domExpr, dom, 1, 31) &&
      matchesCronField(monthExpr, month, 1, 12) &&
      matchesCronField(dowExpr, dow, 0, 6)
    ) {
      runs.push(current.toLocaleString());
    }
  }

  return runs;
}

function matchesCronField(expr: string, value: number, min: number, max: number): boolean {
  if (expr === '*') return true;

  return expr.split(',').some((part) => {
    if (part.includes('/')) {
      const [range, step] = part.split('/');
      const stepNum = parseInt(step, 10);
      const start = range === '*' ? min : parseInt(range, 10);
      if (isNaN(stepNum) || isNaN(start)) return false;
      return value >= start && (value - start) % stepNum === 0;
    }
    if (part.includes('-')) {
      const [from, to] = part.split('-').map(Number);
      return value >= from && value <= to;
    }
    return parseInt(part, 10) === value;
  });
}

function validateCron(cron: string): string | null {
  const parts = cron.trim().split(/\s+/);
  if (parts.length !== 5) {
    return `Expected 5 fields, got ${parts.length}. Format: minute hour day-of-month month day-of-week`;
  }

  const ranges: [number, number][] = [[0, 59], [0, 23], [1, 31], [1, 12], [0, 6]];

  for (let i = 0; i < 5; i++) {
    const part = parts[i];
    if (part === '*') continue;

    const segments = part.split(',');
    for (const seg of segments) {
      const base = seg.split('/')[0];
      if (base === '*') continue;

      const vals = base.split('-').map(Number);
      for (const v of vals) {
        if (isNaN(v) || v < ranges[i][0] || v > ranges[i][1]) {
          return `Invalid ${CRON_PARTS[i]} value: "${seg}" (range: ${ranges[i][0]}-${ranges[i][1]})`;
        }
      }
    }
  }

  return null;
}

export function CronHelper({ value, onChange }: CronHelperProps) {
  const [error, setError] = useState<string | null>(null);
  const [nextRuns, setNextRuns] = useState<string[]>([]);
  const [readable, setReadable] = useState('');

  useEffect(() => {
    if (!value) {
      setError(null);
      setNextRuns([]);
      setReadable('');
      return;
    }

    const validationError = validateCron(value);
    setError(validationError);

    if (!validationError) {
      setNextRuns(getNextRuns(value));
      setReadable(parseCronToReadable(value));
    } else {
      setNextRuns([]);
      setReadable('');
    }
  }, [value]);

  return (
    <div className="space-y-3">
      <div>
        <span className="text-xs font-medium text-gray-500">Presets</span>
        <div className="flex flex-wrap gap-1.5 mt-1">
          {CRON_PRESETS.map((preset) => (
            <button
              key={preset.cron}
              type="button"
              onClick={() => onChange(preset.cron)}
              className={`px-2.5 py-1 text-xs rounded-full border transition-colors ${
                value === preset.cron
                  ? 'bg-blue-100 border-blue-300 text-blue-800'
                  : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'
              }`}
              title={preset.description}
            >
              {preset.label}
            </button>
          ))}
        </div>
      </div>

      {value && (
        <div className="text-xs space-y-2">
          {error ? (
            <p className="text-red-600">{error}</p>
          ) : (
            <>
              <p className="text-gray-700">
                <span className="font-medium">Reads as:</span> {readable}
              </p>
              {nextRuns.length > 0 && (
                <div>
                  <span className="font-medium text-gray-500">Next runs:</span>
                  <ul className="mt-0.5 space-y-0.5">
                    {nextRuns.map((run, i) => (
                      <li key={i} className="text-gray-600 pl-2">
                        {run}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </>
          )}
        </div>
      )}

      <div className="text-xs text-gray-400">
        Format: minute(0-59) hour(0-23) day(1-31) month(1-12) weekday(0-6)
      </div>
    </div>
  );
}
