'use client';

import { useRouter } from "next/navigation";
import { BarChart3 } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function AnalyticsPage() {
  const router = useRouter();

  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] space-y-6">
      <div className="text-center space-y-4">
        <div className="flex justify-center">
          <div className="p-6 bg-gradient-to-br from-indigo-500 to-indigo-600 rounded-2xl shadow-lg">
            <BarChart3 className="h-16 w-16 text-white" />
          </div>
        </div>
        <h1 className="text-4xl font-bold text-gray-900">Analytics</h1>
        <p className="text-lg text-muted-foreground max-w-md">
          Insights and reports on your insurance portfolio performance
        </p>
        <p className="text-sm text-muted-foreground">
          Coming soon...
        </p>
      </div>
      <Button onClick={() => router.push('/portal/dashboard')} variant="outline">
        Back to Dashboard
      </Button>
    </div>
  );
}
