import type { Metadata } from "next";
import "./globals.css";
import Navbar from "@/components/layout/Navbar";
import { AuthProvider } from "@/context/AuthContext";

const navItems =[
    {name:"Home",href:"/"},
    {name:"Dashboard",href:"/dashboard"},
    {name:"Verify",href:"/verify"},
    {name:"Login",href:"/login"}
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
        <AuthProvider>  
          <Navbar navItems={navItems}/>
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
