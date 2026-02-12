import { useState } from 'react';
import { SysLayout } from '@/types/layout';

interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings?: string[];
}

export function useLayoutValidation() {
  const [isValidating, setIsValidating] = useState(false);
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);

  const validate = async (layout: SysLayout): Promise<ValidationResult> => {
    setIsValidating(true);

    try {
      const response = await fetch('/api/layouts/validate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(layout),
      });

      const result = await response.json();
      setValidationResult(result);
      return result;
    } catch (error) {
      const errorResult = {
        valid: false,
        errors: ['Failed to validate layout: ' + error],
      };
      setValidationResult(errorResult);
      return errorResult;
    } finally {
      setIsValidating(false);
    }
  };

  return { validate, isValidating, validationResult };
}
