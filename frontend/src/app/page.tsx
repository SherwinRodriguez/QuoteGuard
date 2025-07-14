"use client";

import About from "@/components/layout/About";

export default function Page() {
  return (
    <div>
      <div className="min-h-screen flex items-center justify-center bg-[#ecf0f1]">
        <h1 className="text-4xl font-bold text-black">Quote Guard</h1>
      </div>
      <About/>
    </div>
  );
}
