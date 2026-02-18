export type SubmissionStatus = 'DRAFT' | 'QUOTED' | 'BOUND' | 'ISSUED' | 'DECLINED';

export interface Submission {
  submissionId: string;
  product: string;
  status: SubmissionStatus;
  formData: Record<string, any>;
  ratingResult?: {
    premium: number;
    tax: number;
    total: number;
  };
  createdAt: string;
}

export interface WorkflowEvent {
  eventId: number;
  eventType: string;
  timestamp: string;
  detail: string;
}

export interface WorkflowStatus {
  workflowId: string;
  runId: string;
  status: string;
  startTime: string;
  closeTime: string | null;
  taskQueue: string;
  events: WorkflowEvent[];
}
