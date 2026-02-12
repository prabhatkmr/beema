import React, { useEffect, useRef } from 'react';
import { FieldMapping } from '../../types/blueprint';
import { MappingConnection } from '../../types/mapping';

interface MappingLineProps {
  connections: MappingConnection[];
}

export const MappingLine: React.FC<MappingLineProps> = ({ connections }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Set canvas size
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width;
    canvas.height = rect.height;

    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw connections
    connections.forEach((connection) => {
      const { sourcePosition, targetPosition, mapping } = connection;

      // Determine color based on mapping type
      let strokeColor = '#9CA3AF'; // gray-400
      if (mapping.mappingType === 'transform') {
        strokeColor = '#3B82F6'; // blue-500
      } else if (mapping.mappingType === 'constant') {
        strokeColor = '#8B5CF6'; // purple-500
      } else if (mapping.mappingType === 'conditional') {
        strokeColor = '#F59E0B'; // amber-500
      } else if (mapping.mappingType === 'direct') {
        strokeColor = '#10B981'; // green-500
      }

      // Draw bezier curve
      ctx.beginPath();
      ctx.strokeStyle = strokeColor;
      ctx.lineWidth = 2;

      const controlPointOffset = Math.abs(targetPosition.x - sourcePosition.x) / 2;

      ctx.moveTo(sourcePosition.x, sourcePosition.y);
      ctx.bezierCurveTo(
        sourcePosition.x + controlPointOffset,
        sourcePosition.y,
        targetPosition.x - controlPointOffset,
        targetPosition.y,
        targetPosition.x,
        targetPosition.y
      );
      ctx.stroke();

      // Draw arrow head
      const arrowSize = 8;
      const angle = Math.atan2(
        targetPosition.y - sourcePosition.y,
        targetPosition.x - sourcePosition.x
      );

      ctx.beginPath();
      ctx.fillStyle = strokeColor;
      ctx.moveTo(targetPosition.x, targetPosition.y);
      ctx.lineTo(
        targetPosition.x - arrowSize * Math.cos(angle - Math.PI / 6),
        targetPosition.y - arrowSize * Math.sin(angle - Math.PI / 6)
      );
      ctx.lineTo(
        targetPosition.x - arrowSize * Math.cos(angle + Math.PI / 6),
        targetPosition.y - arrowSize * Math.sin(angle + Math.PI / 6)
      );
      ctx.closePath();
      ctx.fill();
    });
  }, [connections]);

  return (
    <canvas
      ref={canvasRef}
      className="absolute inset-0 pointer-events-none"
      style={{ zIndex: 1 }}
    />
  );
};
