import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../services/api';
import { MessageBlueprint } from '../types/blueprint';
import { useBlueprintStore } from '../stores/blueprintStore';
import toast from 'react-hot-toast';

export const QUERY_KEYS = {
  blueprints: ['blueprints'] as const,
  blueprint: (id: string) => ['blueprints', id] as const,
  sourceSystems: ['sourceSystems'] as const,
  targetSchemas: ['targetSchemas'] as const,
  sourceSchema: (system: string) => ['sourceSchema', system] as const,
  targetSchema: (schema: string) => ['targetSchema', schema] as const,
};

export const useBlueprintsQuery = () => {
  return useQuery({
    queryKey: QUERY_KEYS.blueprints,
    queryFn: () => apiClient.getBlueprints(),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

export const useBlueprintQuery = (id: string | null) => {
  return useQuery({
    queryKey: QUERY_KEYS.blueprint(id || ''),
    queryFn: () => apiClient.getBlueprintById(id!),
    enabled: !!id,
  });
};

export const useCreateBlueprintMutation = () => {
  const queryClient = useQueryClient();
  const setCurrentBlueprint = useBlueprintStore((state) => state.setCurrentBlueprint);

  return useMutation({
    mutationFn: (blueprint: Omit<MessageBlueprint, 'id' | 'createdAt' | 'updatedAt'>) =>
      apiClient.createBlueprint(blueprint),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.blueprints });
      setCurrentBlueprint(data);
      toast.success('Blueprint created successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create blueprint');
    },
  });
};

export const useUpdateBlueprintMutation = () => {
  const queryClient = useQueryClient();
  const markAsSaved = useBlueprintStore((state) => state.markAsSaved);

  return useMutation({
    mutationFn: ({ id, updates }: { id: string; updates: Partial<MessageBlueprint> }) =>
      apiClient.updateBlueprint(id, updates),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.blueprints });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.blueprint(variables.id) });
      markAsSaved();
      toast.success('Blueprint saved successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to save blueprint');
    },
  });
};

export const useDeleteBlueprintMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => apiClient.deleteBlueprint(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.blueprints });
      toast.success('Blueprint deleted successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to delete blueprint');
    },
  });
};

export const useCloneBlueprintMutation = () => {
  const queryClient = useQueryClient();
  const setCurrentBlueprint = useBlueprintStore((state) => state.setCurrentBlueprint);

  return useMutation({
    mutationFn: ({ id, newName }: { id: string; newName: string }) =>
      apiClient.cloneBlueprint(id, newName),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.blueprints });
      setCurrentBlueprint(data);
      toast.success('Blueprint cloned successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to clone blueprint');
    },
  });
};

export const useValidateBlueprintMutation = () => {
  return useMutation({
    mutationFn: (id: string) => apiClient.validateBlueprint(id),
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Validation failed');
    },
  });
};

export const useTestBlueprintMutation = () => {
  return useMutation({
    mutationFn: ({ id, testData }: { id: string; testData: Record<string, any> }) =>
      apiClient.testBlueprint(id, testData),
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Test execution failed');
    },
  });
};

export const useValidateJexlMutation = () => {
  return useMutation({
    mutationFn: ({ expression, context }: { expression: string; context: Record<string, any> }) =>
      apiClient.validateJexlExpression(expression, context),
  });
};

export const useSourceSystemsQuery = () => {
  return useQuery({
    queryKey: QUERY_KEYS.sourceSystems,
    queryFn: () => apiClient.getSourceSystems(),
    staleTime: 30 * 60 * 1000, // 30 minutes
  });
};

export const useTargetSchemasQuery = () => {
  return useQuery({
    queryKey: QUERY_KEYS.targetSchemas,
    queryFn: () => apiClient.getTargetSchemas(),
    staleTime: 30 * 60 * 1000, // 30 minutes
  });
};

export const useSourceSchemaQuery = (sourceSystem: string | null) => {
  return useQuery({
    queryKey: QUERY_KEYS.sourceSchema(sourceSystem || ''),
    queryFn: () => apiClient.getSourceSchema(sourceSystem!),
    enabled: !!sourceSystem,
    staleTime: 30 * 60 * 1000,
  });
};

export const useTargetSchemaQuery = (targetSchema: string | null) => {
  return useQuery({
    queryKey: QUERY_KEYS.targetSchema(targetSchema || ''),
    queryFn: () => apiClient.getTargetSchema(targetSchema!),
    enabled: !!targetSchema,
    staleTime: 30 * 60 * 1000,
  });
};
