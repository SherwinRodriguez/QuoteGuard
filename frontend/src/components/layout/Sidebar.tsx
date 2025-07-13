import React from 'react';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  navItems: { name: string; href: string }[];
}

const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose, navItems }) => {
  return (
    <>
      {/* Sidebar Panel */}
      <div
        className={`fixed top-0 left-0 w-60 bg-white z-50 transform transition-transform duration-300 ease-in-out h-full md:hidden ${
          isOpen ? 'translate-x-0' : '-translate-x-[100vh]'
        }`}
      >
        <div className="pt-6 flex flex-col h-full justify-between">
          <div className='min-h-screen'>
            <div className='flex justify-between px-2'>
                <h2 className="text-xl font-bold mb-6 text-blue-700">
                    QuoteGuard
                </h2>

                <button
                onClick={onClose}
                className="text-sm text-blue-600 hover:underline"
                >
                    Close Menu
                </button>
          </div>
            <ul className="space-y-4 text-gray-800 font-medium h-full bg-[#85c1e9] p-3">
              {navItems.map((item, idx) => (
                <li key={idx}>
                  <a
                    href={item.href}
                    onClick={onClose}
                    className="block hover:text-blue-500 transition"
                  >
                    {item.name}
                  </a>
                </li>
              ))}
            </ul>
          </div>
          
        </div>
      </div>

      {/* Overlay */}
      {isOpen && (
        <div
          onClick={onClose}
          className="fixed inset-0 bg-black bg-opacity-40 z-40 md:hidden"
        ></div>
      )}
    </>
  );
};

export default Sidebar;
