export type SubmissionStatus = 'DRAFT' | 'QUOTED' | 'BOUND' | 'DECLINED';

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
