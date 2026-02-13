'use client';

import { useRouter } from "next/navigation";
import { ClipboardCheck, FileCheck, Shield, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function UnderwritingPage() {
  const router = useRouter();

  return (
    <div className="space-y-10">
      <div>
        <h2 className="text-2xl font-bold tracking-tight">
          Beema Underwriting Workbench
        </h2>
        <p className="text-sm text-muted-foreground">
          Comprehensive risk evaluation and policy decisioning
        </p>
      </div>

      {/* Hero Section */}
      <div className="bg-gradient-to-br from-amber-500 to-amber-600 rounded-2xl p-8 text-white shadow-xl">
        <div className="flex items-center gap-4 mb-4">
          <div className="p-4 bg-white/20 rounded-xl backdrop-blur-sm">
            <ClipboardCheck className="h-12 w-12" />
          </div>
          <div>
            <h3 className="text-3xl font-bold">Underwriting Workbench</h3>
            <p className="text-amber-100">Intelligent Risk Assessment Platform</p>
          </div>
        </div>
        <p className="text-lg text-amber-50 max-w-2xl">
          Streamline your underwriting process with AI-powered risk analysis, automated
          referrals, and comprehensive decision support tools for accurate policy evaluation.
        </p>
      </div>

      {/* Features Grid */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-6">Core Capabilities</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-green-50 rounded-lg">
                <FileCheck className="h-6 w-6 text-green-600" />
              </div>
            </div>
            <h4 className="text-lg font-semibold text-gray-900 mb-2">Automated Risk Assessment</h4>
            <p className="text-sm text-gray-600">
              AI-driven risk scoring and classification with real-time data validation and fraud detection
            </p>
          </div>

          <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-blue-50 rounded-lg">
                <Shield className="h-6 w-6 text-blue-600" />
              </div>
            </div>
            <h4 className="text-lg font-semibold text-gray-900 mb-2">Policy Decisioning</h4>
            <p className="text-sm text-gray-600">
              Comprehensive workflows for accept, decline, or refer decisions with audit trails
            </p>
          </div>

          <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-red-50 rounded-lg">
                <AlertCircle className="h-6 w-6 text-red-600" />
              </div>
            </div>
            <h4 className="text-lg font-semibold text-gray-900 mb-2">Referral Management</h4>
            <p className="text-sm text-gray-600">
              Smart routing and queue management for cases requiring senior underwriter review
            </p>
          </div>
        </div>
      </div>

      {/* Coming Soon Section */}
      <div className="bg-gray-50 rounded-lg border border-gray-200 p-8 text-center">
        <p className="text-lg font-medium text-gray-900 mb-2">
          Full underwriting workbench launching soon
        </p>
        <p className="text-sm text-muted-foreground">
          Empowering underwriters with cutting-edge tools for faster, more accurate decisions
        </p>
      </div>

      {/* Back to Dashboard Button */}
      <div className="flex justify-center">
        <Button onClick={() => router.push('/portal/dashboard')} variant="outline">
          Back to Dashboard
        </Button>
      </div>
    </div>
  );
}
