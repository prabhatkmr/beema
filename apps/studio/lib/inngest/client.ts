import { Inngest } from 'inngest';

// Define event types
export type Events = {
  'policy/bound': {
    data: {
      policyNumber: string;
      agreementId: string;
      marketContext: string;
      premium?: number;
      productType?: string;
      effectiveDate?: string;
      tenantId?: string;
    };
    user: {
      id: string;
      email: string;
    };
  };
  'claim/opened': {
    data: {
      claimNumber: string;
      claimId: string;
      claimAmount: number;
      claimType: string;
      policyNumber?: string;
      tenantId?: string;
    };
    user: {
      id: string;
      email: string;
    };
  };
  'agreement/updated': {
    data: {
      agreementId: string;
      changeType: string;
      changes: Record<string, any>;
      tenantId?: string;
    };
    user: {
      id: string;
      email: string;
    };
  };
  'claim/settled': {
    data: {
      claimNumber: string;
      settlementAmount: number;
      settlementType: string;
      tenantId?: string;
    };
    user: {
      id: string;
      email: string;
    };
  };
};

// Create Inngest client
export const inngest = new Inngest<Events>({
  id: 'beema-studio',
  eventKey: process.env.INNGEST_EVENT_KEY || 'local'
});
