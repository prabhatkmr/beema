const statusColors: Record<string, string> = {
  ACTIVE: 'bg-green-100 text-green-800',
  SUSPENDED: 'bg-yellow-100 text-yellow-800',
  PROVISIONING: 'bg-blue-100 text-blue-800',
  DEACTIVATED: 'bg-red-100 text-red-800',
  INACTIVE: 'bg-gray-100 text-gray-800',
  MAINTENANCE: 'bg-orange-100 text-orange-800',
  STANDARD: 'bg-gray-100 text-gray-800',
  PREMIUM: 'bg-purple-100 text-purple-800',
  ENTERPRISE: 'bg-indigo-100 text-indigo-800',
};

interface StatusBadgeProps {
  status: string;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const colors = statusColors[status] || 'bg-gray-100 text-gray-800';

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${colors}`}>
      {status}
    </span>
  );
}
