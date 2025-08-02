"use client";

import About from "@/components/layout/About";
import { ArrowRight, Shield, Star, Users, CheckCircle } from "lucide-react";

export default function Page() {
  return (
    <div>
      {/* Hero Section */}
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#ecf0f1] via-[#bdc3c7] to-[#95a5a6] relative overflow-hidden">
        {/* Background decorative elements */}
        <div className="absolute top-20 left-20 w-72 h-72 bg-blue-200 rounded-full opacity-20 blur-3xl animate-pulse"></div>
        <div className="absolute bottom-20 right-20 w-96 h-96 bg-purple-200 rounded-full opacity-20 blur-3xl animate-pulse delay-1000"></div>
        
        <div className="text-center z-10 px-4 max-w-4xl">
          {/* Logo/Icon */}
          <div className="mb-8 flex justify-center">
            <div className="bg-white rounded-full p-6 shadow-2xl">
              <Shield className="w-16 h-16 text-blue-600" />
            </div>
          </div>
          
          {/* Main heading */}
          <h1 className="text-6xl md:text-7xl font-bold text-gray-800 mb-6 leading-tight">
            Quote <span className="text-blue-600">Guard</span>
          </h1>
          
          {/* Subheading */}
          <p className="text-xl md:text-2xl text-gray-600 mb-8 max-w-2xl mx-auto leading-relaxed">
            Protect your quotes with advanced security and seamless management
          </p>
          
          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center mb-12">
            <button className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-4 rounded-lg font-semibold text-lg transition-all duration-300 transform hover:scale-105 shadow-lg hover:shadow-xl flex items-center justify-center gap-2">
              Get Started <ArrowRight className="w-5 h-5" />
            </button>
            <button className="bg-white hover:bg-gray-50 text-gray-800 px-8 py-4 rounded-lg font-semibold text-lg transition-all duration-300 border-2 border-gray-200 hover:border-gray-300 shadow-lg hover:shadow-xl">
              Learn More
            </button>
          </div>
          
          {/* Stats */}
          <div className="flex flex-wrap justify-center gap-8 text-center">
            <div className="bg-white/80 backdrop-blur-sm rounded-lg p-4 min-w-[120px] shadow-lg">
              <div className="text-2xl font-bold text-blue-600">10K+</div>
              <div className="text-sm text-gray-600">Protected Quotes</div>
            </div>
            <div className="bg-white/80 backdrop-blur-sm rounded-lg p-4 min-w-[120px] shadow-lg">
              <div className="text-2xl font-bold text-green-600">99.9%</div>
              <div className="text-sm text-gray-600">Uptime</div>
            </div>
            <div className="bg-white/80 backdrop-blur-sm rounded-lg p-4 min-w-[120px] shadow-lg">
              <div className="text-2xl font-bold text-purple-600">5K+</div>
              <div className="text-sm text-gray-600">Happy Users</div>
            </div>
          </div>
        </div>
        
        {/* Scroll indicator */}
        <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 animate-bounce">
          <div className="w-6 h-10 border-2 border-gray-400 rounded-full flex justify-center">
            <div className="w-1 h-3 bg-gray-400 rounded-full mt-2 animate-pulse"></div>
          </div>
        </div>
      </div>

      {/* Features Preview Section */}
      <div className="py-20 bg-white">
        <div className="max-w-6xl mx-auto px-4">
          <h2 className="text-3xl md:text-4xl font-bold text-center text-gray-800 mb-4">
            Why Choose Quote Guard?
          </h2>
          <p className="text-xl text-gray-600 text-center mb-12 max-w-2xl mx-auto">
            Experience the most secure and user-friendly quote management platform
          </p>
          
          <div className="grid md:grid-cols-3 gap-8">
            <div className="text-center p-6 rounded-xl hover:shadow-lg transition-all duration-300 hover:transform hover:scale-105">
              <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Shield className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-800 mb-2">Secure Protection</h3>
              <p className="text-gray-600">Advanced encryption and security measures to keep your quotes safe</p>
            </div>
            
            <div className="text-center p-6 rounded-xl hover:shadow-lg transition-all duration-300 hover:transform hover:scale-105">
              <div className="bg-green-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <CheckCircle className="w-8 h-8 text-green-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-800 mb-2">Easy Management</h3>
              <p className="text-gray-600">Intuitive interface for effortless quote organization and tracking</p>
            </div>
            
            <div className="text-center p-6 rounded-xl hover:shadow-lg transition-all duration-300 hover:transform hover:scale-105">
              <div className="bg-purple-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Users className="w-8 h-8 text-purple-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-800 mb-2">Team Collaboration</h3>
              <p className="text-gray-600">Seamless sharing and collaboration features for your team</p>
            </div>
          </div>
        </div>
      </div>

      {/* Testimonial Section */}
      <div className="py-20 bg-gray-50">
        <div className="max-w-4xl mx-auto px-4 text-center">
          <h2 className="text-3xl font-bold text-gray-800 mb-12">What Our Users Say</h2>
          <div className="bg-white rounded-2xl p-8 shadow-xl">
            <div className="flex justify-center mb-4">
              {[...Array(5)].map((_, i) => (
                <Star key={i} className="w-6 h-6 text-yellow-400 fill-current" />
              ))}
            </div>
            <blockquote className="text-xl text-gray-700 mb-6 italic">
              "Quote Guard has revolutionized how we manage our business quotes. 
              The security features give us peace of mind, and the interface is incredibly intuitive."
            </blockquote>
            <div className="flex items-center justify-center gap-4">
              <div className="w-12 h-12 bg-gradient-to-r from-blue-400 to-purple-500 rounded-full flex items-center justify-center text-white font-bold">
                JD
              </div>
              <div>
                <div className="font-semibold text-gray-800">John Doe</div>
                <div className="text-gray-600 text-sm">CEO, TechCorp</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <About />
    </div>
  );
}
