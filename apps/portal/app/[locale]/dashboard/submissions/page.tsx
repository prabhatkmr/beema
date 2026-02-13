import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';

export default function SubmissionsPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Back Button */}
        <Link
          href="/dashboard"
          className="inline-flex items-center text-blue-600 hover:text-blue-800 mb-6"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to Dashboard
        </Link>

        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Submissions</h1>
          <p className="text-gray-600">New policy submissions and quotes</p>
        </div>

        {/* Content */}
        <div className="bg-white rounded-lg shadow p-8">
          <p className="text-gray-600">Submission management coming soon...</p>
        </div>
      </div>
    </div>
  );
}
