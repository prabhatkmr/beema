'use client';

import { useState, useEffect, useRef } from 'react';
import { AlertCircle, ChevronDown, Trash2 } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';

export interface DraftClaim {
  id: string;
  title: string;
  claimType?: string;
  insuredName?: string;
  createdAt: Date;
  updatedAt: Date;
  data: Record<string, any>;
}

interface DraftClaimsProps {
  onResumeClaim?: (claim: DraftClaim) => void;
}

export function DraftClaims({ onResumeClaim }: DraftClaimsProps) {
  const t = useTranslations('draftClaims');
  const [isOpen, setIsOpen] = useState(false);
  const [claims, setClaims] = useState<DraftClaim[]>([]);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Load claims from localStorage
    const loadClaims = () => {
      try {
        const stored = localStorage.getItem('draftClaims');
        if (stored) {
          const parsed = JSON.parse(stored);
          // Convert date strings back to Date objects
          const claimsWithDates = parsed.map((c: any) => ({
            ...c,
            createdAt: new Date(c.createdAt),
            updatedAt: new Date(c.updatedAt),
          }));
          setClaims(claimsWithDates);
        }
      } catch (error) {
        console.error('Failed to load draft claims:', error);
      }
    };

    loadClaims();

    // Listen for storage events from other tabs/windows
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === 'draftClaims') {
        loadClaims();
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

  const deleteClaim = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const updatedClaims = claims.filter(c => c.id !== id);
    setClaims(updatedClaims);
    localStorage.setItem('draftClaims', JSON.stringify(updatedClaims));
  };

  const handleResumeClaim = (claim: DraftClaim) => {
    setIsOpen(false);
    onResumeClaim?.(claim);
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
          claims.length > 0 && "border-red-500"
        )}
        aria-label={t('ariaLabel')}
        aria-expanded={isOpen}
      >
        <AlertCircle className="h-4 w-4" />
        <span>{t('button')}</span>
        {claims.length > 0 && (
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-xs font-semibold text-white">
            {claims.length}
          </span>
        )}
        <ChevronDown className={cn("h-4 w-4 transition-transform", isOpen && "rotate-180")} />
      </button>

      {isOpen && (
        <div className="absolute right-0 z-50 mt-2 w-80 bg-background border rounded-md shadow-lg">
          <div className="border-b px-4 py-3">
            <h3 className="font-semibold text-sm">{t('title')}</h3>
            <p className="text-xs text-muted-foreground mt-0.5">
              {claims.length === 0
                ? t('noSavedClaims')
                : t('claimSaved', { count: claims.length })}
            </p>
          </div>

          <div className="max-h-96 overflow-y-auto">
            {claims.length === 0 ? (
              <div className="px-4 py-8 text-center text-sm text-muted-foreground">
                <AlertCircle className="mx-auto h-8 w-8 mb-2 opacity-50" />
                <p>{t('empty')}</p>
                <p className="text-xs mt-1">{t('emptyHint')}</p>
              </div>
            ) : (
              <div className="p-2">
                {claims.map((claim) => (
                  <button
                    key={claim.id}
                    onClick={() => handleResumeClaim(claim)}
                    className="w-full text-left px-3 py-2 rounded hover:bg-muted transition-colors group"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium truncate">
                          {claim.title || t('untitled')}
                        </p>
                        {claim.claimType && (
                          <p className="text-xs text-muted-foreground truncate">
                            {claim.claimType}
                          </p>
                        )}
                        {claim.insuredName && (
                          <p className="text-xs text-muted-foreground truncate">
                            {claim.insuredName}
                          </p>
                        )}
                        <p className="text-xs text-muted-foreground/70 mt-1">
                          {formatDate(claim.updatedAt)}
                        </p>
                      </div>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e) => deleteClaim(claim.id, e)}
                        className="opacity-0 group-hover:opacity-100 h-7 w-7 p-0"
                        aria-label={t('deleteDraft')}
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

// Helper function to save a draft claim (to be used by the claim form)
export function saveDraftClaim(claim: Omit<DraftClaim, 'createdAt' | 'updatedAt'>) {
  try {
    const stored = localStorage.getItem('draftClaims');
    const claims: DraftClaim[] = stored ? JSON.parse(stored) : [];

    const existingIndex = claims.findIndex(c => c.id === claim.id);
    const now = new Date();

    if (existingIndex >= 0) {
      // Update existing claim
      claims[existingIndex] = {
        ...claim,
        createdAt: claims[existingIndex].createdAt,
        updatedAt: now,
      };
    } else {
      // Add new claim
      claims.push({
        ...claim,
        createdAt: now,
        updatedAt: now,
      });
    }

    localStorage.setItem('draftClaims', JSON.stringify(claims));

    // Dispatch custom event to notify other components
    window.dispatchEvent(new StorageEvent('storage', {
      key: 'draftClaims',
      newValue: JSON.stringify(claims),
    }));
  } catch (error) {
    console.error('Failed to save draft claim:', error);
  }
}

// Helper function to remove a draft claim after submission
export function removeDraftClaim(id: string) {
  try {
    const stored = localStorage.getItem('draftClaims');
    if (stored) {
      const claims: DraftClaim[] = JSON.parse(stored);
      const filtered = claims.filter(c => c.id !== id);
      localStorage.setItem('draftClaims', JSON.stringify(filtered));

      window.dispatchEvent(new StorageEvent('storage', {
        key: 'draftClaims',
        newValue: JSON.stringify(filtered),
      }));
    }
  } catch (error) {
    console.error('Failed to remove draft claim:', error);
  }
}
