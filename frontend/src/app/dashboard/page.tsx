'use client';

import { useEffect, useState } from 'react';

interface Stats {
  clients: number;
  invoices: number;
  pending: number;
}

export default function DashboardPage() {
  const [stats, setStats] = useState<Stats | null>(null);

  useEffect(() => {
    const fetchStats = async () => {
      const userId = localStorage.getItem('userId');
      if (!userId) return;

      try {
        const res = await fetch(`https://quoteguard-backend.onrender.com/api/dashboard/stats?userId=${userId}`);
        const data = await res.json();
        setStats(data);
      } catch (err) {
        console.error("❌ Failed to fetch dashboard stats:", err);
      }
    };

    fetchStats();
  }, []);

  return (
    <div className="min-h-screen bg-gray-100 p-6 pt-24 text-black">
      <h1 className="text-3xl font-bold mb-6 text-blue-800">📊 Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card title="Clients" value={stats?.clients ?? 0} color="bg-purple-500" />
        <Card title="Invoices" value={stats?.invoices ?? 0} color="bg-green-600" />
        <Card title="Pending Invoices" value={stats?.pending ?? 0} color="bg-red-500" />
      </div>
    </div>
  );
}

function Card({ title, value, color }: { title: string; value: number; color: string }) {
  return (
    <div className={`rounded-xl p-6 text-white shadow-md ${color}`}>
      <h2 className="text-xl font-semibold mb-2">{title}</h2>
      <p className="text-4xl font-bold">{value}</p>
    </div>
  );
}