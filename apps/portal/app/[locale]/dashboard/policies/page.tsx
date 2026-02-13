import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";

const policies = [
  {
    id: "POL-001",
    insuredName: "Acme Corp Ltd",
    status: "Active" as const,
    premium: "$12,500.00",
    effectiveDate: "2024-01-15",
  },
  {
    id: "POL-002",
    insuredName: "Globe Industries",
    status: "Review" as const,
    premium: "$8,750.00",
    effectiveDate: "2024-02-01",
  },
  {
    id: "POL-003",
    insuredName: "Pinnacle Holdings",
    status: "Active" as const,
    premium: "$23,100.00",
    effectiveDate: "2024-01-10",
  },
  {
    id: "POL-004",
    insuredName: "Meridian Services",
    status: "Review" as const,
    premium: "$5,400.00",
    effectiveDate: "2024-03-05",
  },
  {
    id: "POL-005",
    insuredName: "Horizon Partners",
    status: "Active" as const,
    premium: "$17,800.00",
    effectiveDate: "2024-02-20",
  },
];

function StatusBadge({ status }: { status: "Active" | "Review" }) {
  return (
    <Badge
      className={
        status === "Active"
          ? "bg-green-100 text-green-800 hover:bg-green-100"
          : "bg-yellow-100 text-yellow-800 hover:bg-yellow-100"
      }
    >
      {status}
    </Badge>
  );
}

export default function PoliciesPage() {
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
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Policies</h1>
          <p className="text-gray-600">Manage your insurance policies</p>
        </div>

        {/* Policies Table */}
        <div className="bg-white rounded-lg shadow">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Policy ID</TableHead>
                <TableHead>Insured Name</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Effective Date</TableHead>
                <TableHead className="text-right">Premium</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {policies.map((policy) => (
                <TableRow key={policy.id} className="cursor-pointer hover:bg-gray-50">
                  <TableCell className="font-medium">{policy.id}</TableCell>
                  <TableCell>{policy.insuredName}</TableCell>
                  <TableCell>
                    <StatusBadge status={policy.status} />
                  </TableCell>
                  <TableCell>{policy.effectiveDate}</TableCell>
                  <TableCell className="text-right">{policy.premium}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>
    </div>
  );
}
