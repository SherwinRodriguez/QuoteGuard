import { ReactNode } from 'react';
import Navbar from '@/components/Navbar';

const dashboardNavItems = [
  { name: 'Home', href: '/' },
  { name: 'Clients', href: '/dashboard/clients' },
  { name: 'Invoices', href: '/dashboard/invoices' },
  { name: 'Profile', href: '/dashboard/profile' },
  { name: 'Settings', href: '/settings' },
];

interface DashboardLayoutProps {
  children: ReactNode;
}

export default function DashboardLayout({ children }: DashboardLayoutProps) {
  return (
    <div className="flex">
      <div className="flex-1 flex flex-col">
        <Navbar navItems={dashboardNavItems} />
        <main>{children}</main>
      </div>
    </div>
  );
}
