'use client';

import { useRouter, usePathname } from 'next/navigation';
import { QuotesInProgress, type QuoteInProgress } from './QuotesInProgress';
import { DraftClaims, type DraftClaim } from './DraftClaims';

export function QuotesInProgressWrapper() {
  const router = useRouter();
  const pathname = usePathname();

  const handleResumeQuote = (quote: QuoteInProgress) => {
    // Extract locale from pathname
    const segments = pathname.split('/');
    const locale = segments[1];

    // Navigate to policy page with quote data in URL state
    const policyPath = `/${locale}/portal/policy`;

    // Store the quote data temporarily for the policy page to pick up
    sessionStorage.setItem('resumeQuote', JSON.stringify(quote));

    // Navigate to policy page
    router.push(policyPath);
  };

  const handleResumeClaim = (claim: DraftClaim) => {
    // Extract locale from pathname
    const segments = pathname.split('/');
    const locale = segments[1];

    // Navigate to claims page with claim data in URL state
    const claimsPath = `/${locale}/portal/claims`;

    // Store the claim data temporarily for the claims page to pick up
    sessionStorage.setItem('resumeClaim', JSON.stringify(claim));

    // Navigate to claims page
    router.push(claimsPath);
  };

  // Show appropriate dropdown based on current page
  const isPolicyPage = pathname.includes('/portal/policy');
  const isClaimsPage = pathname.includes('/portal/claims');

  if (isPolicyPage) {
    return <QuotesInProgress onResumeQuote={handleResumeQuote} />;
  }

  if (isClaimsPage) {
    return <DraftClaims onResumeClaim={handleResumeClaim} />;
  }

  // Don't show anything on other pages (billing, etc.)
  return null;
}
