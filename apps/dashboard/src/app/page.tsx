import Link from 'next/link';
import {
  LayoutDashboard,
  Palette,
  Users,
  Database,
  Settings,
  BarChart3,
  Clock,
  Activity,
  Shield
} from 'lucide-react';

export default function DashboardPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <Shield className="w-8 h-8 text-blue-600" />
              <h1 className="text-2xl font-bold text-gray-900">Beema Platform</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-sm text-gray-600">v0.1.0</span>
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" title="System Healthy" />
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Welcome Section */}
        <div className="mb-12">
          <h2 className="text-3xl font-bold text-gray-900 mb-2">Welcome to Beema</h2>
          <p className="text-lg text-gray-600">
            AI-powered insurance platform with metadata-driven architecture
          </p>
        </div>

        {/* Applications Grid */}
        <div className="mb-12">
          <h3 className="text-xl font-semibold text-gray-900 mb-6">Applications</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {/* Studio App */}
            <AppCard
              href="http://localhost:3010"
              icon={<Palette className="w-8 h-8" />}
              title="Studio"
              description="Visual message blueprint editor with drag-and-drop interface"
              color="blue"
              external
            />

            {/* Portal App */}
            <AppCard
              href="http://localhost:3011"
              icon={<Users className="w-8 h-8" />}
              title="Portal"
              description="Customer portal for policy and claim management"
              color="purple"
              external
            />

            {/* Admin Console */}
            <AppCard
              href="http://localhost:3012"
              icon={<Settings className="w-8 h-8" />}
              title="Admin"
              description="Global platform administration and tenant management"
              color="green"
              external
            />

            {/* Kernel API */}
            <AppCard
              href="http://localhost:8080/swagger-ui.html"
              icon={<Database className="w-8 h-8" />}
              title="Kernel API"
              description="Core API with Swagger documentation"
              color="blue"
              external
            />
          </div>
        </div>

        {/* Infrastructure Services */}
        <div className="mb-12">
          <h3 className="text-xl font-semibold text-gray-900 mb-6">Infrastructure & Observability</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <ServiceCard
              href="http://localhost:16686"
              icon={<Activity className="w-6 h-6" />}
              title="Jaeger"
              description="Distributed tracing"
              external
            />
            <ServiceCard
              href="http://localhost:3002"
              icon={<BarChart3 className="w-6 h-6" />}
              title="Grafana"
              description="Metrics & dashboards"
              external
            />
            <ServiceCard
              href="http://localhost:8088"
              icon={<Clock className="w-6 h-6" />}
              title="Temporal"
              description="Workflow engine"
              external
            />
            <ServiceCard
              href="http://localhost:8180"
              icon={<Shield className="w-6 h-6" />}
              title="Keycloak"
              description="Authentication"
              external
            />
            <ServiceCard
              href="http://localhost:9001"
              icon={<Database className="w-6 h-6" />}
              title="MinIO"
              description="Object storage"
              external
            />
            <ServiceCard
              href="http://localhost:8081"
              icon={<Activity className="w-6 h-6" />}
              title="Flink"
              description="Stream processing"
              external
            />
            <ServiceCard
              href="http://localhost:8288"
              icon={<Settings className="w-6 h-6" />}
              title="Inngest"
              description="Background jobs"
              external
            />
            <ServiceCard
              href="http://localhost:9090"
              icon={<BarChart3 className="w-6 h-6" />}
              title="Prometheus"
              description="Metrics collection"
              external
            />
          </div>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <StatCard
            title="Services Running"
            value="15"
            change="+100%"
            positive
          />
          <StatCard
            title="Uptime"
            value="99.9%"
            change="Last 30 days"
            positive
          />
          <StatCard
            title="Environment"
            value="Development"
            change="Local"
            positive
          />
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-gray-200 mt-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center justify-between text-sm text-gray-600">
            <p>Beema Insurance Platform - Powered by Spring Boot, Next.js, Temporal & Apache Flink</p>
            <p>
              <a href="https://github.com/prabhatkmr/beema" className="text-blue-600 hover:text-blue-800" target="_blank" rel="noopener noreferrer">
                GitHub
              </a>
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}

// Component for main application cards
function AppCard({
  href,
  icon,
  title,
  description,
  color,
  external = false
}: {
  href: string;
  icon: React.ReactNode;
  title: string;
  description: string;
  color: 'blue' | 'purple' | 'green';
  external?: boolean;
}) {
  const colorClasses = {
    blue: 'from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700',
    purple: 'from-purple-500 to-purple-600 hover:from-purple-600 hover:to-purple-700',
    green: 'from-green-500 to-green-600 hover:from-green-600 hover:to-green-700',
  };

  const Component = external ? 'a' : Link;
  const props = external ? { target: '_blank', rel: 'noopener noreferrer' } : {};

  return (
    <Component
      href={href}
      {...props}
      className={`block p-6 rounded-xl bg-gradient-to-br ${colorClasses[color]} text-white shadow-lg hover:shadow-xl transition-all duration-200 transform hover:-translate-y-1`}
    >
      <div className="flex items-start justify-between mb-4">
        <div className="p-3 bg-white/20 rounded-lg backdrop-blur-sm">
          {icon}
        </div>
        <svg className="w-5 h-5 opacity-70" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
        </svg>
      </div>
      <h3 className="text-xl font-bold mb-2">{title}</h3>
      <p className="text-white/90 text-sm">{description}</p>
    </Component>
  );
}

// Component for service cards
function ServiceCard({
  href,
  icon,
  title,
  description,
  external = false
}: {
  href: string;
  icon: React.ReactNode;
  title: string;
  description: string;
  external?: boolean;
}) {
  const Component = external ? 'a' : Link;
  const props = external ? { target: '_blank', rel: 'noopener noreferrer' } : {};

  return (
    <Component
      href={href}
      {...props}
      className="block p-4 bg-white rounded-lg border border-gray-200 hover:border-blue-500 hover:shadow-md transition-all duration-200"
    >
      <div className="flex items-center space-x-3 mb-2">
        <div className="text-blue-600">
          {icon}
        </div>
        <h4 className="font-semibold text-gray-900">{title}</h4>
      </div>
      <p className="text-sm text-gray-600">{description}</p>
    </Component>
  );
}

// Component for stats cards
function StatCard({
  title,
  value,
  change,
  positive
}: {
  title: string;
  value: string;
  change: string;
  positive: boolean;
}) {
  return (
    <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm">
      <h4 className="text-sm font-medium text-gray-600 mb-2">{title}</h4>
      <p className="text-3xl font-bold text-gray-900 mb-1">{value}</p>
      <p className={`text-sm ${positive ? 'text-green-600' : 'text-gray-600'}`}>
        {change}
      </p>
    </div>
  );
}
