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

const claims = [
  {
    id: "CLM-001",
    policyId: "POL-001",
    claimType: "Property Damage",
    status: "Pending" as const,
    amount: "$15,000.00",
    dateSubmitted: "2024-03-15",
  },
  {
    id: "CLM-002",
    policyId: "POL-003",
    claimType: "Liability",
    status: "Approved" as const,
    amount: "$8,500.00",
    dateSubmitted: "2024-03-10",
  },
  {
    id: "CLM-003",
    policyId: "POL-005",
    claimType: "Business Interruption",
    status: "Under Review" as const,
    amount: "$25,000.00",
    dateSubmitted: "2024-03-20",
  },
];

function StatusBadge({ status }: { status: "Pending" | "Approved" | "Under Review" }) {
  const colorClass = {
    Pending: "bg-yellow-100 text-yellow-800 hover:bg-yellow-100",
    Approved: "bg-green-100 text-green-800 hover:bg-green-100",
    "Under Review": "bg-blue-100 text-blue-800 hover:bg-blue-100",
  }[status];

  return <Badge className={colorClass}>{status}</Badge>;
}

export default function ClaimsPage() {
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
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">Claims</h1>
            <p className="text-gray-600">Submit and track insurance claims</p>
          </div>
          <button className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors">
            New Claim
          </button>
        </div>

        {/* Claims Table */}
        <div className="bg-white rounded-lg shadow">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Claim ID</TableHead>
                <TableHead>Policy ID</TableHead>
                <TableHead>Claim Type</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Date Submitted</TableHead>
                <TableHead className="text-right">Amount</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {claims.map((claim) => (
                <TableRow key={claim.id} className="cursor-pointer hover:bg-gray-50">
                  <TableCell className="font-medium">{claim.id}</TableCell>
                  <TableCell>{claim.policyId}</TableCell>
                  <TableCell>{claim.claimType}</TableCell>
                  <TableCell>
                    <StatusBadge status={claim.status} />
                  </TableCell>
                  <TableCell>{claim.dateSubmitted}</TableCell>
                  <TableCell className="text-right">{claim.amount}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>
    </div>
  );
}
