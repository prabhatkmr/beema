'use client';

import { useRouter } from "next/navigation";
import { useTranslations } from 'next-intl';
import { BarChart3 } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function AnalyticsPage() {
  const router = useRouter();
  const t = useTranslations('placeholder.analytics');
  const tc = useTranslations('common');

  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] space-y-6">
      <div className="text-center space-y-4">
        <div className="flex justify-center">
          <div className="p-6 bg-gradient-to-br from-indigo-500 to-indigo-600 rounded-2xl shadow-lg">
            <BarChart3 className="h-16 w-16 text-white" />
          </div>
        </div>
        <h2 className="text-4xl font-bold text-gray-900">{t('title')}</h2>
        <p className="text-lg text-muted-foreground max-w-md">
          {t('description')}
        </p>
        <p className="text-sm text-muted-foreground">
          {tc('comingSoon')}
        </p>
      </div>
      <Button onClick={() => router.push('/portal/dashboard')} variant="outline">
        {tc('backToDashboard')}
      </Button>
    </div>
  );
}
