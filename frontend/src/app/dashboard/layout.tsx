'use client';

import { ReactNode, useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useRouter } from 'next/navigation';
import Navbar from '@/components/layout/Navbar';

const dashboardNavItems = [
  { name: 'Home', href: '/' },
  { name: 'Clients', href: '/dashboard/clients'},
  { name: 'Invoices', href: '/dashboard/invoices' },
  { name: 'Profile', href: '/dashboard/profile' },
  { name: 'Settings', href: '/settings' },
];

interface DashboardLayoutProps {
  children: ReactNode;
}

export default function DashboardLayout({ children }: DashboardLayoutProps) {
  const { isLoggedIn } = useAuth();
  const router = useRouter();
  const [checked, setChecked] = useState(false);

  useEffect(() => {
    if (!isLoggedIn) {
      router.replace('/login');
    } else {
      setChecked(true);
    }
  }, [isLoggedIn, router]);

  if (!checked) {
    return (
      <div className="h-screen flex items-center justify-center text-gray-600">
        Checking login...
      </div>
    );
  }

  return (
    <div className="flex">
      <div className="flex-1 flex flex-col">
        <Navbar navItems={dashboardNavItems} />
        <main>{children}</main>
      </div>
    </div>
  );
}