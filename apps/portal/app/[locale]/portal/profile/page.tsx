'use client';

import { useRouter } from "next/navigation";
import { UserCircle } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function ProfilePage() {
  const router = useRouter();

  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] space-y-6">
      <div className="text-center space-y-4">
        <div className="flex justify-center">
          <div className="p-6 bg-gradient-to-br from-slate-500 to-slate-600 rounded-2xl shadow-lg">
            <UserCircle className="h-16 w-16 text-white" />
          </div>
        </div>
        <h1 className="text-4xl font-bold text-gray-900">Profile</h1>
        <p className="text-lg text-muted-foreground max-w-md">
          Manage your account settings and preferences
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
