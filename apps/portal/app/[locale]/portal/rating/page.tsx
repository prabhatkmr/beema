'use client';

import { useRouter } from "next/navigation";
import { useTranslations } from 'next-intl';
import { Calculator, TrendingUp, Zap, Shield } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function RatingPage() {
  const router = useRouter();
  const t = useTranslations('rating');
  const tc = useTranslations('common');

  return (
    <div className="space-y-10">
      <div>
        <h2 className="text-2xl font-bold tracking-tight">
          {t('title')}
        </h2>
        <p className="text-sm text-muted-foreground">
          {t('subtitle')}
        </p>
      </div>

      {/* Hero Section */}
      <div className="bg-gradient-to-br from-teal-500 to-teal-600 rounded-2xl p-8 text-white shadow-xl">
        <div className="flex items-center gap-4 mb-4">
          <div className="p-4 bg-white/20 rounded-xl backdrop-blur-sm">
            <Calculator className="h-12 w-12" />
          </div>
          <div>
            <h3 className="text-3xl font-bold">{t('heroTitle')}</h3>
            <p className="text-teal-100">{t('heroSubtitle')}</p>
          </div>
        </div>
        <p className="text-lg text-teal-50 max-w-2xl">
          {t('heroDescription')}
        </p>
      </div>

      {/* Features Grid */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-6">{t('keyFeatures')}</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-blue-50 rounded-lg">
                <TrendingUp className="h-6 w-6 text-blue-600" />
              </div>
            </div>
            <h4 className="text-lg font-semibold text-gray-900 mb-2">{t('dynamicPricing')}</h4>
            <p className="text-sm text-gray-600">
              {t('dynamicPricingDesc')}
            </p>
          </div>

          <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-purple-50 rounded-lg">
                <Zap className="h-6 w-6 text-purple-600" />
              </div>
            </div>
            <h4 className="text-lg font-semibold text-gray-900 mb-2">{t('realTimeCalc')}</h4>
            <p className="text-sm text-gray-600">
              {t('realTimeCalcDesc')}
            </p>
          </div>

          <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-green-50 rounded-lg">
                <Shield className="h-6 w-6 text-green-600" />
              </div>
            </div>
            <h4 className="text-lg font-semibold text-gray-900 mb-2">{t('riskAssessment')}</h4>
            <p className="text-sm text-gray-600">
              {t('riskAssessmentDesc')}
            </p>
          </div>
        </div>
      </div>

      {/* Coming Soon Section */}
      <div className="bg-gray-50 rounded-lg border border-gray-200 p-8 text-center">
        <p className="text-lg font-medium text-gray-900 mb-2">
          {t('comingSoonTitle')}
        </p>
        <p className="text-sm text-muted-foreground">
          {t('comingSoonDesc')}
        </p>
      </div>

      {/* Back to Dashboard Button */}
      <div className="flex justify-center">
        <Button onClick={() => router.push('/portal/dashboard')} variant="outline">
          {tc('backToDashboard')}
        </Button>
      </div>
    </div>
  );
}
