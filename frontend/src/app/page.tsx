"use client";

import About from "@/components/layout/About";
import { ArrowRight, Shield, Star, Users, CheckCircle, Zap, Lock, TrendingUp, Play } from "lucide-react";
import { useState, useEffect } from "react";

export default function Page() {
  const [isVisible, setIsVisible] = useState(false);
  const [activeFeature, setActiveFeature] = useState(0);
  const [mousePosition, setMousePosition] = useState({ x: 0, y: 0 });

  useEffect(() => {
    setIsVisible(true);
    
    // Mouse move effect
    const handleMouseMove = (e: MouseEvent) => {
      setMousePosition({ x: e.clientX, y: e.clientY });
    };
    
    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, []);

  // Auto-rotate features
  useEffect(() => {
    const interval = setInterval(() => {
      setActiveFeature((prev) => (prev + 1) % 3);
    }, 3000);
    return () => clearInterval(interval);
  }, []);

  const features = [
    {
      icon: Shield,
      title: "Secure Protection",
      description: "Advanced encryption and security measures to keep your quotes safe",
      color: "blue",
      gradient: "from-blue-500 to-blue-600"
    },
    {
      icon: CheckCircle,
      title: "Easy Management",
      description: "Intuitive interface for effortless quote organization and tracking",
      color: "green",
      gradient: "from-green-500 to-green-600"
    },
    {
      icon: Users,
      title: "Team Collaboration",
      description: "Seamless sharing and collaboration features for your team",
      color: "purple",
      gradient: "from-purple-500 to-purple-600"
    }
  ];

  const stats = [
    { value: "10K+", label: "Protected Quotes", icon: Shield, color: "blue" },
    { value: "99.9%", label: "Uptime", icon: TrendingUp, color: "green" },
    { value: "5K+", label: "Happy Users", icon: Users, color: "purple" },
    { value: "24/7", label: "Support", icon: Zap, color: "orange" }
  ];

  return (
    <div className="overflow-hidden">
      {/* Hero Section */}
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#0f0c29] via-[#302b63] to-[#24243e] relative overflow-hidden">
        {/* Animated background elements */}
        <div className="absolute inset-0">
          <div 
            className="absolute w-96 h-96 bg-white/10 rounded-full blur-3xl transition-all duration-1000"
            style={{
              left: mousePosition.x / 20,
              top: mousePosition.y / 20,
              transform: 'translate(-50%, -50%)'
            }}
          ></div>
          <div className="absolute top-20 left-20 w-72 h-72 bg-blue-300/20 rounded-full blur-3xl animate-pulse"></div>
          <div className="absolute bottom-20 right-20 w-96 h-96 bg-purple-300/20 rounded-full blur-3xl animate-pulse delay-1000"></div>
          <div className="absolute top-1/2 left-1/2 w-[800px] h-[800px] bg-gradient-to-r from-blue-400/10 to-purple-400/10 rounded-full blur-3xl animate-spin" style={{ animationDuration: '20s' }}></div>
        </div>
        
        <div className={`text-center z-10 px-4 max-w-5xl transition-all duration-1000 ${isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-10'}`}>
          {/* Animated Logo */}
          <div className="mb-8 flex justify-center">
            <div className="bg-white/90 backdrop-blur-sm rounded-full p-8 shadow-2xl hover:shadow-3xl transition-all duration-500 hover:scale-110 group">
              <Shield className="w-20 h-20 text-blue-600 group-hover:rotate-12 transition-transform duration-500" />
            </div>
          </div>
          
          {/* Animated heading */}
          <h1 className="text-6xl md:text-8xl font-bold text-white mb-6 leading-tight">
            Quote{" "}
            <span className="bg-gradient-to-r from-yellow-400 to-orange-400 bg-clip-text text-transparent animate-pulse">
              Guard
            </span>
          </h1>
          
          {/* Typewriter effect subheading */}
          <p className="text-xl md:text-2xl text-white/90 mb-8 max-w-3xl mx-auto leading-relaxed">
            Protect your quotes with{" "}
            <span className="text-yellow-300 font-semibold">advanced security</span> and{" "}
            <span className="text-yellow-300 font-semibold">seamless management</span>
          </p>
          
          {/* Enhanced CTA Buttons */}
          <div className="flex flex-col sm:flex-row gap-6 justify-center mb-16">
            <button className="group bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 text-white px-10 py-5 rounded-full font-semibold text-lg transition-all duration-300 transform hover:scale-105 shadow-2xl hover:shadow-3xl flex items-center justify-center gap-3 relative overflow-hidden">
              <span className="relative z-10">Get Started</span>
              <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform duration-300 relative z-10" />
              <div className="absolute inset-0 bg-gradient-to-r from-blue-400 to-purple-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
            </button>
            <button className="group bg-white/20 backdrop-blur-sm hover:bg-white/30 text-white px-10 py-5 rounded-full font-semibold text-lg transition-all duration-300 border-2 border-white/30 hover:border-white/50 shadow-2xl hover:shadow-3xl flex items-center justify-center gap-3">
              <Play className="w-5 h-5" />
              Watch Demo
            </button>
          </div>
          
          {/* Interactive Stats */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 max-w-4xl mx-auto">
            {stats.map((stat, index) => (
              <div
                key={index}
                className="bg-white/10 backdrop-blur-sm rounded-xl p-6 shadow-xl hover:shadow-2xl transition-all duration-500 hover:scale-105 hover:bg-white/20 group cursor-pointer"
                style={{ animationDelay: `${index * 200}ms` }}
              >
                <div className={`w-12 h-12 mx-auto mb-3 rounded-full bg-gradient-to-r ${stat.color === 'blue' ? 'from-blue-400 to-blue-600' : stat.color === 'green' ? 'from-green-400 to-green-600' : stat.color === 'purple' ? 'from-purple-400 to-purple-600' : 'from-orange-400 to-orange-600'} flex items-center justify-center group-hover:rotate-12 transition-transform duration-300`}>
                  <stat.icon className="w-6 h-6 text-white" />
                </div>
                <div className="text-3xl font-bold text-white mb-1 group-hover:scale-110 transition-transform duration-300">{stat.value}</div>
                <div className="text-sm text-white/80">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
        
        {/* Enhanced scroll indicator */}
        <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 animate-bounce cursor-pointer hover:scale-110 transition-transform duration-300">
          <div className="w-8 h-12 border-2 border-white/50 rounded-full flex justify-center backdrop-blur-sm">
            <div className="w-1 h-4 bg-white rounded-full mt-2 animate-pulse"></div>
          </div>
        </div>
      </div>

      {/* Interactive Features Section */}
      <div className="py-24 bg-white relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-32 bg-gradient-to-b from-purple-100/20 to-transparent"></div>
        
        <div className="max-w-7xl mx-auto px-4">
          <div className="text-center mb-16">
            <h2 className="text-4xl md:text-5xl font-bold text-gray-800 mb-6">
              Why Choose{" "}
              <span className="bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
                Quote Guard?
              </span>
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              Experience the most secure and user-friendly quote management platform with cutting-edge features
            </p>
          </div>
          
          <div className="grid md:grid-cols-3 gap-8">
            {features.map((feature, index) => (
              <div
                key={index}
                className={`group p-8 rounded-2xl transition-all duration-500 cursor-pointer relative overflow-hidden ${
                  activeFeature === index
                    ? 'bg-gradient-to-br from-blue-50 to-purple-50 shadow-2xl scale-105'
                    : 'hover:shadow-xl hover:scale-105 bg-white'
                }`}
                onMouseEnter={() => setActiveFeature(index)}
              >
                <div className="relative z-10">
                  <div className={`w-20 h-20 rounded-full bg-gradient-to-r ${feature.gradient} flex items-center justify-center mx-auto mb-6 group-hover:scale-110 transition-all duration-300 shadow-lg`}>
                    <feature.icon className="w-10 h-10 text-white" />
                  </div>
                  <h3 className="text-2xl font-bold text-gray-800 mb-4 group-hover:text-blue-600 transition-colors duration-300">
                    {feature.title}
                  </h3>
                  <p className="text-gray-600 leading-relaxed">{feature.description}</p>
                </div>
                
                {/* Animated background */}
                <div className={`absolute inset-0 opacity-0 group-hover:opacity-10 transition-opacity duration-500 bg-gradient-to-r ${feature.gradient} rounded-2xl`}></div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Enhanced Testimonial Section */}
      <div className="py-24 bg-gradient-to-br from-gray-50 to-blue-50 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-blue-200/20 rounded-full blur-3xl"></div>
        <div className="absolute bottom-0 left-0 w-96 h-96 bg-purple-200/20 rounded-full blur-3xl"></div>
        
        <div className="max-w-5xl mx-auto px-4 text-center relative z-10">
          <h2 className="text-4xl font-bold text-gray-800 mb-16">What Our Users Say</h2>
          
          <div className="bg-white/80 backdrop-blur-sm rounded-3xl p-10 shadow-2xl hover:shadow-3xl transition-all duration-500 relative overflow-hidden group">
            <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-[#0f0c29] via-[#302b63] to-[#24243e]"></div>
            
            <div className="flex justify-center mb-6">
              {[...Array(5)].map((_, i) => (
                <Star 
                  key={i} 
                  className="w-8 h-8 text-yellow-400 fill-current hover:scale-125 transition-transform duration-200 cursor-pointer" 
                  style={{ animationDelay: `${i * 100}ms` }}
                />
              ))}
            </div>
            
            <blockquote className="text-2xl text-gray-700 mb-8 italic leading-relaxed">
              "Quote Guard has revolutionized how we manage our business quotes. 
              The security features give us peace of mind, and the interface is incredibly intuitive."
            </blockquote>
            
            <div className="flex items-center justify-center gap-4">
              <div className="w-16 h-16 bg-gradient-to-r from-blue-500 to-purple-500 rounded-full flex items-center justify-center text-white font-bold text-xl shadow-lg hover:scale-110 transition-transform duration-300 cursor-pointer">
                JD
              </div>
              <div className="text-left">
                <div className="font-bold text-gray-800 text-lg">John Doe</div>
                <div className="text-blue-600 font-medium">CEO, TechCorp</div>
              </div>
            </div>
            
            {/* Floating elements */}
            <div className="absolute top-4 right-4 w-8 h-8 bg-blue-200 rounded-full opacity-20 group-hover:animate-bounce"></div>
            <div className="absolute bottom-4 left-4 w-6 h-6 bg-purple-200 rounded-full opacity-20 group-hover:animate-pulse"></div>
          </div>
        </div>
      </div>

      {/* Call to Action Section */}
      <div className="py-24 bg-gradient-to-r from-blue-600 to-purple-700 relative overflow-hidden">
        <div className="absolute inset-0 bg-black/10"></div>
        <div className="absolute top-0 left-0 w-full h-full">
          <div className="w-96 h-96 bg-white/5 rounded-full blur-3xl absolute top-10 left-10 animate-pulse"></div>
          <div className="w-72 h-72 bg-white/5 rounded-full blur-3xl absolute bottom-10 right-10 animate-pulse delay-1000"></div>
        </div>
        
        <div className="max-w-4xl mx-auto px-4 text-center relative z-10">
          <h2 className="text-4xl md:text-5xl font-bold text-white mb-6">
            Ready to Secure Your Quotes?
          </h2>
          <p className="text-xl text-white/90 mb-10 max-w-2xl mx-auto">
            Join thousands of businesses that trust Quote Guard for their quote management needs
          </p>
          
          <div className="flex flex-col sm:flex-row gap-6 justify-center">
            <button className="group bg-white text-blue-600 px-10 py-5 rounded-full font-bold text-lg transition-all duration-300 transform hover:scale-105 shadow-2xl hover:shadow-3xl flex items-center justify-center gap-3 relative overflow-hidden">
              <span className="relative z-10">Start Free Trial</span>
              <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform duration-300 relative z-10" />
              <div className="absolute inset-0 bg-gradient-to-r from-blue-50 to-purple-50 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
            </button>
            <button className="group bg-transparent border-2 border-white text-white px-10 py-5 rounded-full font-bold text-lg transition-all duration-300 hover:bg-white hover:text-blue-600 flex items-center justify-center gap-3">
              <Lock className="w-5 h-5" />
              Enterprise Solutions
            </button>
          </div>
        </div>
      </div>

      <About />
    </div>
  );
}