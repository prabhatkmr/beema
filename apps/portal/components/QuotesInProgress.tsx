'use client';

import { useState, useEffect, useRef } from 'react';
import { FileText, ChevronDown, Trash2 } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';

export interface QuoteInProgress {
  id: string;
  title: string;
  product?: string;
  insuredName?: string;
  createdAt: Date;
  updatedAt: Date;
  data: Record<string, any>;
}

interface QuotesInProgressProps {
  onResumeQuote?: (quote: QuoteInProgress) => void;
}

export function QuotesInProgress({ onResumeQuote }: QuotesInProgressProps) {
  const t = useTranslations('quotesInProgress');
  const [isOpen, setIsOpen] = useState(false);
  const [quotes, setQuotes] = useState<QuoteInProgress[]>([]);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Load quotes from localStorage
    const loadQuotes = () => {
      try {
        const stored = localStorage.getItem('quotesInProgress');
        if (stored) {
          const parsed = JSON.parse(stored);
          // Convert date strings back to Date objects
          const quotesWithDates = parsed.map((q: any) => ({
            ...q,
            createdAt: new Date(q.createdAt),
            updatedAt: new Date(q.updatedAt),
          }));
          setQuotes(quotesWithDates);
        }
      } catch (error) {
        console.error('Failed to load quotes:', error);
      }
    };

    loadQuotes();

    // Listen for storage events from other tabs/windows
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === 'quotesInProgress') {
        loadQuotes();
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const deleteQuote = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const updatedQuotes = quotes.filter(q => q.id !== id);
    setQuotes(updatedQuotes);
    localStorage.setItem('quotesInProgress', JSON.stringify(updatedQuotes));
  };

  const handleResumeQuote = (quote: QuoteInProgress) => {
    setIsOpen(false);
    onResumeQuote?.(quote);
  };

  const formatDate = (date: Date) => {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays}d ago`;

    return date.toLocaleDateString();
  };

  return (
    <div ref={dropdownRef} className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={cn(
          "flex items-center gap-2 px-3 py-1.5 text-sm border rounded-md bg-background hover:bg-muted/50 focus:ring-2 focus:ring-blue-500 focus:outline-none transition-colors",
          quotes.length > 0 && "border-blue-500"
        )}
        aria-label={t('ariaLabel')}
        aria-expanded={isOpen}
      >
        <FileText className="h-4 w-4" />
        <span>{t('button')}</span>
        {quotes.length > 0 && (
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-blue-500 text-xs font-semibold text-white">
            {quotes.length}
          </span>
        )}
        <ChevronDown className={cn("h-4 w-4 transition-transform", isOpen && "rotate-180")} />
      </button>

      {isOpen && (
        <div className="absolute right-0 z-50 mt-2 w-80 bg-background border rounded-md shadow-lg">
          <div className="border-b px-4 py-3">
            <h3 className="font-semibold text-sm">{t('title')}</h3>
            <p className="text-xs text-muted-foreground mt-0.5">
              {quotes.length === 0
                ? t('noSavedQuotes')
                : t('quoteSaved', { count: quotes.length })}
            </p>
          </div>

          <div className="max-h-96 overflow-y-auto">
            {quotes.length === 0 ? (
              <div className="px-4 py-8 text-center text-sm text-muted-foreground">
                <FileText className="mx-auto h-8 w-8 mb-2 opacity-50" />
                <p>{t('empty')}</p>
                <p className="text-xs mt-1">{t('emptyHint')}</p>
              </div>
            ) : (
              <div className="p-2">
                {quotes.map((quote) => (
                  <button
                    key={quote.id}
                    onClick={() => handleResumeQuote(quote)}
                    className="w-full text-left px-3 py-2 rounded hover:bg-muted transition-colors group"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium truncate">
                          {quote.title || t('untitled')}
                        </p>
                        {quote.product && (
                          <p className="text-xs text-muted-foreground truncate">
                            {quote.product}
                          </p>
                        )}
                        {quote.insuredName && (
                          <p className="text-xs text-muted-foreground truncate">
                            {quote.insuredName}
                          </p>
                        )}
                        <p className="text-xs text-muted-foreground/70 mt-1">
                          {formatDate(quote.updatedAt)}
                        </p>
                      </div>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e) => deleteQuote(quote.id, e)}
                        className="opacity-0 group-hover:opacity-100 h-7 w-7 p-0"
                        aria-label={t('deleteQuote')}
                      >
                        <Trash2 className="h-3.5 w-3.5 text-destructive" />
                      </Button>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

// Helper function to save a quote (to be used by the quote form)
export function saveQuoteInProgress(quote: Omit<QuoteInProgress, 'createdAt' | 'updatedAt'>) {
  try {
    const stored = localStorage.getItem('quotesInProgress');
    const quotes: QuoteInProgress[] = stored ? JSON.parse(stored) : [];

    const existingIndex = quotes.findIndex(q => q.id === quote.id);
    const now = new Date();

    if (existingIndex >= 0) {
      // Update existing quote
      quotes[existingIndex] = {
        ...quote,
        createdAt: quotes[existingIndex].createdAt,
        updatedAt: now,
      };
    } else {
      // Add new quote
      quotes.push({
        ...quote,
        createdAt: now,
        updatedAt: now,
      });
    }

    localStorage.setItem('quotesInProgress', JSON.stringify(quotes));

    // Dispatch custom event to notify other components
    window.dispatchEvent(new StorageEvent('storage', {
      key: 'quotesInProgress',
      newValue: JSON.stringify(quotes),
    }));
  } catch (error) {
    console.error('Failed to save quote:', error);
  }
}

// Helper function to remove a quote after submission
export function removeQuoteInProgress(id: string) {
  try {
    const stored = localStorage.getItem('quotesInProgress');
    if (stored) {
      const quotes: QuoteInProgress[] = JSON.parse(stored);
      const filtered = quotes.filter(q => q.id !== id);
      localStorage.setItem('quotesInProgress', JSON.stringify(filtered));

      window.dispatchEvent(new StorageEvent('storage', {
        key: 'quotesInProgress',
        newValue: JSON.stringify(filtered),
      }));
    }
  } catch (error) {
    console.error('Failed to remove quote:', error);
  }
}
