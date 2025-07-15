'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Sidebar from './Sidebar';
import { useAuth } from '@/context/AuthContext';

interface NavbarProps {
  navItems: { name: string; href: string }[];
}

const Navbar: React.FC<NavbarProps> = ({ navItems }) => {
  const [isOpen, setIsopen] = useState(false);
 const { isLoggedIn, logout } = useAuth();

  return (
    <nav className="fixed w-full z-50 bg-blue-500 backdrop-blur-md border border-white/20 shadow-xl p-5">
      <div className="flex justify-between w-full items-center">
        {/* Logo */}
        <h1 className="text-2xl font-extrabold bg-gradient-to-r from-red-500 via-pink-500 to-blue-800 text-transparent bg-clip-text">
          QuoteGuard
        </h1>

        {/* Desktop nav links */}
        <ul className="hidden md:flex gap-5 md:gap-8 items-center text-sm md:text-base font-medium text-white">
          {navItems.map((item, idx) => (
            <li key={idx} className="relative cursor-pointer">
              {item.name === 'Login' && isLoggedIn ? null : (
                <Link
                  href={item.href}
                  className={
                    item.name === 'Login'
                      ? 'text-blue-950 font-bold rounded-xl p-2 hover:bg-amber-400 transition'
                      : 'hover:text-red-600'
                  }
                >
                  {item.name}
                </Link>
              )}
            </li>
          ))}
          {isLoggedIn && (
            <li>
              <button
                onClick={logout}
                className="bg-red-600 text-white px-3 py-1 rounded-md hover:bg-red-700 transition"
              >
                Logout
              </button>
            </li>
          )}
        </ul>

        {/* Mobile toggle */}
        <button
          onClick={() => setIsopen(!isOpen)}
          className="md:hidden text-blue-900"
          aria-label="Toggle menu"
        >
          {isOpen ? (
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          ) : (
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8h16M4 16h16" />
            </svg>
          )}
        </button>
      </div>

      {/* Mobile Sidebar */}
      <Sidebar isOpen={isOpen} onClose={() => setIsopen(false)} navItems={navItems} />
    </nav>
  );
};

export default Navbar;