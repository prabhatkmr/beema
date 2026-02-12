import React from 'react';
import { BlueprintCanvas } from '../components/BlueprintEditor/BlueprintCanvas';
import { useBlueprintStore } from '../stores/blueprintStore';
import { useSourceSchemaQuery, useTargetSchemaQuery } from '../hooks/useBlueprintQuery';
import { Spinner } from '../components/ui/Spinner';

export const BlueprintEditorPage: React.FC = () => {
  const currentBlueprint = useBlueprintStore((state) => state.currentBlueprint);

  const { data: sourceSchema, isLoading: isLoadingSource } = useSourceSchemaQuery(
    currentBlueprint?.sourceSystem || null
  );

  const { data: targetSchema, isLoading: isLoadingTarget } = useTargetSchemaQuery(
    currentBlueprint?.targetSchema || null
  );

  const isLoading = isLoadingSource || isLoadingTarget;

  // Mock data for development (remove when API is ready)
  const mockSourceFields = [
    {
      name: 'policyNumber',
      path: 'policy.number',
      type: 'string',
      description: 'Unique policy identifier',
      example: 'POL-12345',
    },
    {
      name: 'premium',
      path: 'policy.premium',
      type: 'number',
      description: 'Policy premium amount',
      example: 1500.0,
    },
    {
      name: 'firstName',
      path: 'insured.firstName',
      type: 'string',
      description: 'Insured person first name',
      example: 'John',
    },
    {
      name: 'lastName',
      path: 'insured.lastName',
      type: 'string',
      description: 'Insured person last name',
      example: 'Doe',
    },
    {
      name: 'effectiveDate',
      path: 'policy.effectiveDate',
      type: 'date',
      description: 'Policy effective date',
      example: '2024-01-01',
    },
  ];

  const mockTargetFields = [
    {
      name: 'policyId',
      path: 'policyId',
      type: 'string',
      required: true,
      description: 'Internal policy ID',
    },
    {
      name: 'premiumAmount',
      path: 'premium.amount',
      type: 'number',
      required: true,
      description: 'Premium amount in cents',
    },
    {
      name: 'insuredName',
      path: 'insured.fullName',
      type: 'string',
      required: true,
      description: 'Full name of insured',
    },
    {
      name: 'validFrom',
      path: 'validity.from',
      type: 'datetime',
      required: true,
      description: 'Policy validity start date',
    },
    {
      name: 'validTo',
      path: 'validity.to',
      type: 'datetime',
      required: false,
      description: 'Policy validity end date',
    },
  ];

  const sourceFields = sourceSchema?.fields || mockSourceFields;
  const targetFields = targetSchema?.fields || mockTargetFields;

  if (isLoading && currentBlueprint) {
    return (
      <div className="flex items-center justify-center h-full">
        <Spinner size="lg" />
      </div>
    );
  }

  return <BlueprintCanvas sourceFields={sourceFields} targetFields={targetFields} />;
};
