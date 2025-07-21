"use client";

import { useEffect, useState } from "react";

export default function DashboardPage() {
  const [clientsCount, setClientsCount] = useState(0);
  const [invoicesCount, setInvoicesCount] = useState(0);
  const [pendingCount, setPendingCount] = useState(0);

  const fetchStats = async () => {
    const userId = localStorage.getItem("userId");
    if (!userId) return;

    try {
      const res = await fetch(`http://localhost:8080/api/dashboard/stats?userId=${userId}`);
      const data = await res.json();
      setClientsCount(data.clients);
      setInvoicesCount(data.invoices);
      setPendingCount(data.pending);
    } catch (err) {
      console.error("Error fetching dashboard stats:", err);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  return (
    <div className="grid grid-cols-3 pt-24 gap-4 p-4">
      <div className="bg-white rounded-2xl shadow p-4">
        <h2 className="text-lg font-semibold text-gray-700">Total Clients</h2>
        <p className="text-2xl font-bold text-blue-600">{clientsCount}</p>
      </div>

      <div className="bg-white rounded-2xl shadow p-4">
        <h2 className="text-lg font-semibold text-gray-700">Invoices Created</h2>
        <p className="text-2xl font-bold text-green-600">{invoicesCount}</p>
      </div>

      <div className="bg-white rounded-2xl shadow p-4">
        <h2 className="text-lg font-semibold text-gray-700">Pending Verifications</h2>
        <p className="text-2xl font-bold text-red-600">{pendingCount}</p>
      </div>
    </div>
  );
}