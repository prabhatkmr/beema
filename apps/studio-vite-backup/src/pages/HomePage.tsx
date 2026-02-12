import React from 'react';
import { Link } from 'react-router-dom';
import { Layout, FileCode } from 'lucide-react';

export const HomePage: React.FC = () => {
  return (
    <div className="h-full flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-50">
      <div className="max-w-4xl mx-auto px-6 text-center">
        <h1 className="text-5xl font-bold text-gray-900 mb-4">Beema Studio</h1>
        <p className="text-xl text-gray-600 mb-12">
          Visual tools for building insurance data transformations and UI layouts
        </p>

        <div className="grid md:grid-cols-2 gap-8">
          <Link
            to="/blueprints"
            className="group bg-white p-8 rounded-xl shadow-lg hover:shadow-xl transition-all border-2 border-transparent hover:border-blue-500"
          >
            <div className="w-16 h-16 bg-blue-100 rounded-lg flex items-center justify-center mx-auto mb-4 group-hover:bg-blue-500 transition-colors">
              <FileCode className="w-8 h-8 text-blue-600 group-hover:text-white transition-colors" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Blueprint Editor</h2>
            <p className="text-gray-600">
              Design message mapping blueprints with visual field connections and JEXL expressions
            </p>
          </Link>

          <Link
            to="/layout-builder"
            className="group bg-white p-8 rounded-xl shadow-lg hover:shadow-xl transition-all border-2 border-transparent hover:border-indigo-500"
          >
            <div className="w-16 h-16 bg-indigo-100 rounded-lg flex items-center justify-center mx-auto mb-4 group-hover:bg-indigo-500 transition-colors">
              <Layout className="w-8 h-8 text-indigo-600 group-hover:text-white transition-colors" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Layout Builder</h2>
            <p className="text-gray-600">
              Create dynamic form layouts with drag-and-drop field components and validation rules
            </p>
          </Link>
        </div>

        <div className="mt-12 pt-8 border-t border-gray-200">
          <p className="text-sm text-gray-500">
            Part of the Beema Unified Platform - Metadata-driven insurance software
          </p>
        </div>
      </div>
    </div>
  );
};
