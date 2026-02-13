'use client';

import { useRouter, usePathname } from 'next/navigation';
import { QuotesInProgress, type QuoteInProgress } from './QuotesInProgress';

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

  return <QuotesInProgress onResumeQuote={handleResumeQuote} />;
}
