import React, { useEffect } from 'react';
import { LayoutBuilder } from '../components/LayoutBuilder/LayoutBuilder';
import { useLayoutStore } from '../stores/layoutStore';

export const LayoutBuilderPage: React.FC = () => {
  const { currentLayout, createLayout } = useLayoutStore();

  useEffect(() => {
    // If no layout exists, create a default one
    if (!currentLayout) {
      createLayout('My First Layout');
    }
  }, [currentLayout, createLayout]);

  return (
    <div className="h-full">
      <LayoutBuilder />
    </div>
  );
};
