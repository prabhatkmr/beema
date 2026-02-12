import React from 'react';
import { useLocation, Link } from 'react-router-dom';
import { Save, Play, FileText, Settings, AlertCircle, Home, Layout } from 'lucide-react';
import { Button } from '../ui/Button';
import { useBlueprintStore } from '../../stores/blueprintStore';
import { useUpdateBlueprintMutation, useValidateBlueprintMutation } from '../../hooks/useBlueprintQuery';

export const Header: React.FC = () => {
  const location = useLocation();
  const { currentBlueprint, isDirty, setTestPanelOpen } = useBlueprintStore();
  const updateMutation = useUpdateBlueprintMutation();
  const validateMutation = useValidateBlueprintMutation();

  const isBlueprintPage = location.pathname === '/blueprints';
  const isLayoutBuilderPage = location.pathname === '/layout-builder';

  const handleSave = () => {
    if (currentBlueprint) {
      updateMutation.mutate({
        id: currentBlueprint.id,
        updates: currentBlueprint,
      });
    }
  };

  const handleValidate = async () => {
    if (currentBlueprint) {
      const result = await validateMutation.mutateAsync(currentBlueprint.id);
      if (result.isValid) {
        alert('Blueprint is valid!');
      } else {
        alert(`Validation failed:\n${result.errors.map((e) => e.message).join('\n')}`);
      }
    }
  };

  const handleTest = () => {
    setTestPanelOpen(true);
  };

  return (
    <header className="bg-white border-b border-gray-200 px-6 py-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link to="/" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
            <FileText className="text-primary-600" size={28} />
            <h1 className="text-2xl font-bold text-gray-900">Beema Studio</h1>
          </Link>

          <nav className="flex items-center gap-2 ml-4">
            <Link
              to="/"
              className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                location.pathname === '/'
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              <Home size={16} className="inline mr-1" />
              Home
            </Link>
            <Link
              to="/blueprints"
              className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                isBlueprintPage
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              <FileText size={16} className="inline mr-1" />
              Blueprints
            </Link>
            <Link
              to="/layout-builder"
              className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                isLayoutBuilderPage
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              <Layout size={16} className="inline mr-1" />
              Layout Builder
            </Link>
          </nav>

          {currentBlueprint && isBlueprintPage && (
            <div className="flex items-center gap-2 px-4 py-2 bg-gray-50 rounded-lg">
              <span className="text-sm text-gray-600">Blueprint:</span>
              <span className="font-semibold text-gray-900">{currentBlueprint.name}</span>
              {isDirty && (
                <span className="inline-flex items-center gap-1 text-xs text-orange-600">
                  <AlertCircle size={14} />
                  Unsaved changes
                </span>
              )}
            </div>
          )}
        </div>

        <div className="flex items-center gap-3">
          {currentBlueprint && isBlueprintPage && (
            <>
              <Button
                variant="ghost"
                size="sm"
                onClick={handleValidate}
                disabled={validateMutation.isPending}
              >
                <AlertCircle size={18} className="mr-2" />
                Validate
              </Button>
              <Button
                variant="secondary"
                size="sm"
                onClick={handleTest}
              >
                <Play size={18} className="mr-2" />
                Test
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleSave}
                disabled={!isDirty || updateMutation.isPending}
              >
                <Save size={18} className="mr-2" />
                {updateMutation.isPending ? 'Saving...' : 'Save'}
              </Button>
            </>
          )}
          <Button variant="ghost" size="sm">
            <Settings size={18} />
          </Button>
        </div>
      </div>
    </header>
  );
};
