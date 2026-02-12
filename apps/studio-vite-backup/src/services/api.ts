import axios, { AxiosInstance, AxiosError } from 'axios';
import { MessageBlueprint, BlueprintValidationResult, TestResult } from '../types/blueprint';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 30000,
    });

    // Request interceptor
    this.client.interceptors.request.use(
      (config) => {
        // Add auth token if available
        const token = localStorage.getItem('authToken');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        if (error.response?.status === 401) {
          // Handle unauthorized
          localStorage.removeItem('authToken');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  // Blueprint operations
  async getBlueprints(): Promise<MessageBlueprint[]> {
    const response = await this.client.get<MessageBlueprint[]>('/blueprints');
    return response.data;
  }

  async getBlueprintById(id: string): Promise<MessageBlueprint> {
    const response = await this.client.get<MessageBlueprint>(`/blueprints/${id}`);
    return response.data;
  }

  async createBlueprint(blueprint: Omit<MessageBlueprint, 'id' | 'createdAt' | 'updatedAt'>): Promise<MessageBlueprint> {
    const response = await this.client.post<MessageBlueprint>('/blueprints', blueprint);
    return response.data;
  }

  async updateBlueprint(id: string, blueprint: Partial<MessageBlueprint>): Promise<MessageBlueprint> {
    const response = await this.client.put<MessageBlueprint>(`/blueprints/${id}`, blueprint);
    return response.data;
  }

  async deleteBlueprint(id: string): Promise<void> {
    await this.client.delete(`/blueprints/${id}`);
  }

  async cloneBlueprint(id: string, newName: string): Promise<MessageBlueprint> {
    const response = await this.client.post<MessageBlueprint>(`/blueprints/${id}/clone`, {
      name: newName,
    });
    return response.data;
  }

  // Validation operations
  async validateBlueprint(id: string): Promise<BlueprintValidationResult> {
    const response = await this.client.post<BlueprintValidationResult>(
      `/blueprints/${id}/validate`
    );
    return response.data;
  }

  async validateJexlExpression(
    expression: string,
    context: Record<string, any>
  ): Promise<{ isValid: boolean; result?: any; error?: string }> {
    const response = await this.client.post('/jexl/validate', {
      expression,
      context,
    });
    return response.data;
  }

  // Test operations
  async testBlueprint(
    id: string,
    testData: Record<string, any>
  ): Promise<TestResult> {
    const response = await this.client.post<TestResult>(`/blueprints/${id}/test`, testData);
    return response.data;
  }

  // Schema operations
  async getSourceSchema(sourceSystem: string): Promise<any> {
    const response = await this.client.get(`/schemas/source/${sourceSystem}`);
    return response.data;
  }

  async getTargetSchema(schemaName: string): Promise<any> {
    const response = await this.client.get(`/schemas/target/${schemaName}`);
    return response.data;
  }

  // System info
  async getSourceSystems(): Promise<string[]> {
    const response = await this.client.get<string[]>('/systems/sources');
    return response.data;
  }

  async getTargetSchemas(): Promise<string[]> {
    const response = await this.client.get<string[]>('/schemas/targets');
    return response.data;
  }
}

export const apiClient = new ApiClient();
export default apiClient;
