import type { Metadata } from "next";
import "./globals.css";
import Navbar from "@/components/layout/Navbar";

const navItems =[
    {name:"Home",href:"/"},
    {name:"Dashboard",href:"/dashboard"},
    {name:"Login",href:"/login"},
    {name:"Verify",href:"/verify"},
]

export const metadata: Metadata = {
  title: "Quote Guard",
  description: "Invoice & QR verification system",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`antialiased`}
      >
        <Navbar navItems={navItems}/>
        {children}
      </body>
    </html>
  );
}
